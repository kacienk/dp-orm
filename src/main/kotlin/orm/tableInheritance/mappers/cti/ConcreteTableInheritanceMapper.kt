package orm.tableInheritance.mappers.cti

import Player
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import orm.decorators.Entity
import orm.tableInheritance.ITableInheritanceMapper
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import orm.EntityProcessor
import orm.tableInheritance.RelationsHandler
import java.sql.ResultSet
import kotlin.reflect.full.*

class ConcreteTableInheritanceMapper(private val clazz: KClass<*>): ITableInheritanceMapper, EntityProcessor() {
    override fun insert(entity: Any): Boolean {
        val sqlInsert = StringBuilder()
        val castedEntity = clazz.cast(entity)
        val castedEntityClass = castedEntity::class
        val tableName = getTableName(clazz)

        val columnNamesAndValues = extractAllPropertiesWithInheritance(castedEntityClass).filter { prop ->
            getColumnName(prop) != null
        }.map { prop -> getColumnName(prop) to prop.call(castedEntity) }

        val mostBaseClass = extractMostBaseClass(clazz)
        val keyTableName = "${getTableName(mostBaseClass)}Keys"
        val primaryKeyName = getPrimaryKeyName(mostBaseClass)
        val resultPair = columnNamesAndValues.find { it.first == primaryKeyName }

        // Not sure about this
        sqlInsert.append("INSERT INTO $keyTableName (")
        sqlInsert.append(getPrimaryKeyName(mostBaseClass))
        sqlInsert.append(") VALUES (")
        if (resultPair != null) {
            sqlInsert.append(formatValue(resultPair.second))
        }
        sqlInsert.append(");\n")

        sqlInsert.append("INSERT INTO $tableName (")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { it.first!! })
        sqlInsert.append(") VALUES (")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { formatValue(it.second) })
        sqlInsert.append(");\n")

        val sqlStatement = sqlInsert.toString()
        println(sqlStatement)

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }
        return true
    }

    override fun find(id: Int?): Any? {
        if (clazz.isAbstract) {
            val mostBaseClass = extractMostBaseClass(clazz)
            getChildrenClasses(mostBaseClass).forEach{ childClass ->
                val result = smallerFind(id, childClass).execAndMap(::findWithRelationsTransform)
                if (result.isNotEmpty()) {
                    return  result
                }
            }
        }
        else {
            val result = smallerFind(id, clazz).execAndMap(::findWithRelationsTransform)
            return result
        }

        return null
    }

    override fun findWithoutRelations(id: Int, entityClass: KClass<*>): Any? {
        val mostBaseClass = extractMostBaseClass(clazz)
        getChildrenClasses(mostBaseClass).forEach{ childClass ->

            val result = smallerFind(id, childClass).execAndMap(::transform)

            if (result.isNotEmpty()) {
                return  result
            }
        }

        return null
    }

    override fun update(entity: Any): Boolean {
        val sqlUpdate = StringBuilder()
        val castedEntity = clazz.cast(entity)
        val castedEntityClass = castedEntity::class
        val tableName = getTableName(clazz)
        val mostBaseClass = extractMostBaseClass(clazz)

        val primaryKeyProp = getPrimaryKeyProp(mostBaseClass)
        val primaryKey = getColumnName(primaryKeyProp) to (
                primaryKeyProp.call(castedEntity) ?: throw IllegalArgumentException(
                    "Updated entity must have value in field annotated with @PrimaryKey"
                )
                )

        val columnNamesAndValues = extractAllPropertiesWithInheritance(castedEntityClass).filter { prop ->
            getColumnName(prop) != null
        }.map { prop -> getColumnName(prop) to prop.call(castedEntity) }

        sqlUpdate.append("UPDATE $tableName SET\n")
        sqlUpdate.append(columnNamesAndValues.joinToString(", ") { "${it.first!!} = ${formatValue(it.second)}" })
        sqlUpdate.append("\nWHERE ${primaryKey.first} = ${formatValue(primaryKey.second)};")

        val sqlStatement = sqlUpdate.toString()

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun remove(id: Int): Boolean {
        val sqlDelete = java.lang.StringBuilder()
        val tableName = getTableName(clazz)
        val mostBaseClass = extractMostBaseClass(clazz)
        val primaryKeyColumn = getPrimaryKeyName(mostBaseClass)
        val keyTableName = getTableName(mostBaseClass)+"Keys"

        sqlDelete.append("DELETE FROM $tableName WHERE $primaryKeyColumn = $id;\n")
        sqlDelete.append("DELETE FROM $keyTableName WHERE $primaryKeyColumn = $id;")
        val sqlStatement = sqlDelete.toString()

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun query(q: String): Any? {
        return q.execAndMap(::transform)
    }

    private fun extractAllPropertiesWithInheritance(clazz: KClass<*>): List<KProperty1<out Any, *>> {
        val mostBaseClass = extractMostBaseClass(clazz)
        val notEntityClass = mostBaseClass.superclasses.firstOrNull() ?: return emptyList()

        return clazz.memberProperties.filter { prop -> prop !in notEntityClass.memberProperties }
    }

    private fun extractMostBaseClass(clazz: KClass<*>): KClass<*> {
        val baseClass = clazz.superclasses.firstOrNull() ?: return clazz
        baseClass.findAnnotation<Entity>() ?: return clazz
        return extractMostBaseClass(baseClass)
    }

    private fun getChildrenClasses(entityClass: KClass<*>): MutableList<KClass<*>> {
        val childrenClasses = mutableListOf<KClass<*>>()
        entityClass.sealedSubclasses.forEach { subclass ->
            if (subclass.findAnnotation<Entity>() != null) {
                childrenClasses.add(subclass)
            }
        }

        return childrenClasses
    }

    private fun getColumnNamesWithInheritanceSql(entityClass: KClass<*>): String {
        return this.getColumnNamesWithInheritance(entityClass).joinToString(", ")
    }

    private fun getColumnNamesWithInheritance(entityClass: KClass<*>): MutableList<String> {
        val columnNames = mutableListOf<String>()

        entityClass.declaredMemberProperties.filter {
                prop -> getColumnName(prop) != null
        }.forEach { prop -> columnNames.add(getColumnName(prop)!!) } // nulls filtered out

        entityClass.superclasses.forEach { superclass ->
            if (superclass.findAnnotation<Entity>() != null) {
                columnNames.addAll(this.getColumnNamesWithInheritance(superclass))
            }
        }

        return columnNames
    }

    private fun smallerFind(id: Int?, childClass: KClass<*>): String {
        val sqlSelect = StringBuilder("SELECT ")

        val columnNames = getColumnNamesWithInheritanceSql(childClass)
        sqlSelect.append("$columnNames\n")

        val tableName = getTableName(childClass)

        val mostBaseClass = extractMostBaseClass(childClass)
        val keyTableName = getTableName(mostBaseClass)
        val primaryKeyName = getPrimaryKeyName(mostBaseClass)

        sqlSelect.append("FROM $keyTableName\n")

        sqlSelect.append("JOIN $tableName ON $keyTableName.$primaryKeyName = $tableName.$primaryKeyName")

        sqlSelect.append("WHERE $primaryKeyName = $id\n")

        sqlSelect.append(";")
        println(sqlSelect.toString())
        return sqlSelect.toString()
    }

    private fun transform(rs: ResultSet): Any {
        getTableName(clazz) // Check if entity class

        val values = this.getColumnNamesWithInheritance(clazz).map {
                prop -> prop to rs.getObject(prop)
        }

        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }

    private fun findWithRelationsTransform(rs: ResultSet): Any {
        val props = extractAllPropertiesWithInheritance(clazz)

        val values = props.map { prop ->
            if (isRelationalColumn(prop)) {
                val relationPair = RelationsHandler(clazz, prop, rs).handleRelation()
                if (relationPair != null) return relationPair
            }

            // normal columns
            val columnName = getColumnName(prop)
            return columnName to rs.getObject(columnName)
        }

        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }
}