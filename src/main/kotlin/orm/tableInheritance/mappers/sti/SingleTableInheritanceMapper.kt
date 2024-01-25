package orm.tableInheritance.mappers.sti

import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import orm.EntityProcessor
import orm.decorators.*
import orm.tableInheritance.ITableInheritanceMapper
import orm.tableInheritance.RelationsHandler
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*


class SingleTableInheritanceMapper(private val clazz: KClass<*>): ITableInheritanceMapper, ISingleTableInheritance, EntityProcessor() {
    override fun insert(entity: Any): Boolean {
        val sqlInsert = StringBuilder()
        val castedEntity = clazz.cast(entity)
        val castedEntityClass = castedEntity::class
        val mostBaseClass = this.extractMostBaseClass(castedEntityClass)
        val tableName = getTableName(mostBaseClass)

        val columnNamesAndValues = extractAllPropertiesWithInheritance(castedEntityClass).filter { prop ->
            getColumnName(prop) != null
        }.map { prop -> getColumnName(prop) to prop.call(castedEntity) }

        sqlInsert.append("INSERT INTO $tableName (\n")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { it.first!! })
        sqlInsert.append(", class_type")
        sqlInsert.append("\n) VALUES (\n")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { formatValue(it.second) })
        sqlInsert.append(", '${getTableName(castedEntityClass)}'")
        sqlInsert.append("\n);")

        val sqlStatement = sqlInsert.toString()

        println(sqlStatement)

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun findWithoutRelations(id: Int, entityClass: KClass<*>): Any? {
        val sqlSelect = StringBuilder("SELECT ")

        val columnNames = getColumnNamesWithInheritanceSql(clazz)
        sqlSelect.append("$columnNames\n")

        val mostBaseClass = this.extractMostBaseClass(clazz)
        val tableName = getTableName(mostBaseClass)
        sqlSelect.append("FROM $tableName\n")

        if (id != null) {
            val primaryKey = getPrimaryKeyName(mostBaseClass)
            sqlSelect.append("WHERE $primaryKey = $id\n")
        }

        sqlSelect.append(";")
        val sqlStatement = sqlSelect.toString()

        println(sqlStatement)

        var result:Any? = null
        transaction {
            result = sqlStatement.execAndMap(::findWithRelationsTransform).firstOrNull()
        }
        return result
    }

    override fun find(id: Int?): Any? {
        val sqlSelect = StringBuilder("SELECT ")

        val columnNames = getColumnNamesWithInheritanceSql(clazz)
        sqlSelect.append("$columnNames\n")

        val mostBaseClass =this.extractMostBaseClass(clazz)
        val tableName = getTableName(mostBaseClass)
        sqlSelect.append("FROM $tableName\n")

        if (id != null) {
            val primaryKey = getPrimaryKeyName(mostBaseClass)
            sqlSelect.append("WHERE $primaryKey = $id\n")
        }
        else {
            sqlSelect.append("WHERE class_type = '${getTableName(clazz)}'")
        }

        sqlSelect.append(";")
        val sqlStatement = sqlSelect.toString()

        println(sqlStatement)

        var result:Any? = null
        transaction {
            result = sqlStatement.execAndMap(::findWithRelationsTransform).firstOrNull()
        }
        return result
    }

    override fun update(entity: Any): Boolean {
        val sqlUpdate = StringBuilder()

        val castedEntity = clazz.cast(entity)
        val castedEntityClass = castedEntity::class

        val mostBaseClass = this.extractMostBaseClass(castedEntityClass)
        val tableName = getTableName(mostBaseClass)

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

        println(sqlStatement)

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun remove(id: Int): Boolean {
        val sqlRemove = StringBuilder()
        val mostBaseClass = this.extractMostBaseClass(clazz)
        val tableName = getTableName(mostBaseClass)
        val primaryKeyColumn = getPrimaryKeyName(mostBaseClass)

        sqlRemove.append("DELETE FROM $tableName WHERE $primaryKeyColumn = $id;")
        val sqlStatement = sqlRemove.toString()

        println(sqlStatement)
        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun query(q: String): Any {
        return q.execAndMap(::transform)
    }

    private fun extractAllPropertiesWithInheritance(clazz: KClass<*>): List<KProperty1<out Any, *>> {
        val mostBaseClass = extractMostBaseClass(clazz)
        val notEntityClass = mostBaseClass.superclasses.firstOrNull() ?: return emptyList()

        return clazz.memberProperties.filter { prop -> prop !in notEntityClass.memberProperties }
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

    private fun transform(rs: ResultSet): Any {
        getTableName(clazz) // Check if entity class

        val values = extractAllPropertiesWithInheritance(clazz).map { prop ->
            val columnName = getColumnName(prop)
            if (isRelationalColumn(prop)) return columnName to null
            return columnName to rs.getObject(columnName)
        }

        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }

    private fun findWithRelationsTransform(rs: ResultSet): Any {
        val props = extractAllPropertiesWithInheritance(clazz)

        val values = props.map { prop ->
            if (isRelationalColumn(prop)) {
                val relationPair = RelationsHandler(clazz, prop, rs).handleRelation()
                if (relationPair != null) return@map relationPair
            }

            val columnName = getColumnName(prop)
            println(columnName)
            println(rs.getObject(columnName))
            // normal columns
            return@map columnName to rs.getObject(columnName)
        }

        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }

    private fun getRelationalProperties(): List<KProperty1<out Any, *>> {
        return extractAllPropertiesWithInheritance(clazz).filter { prop -> isRelationalColumn(prop) }
    }
}