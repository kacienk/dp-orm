package orm.tableInheritance.mappers.cti

import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import orm.decorators.Entity
import orm.tableInheritance.ITableInheritanceMapper
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import orm.EntityProcessor
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

        sqlInsert.append("INSERT INTO $tableName (")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { it.first!! })
        sqlInsert.append(") VALUES (")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { formatValue(it.second) })
        sqlInsert.append(");")

        val sqlStatement = sqlInsert.toString()

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun find(id: Long?): Any? {

        if (!clazz.isAbstract) {
            val sqlSelect = StringBuilder("SELECT ")

            val columnNames = getColumnNamesWithInheritanceSql(clazz)
            sqlSelect.append("$columnNames\n")

            val tableName = getTableName(clazz)
            sqlSelect.append("FROM $tableName\n")

            val primaryKey = getPrimaryKeyName(clazz)
            sqlSelect.append("WHERE $primaryKey = $id\n")

            sqlSelect.append(";")
            val sqlStatement = sqlSelect.toString()

            return sqlStatement.execAndMap(::transform)
        }
        else {
            val childrenClasses = getChildrenClasses(clazz)

            for (child in childrenClasses) {
                return smallerFind(id, child) ?: continue
            }

            val sqlSelect = StringBuilder("SELECT ")

            val columnNames = getColumnNamesWithInheritanceSql(childrenClasses.first())
            sqlSelect.append("$columnNames\n")

            val tableName = getTableName(childrenClasses.first())
            sqlSelect.append("FROM $tableName\n")

            val primaryKey = getPrimaryKeyName(childrenClasses.first())
            sqlSelect.append("WHERE $primaryKey = $id\n")

            sqlSelect.append(";")
            val sqlStatement = sqlSelect.toString()

            return sqlStatement.execAndMap(::transform)
        }
    }

    override fun update(entity: Any): Boolean {
        val sqlUpdate = StringBuilder()
        val castedEntity = clazz.cast(entity)
        val castedEntityClass = castedEntity::class
        val tableName = getTableName(clazz)

        val primaryKeyProp = getPrimaryKeyProp(clazz)
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

    override fun remove(id: Long): Boolean {
        val sqlUpdate = java.lang.StringBuilder()
        val tableName = getTableName(clazz)
        val primaryKeyColumn = getPrimaryKeyName(clazz)

        sqlUpdate.append("DELETE FROM $tableName WHERE $primaryKeyColumn = $id")
        val sqlStatement = sqlUpdate.toString()

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

    private fun smallerFind(id: Long?, childClass: KClass<*>): Any? {
        val sqlSelect = StringBuilder("SELECT ")

        val columnNames = getColumnNamesWithInheritanceSql(childClass)
        sqlSelect.append("$columnNames\n")

        val tableName = getTableName(childClass)
        sqlSelect.append("FROM $tableName\n")

        val primaryKey = getPrimaryKeyName(childClass)
        sqlSelect.append("WHERE $primaryKey = $id\n")

        sqlSelect.append(";")
        val sqlStatement = sqlSelect.toString()

        val results = sqlStatement.execAndMap(::transform)

        return results.ifEmpty { null }
    }

    private fun transform(rs: ResultSet): Any {
        getTableName(clazz) // Check if entity class

        val values = this.getColumnNamesWithInheritance(clazz).map {
                prop -> prop to rs.getObject(prop)
        }

        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }
}