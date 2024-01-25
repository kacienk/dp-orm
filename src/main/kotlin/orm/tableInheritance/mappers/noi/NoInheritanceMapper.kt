package orm.tableInheritance.mappers.noi

import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import orm.EntityProcessor
import orm.decorators.ManyToMany
import orm.decorators.ManyToOne
import orm.decorators.OneToMany
import orm.decorators.OneToOne
import orm.tableInheritance.ITableInheritanceMapper
import orm.tableInheritance.RelationsHandler
import java.lang.StringBuilder
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.cast
import kotlin.reflect.full.*

class NoInheritanceMapper(private val clazz: KClass<*>): INoInheritance, ITableInheritanceMapper, EntityProcessor() {
    override fun insert(entity: Any): Boolean {
        val sqlInsert = StringBuilder()
        val castedEntity = clazz.cast(entity)
        val tableName = getTableName(clazz)
        val columnNamesAndValues = castedEntity::class.declaredMemberProperties.filter { prop ->
            getColumnName(prop) != null
        }.map { prop -> getColumnName(prop) to prop.call(castedEntity) }

        sqlInsert.append("insert into $tableName (")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { it.first!! })
        sqlInsert.append(") values (")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { formatValue(it.second) })
        sqlInsert.append(");")

        val sqlStatement = sqlInsert.toString()

        println(sqlStatement)

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun find(id: Int?): Any? {
        val sqlSelect = StringBuilder()
        val tableName = getTableName(clazz)
        val primaryKeyName = getPrimaryKeyName(clazz)

        sqlSelect.append("select ${getColumnNamesSql(clazz)}")
        sqlSelect.append(" from $tableName")
        if (id != null)
            sqlSelect.append(" where $primaryKeyName = $id")
        sqlSelect.append(";")
        val sqlStatement = sqlSelect.toString()

        println(sqlStatement)
        var result:Any? = null
        transaction {
            result = sqlStatement.execAndMap(::findWithRelationsTransform).firstOrNull()
        }
        return result
    }

    override fun findWithoutRelations(id: Int, entityClass: KClass<*>): Any? {
        val sqlSelect = StringBuilder()
        val tableName = getTableName(clazz)
        val primaryKeyName = getPrimaryKeyName(clazz)

        sqlSelect.append("select ${getColumnNamesSql(clazz)}")
        sqlSelect.append(" from $tableName")
        if (id != null)
            sqlSelect.append(" where $primaryKeyName = $id")
        sqlSelect.append(";")
        val sqlStatement = sqlSelect.toString()

        println(sqlStatement)

        return sqlStatement.execAndMap(::transform).firstOrNull()
    }

    override fun update(entity: Any): Boolean {
        val sqlUpdate = StringBuilder()
        val castedEntity = clazz.cast(entity)
        val tableName = getTableName(clazz)
        val primaryKeyProp = getPrimaryKeyProp(clazz)
        val primaryKey = getColumnName(primaryKeyProp) to (
                primaryKeyProp.call(entity) ?: throw IllegalArgumentException(
                    "Updated entity must have value in field annotated with @PrimaryKey"
                )
        )

        val columnNamesAndValues = castedEntity::class.declaredMemberProperties.filter { prop ->
            getColumnName(prop) != null && prop.call(castedEntity) != null
        }.map { prop -> getColumnName(prop) to prop.call(castedEntity) }

        sqlUpdate.append("update $tableName set ")
        sqlUpdate.append(columnNamesAndValues.joinToString(", ") { "${it.first!!} = ${formatValue(it.second)}" })
        sqlUpdate.append(" where ${primaryKey.first} = ${formatValue(primaryKey.second)};")

        val sqlStatement = sqlUpdate.toString()

        println(sqlStatement)

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun remove(id: Int): Boolean {
        val sqlUpdate = StringBuilder()
        val tableName = getTableName(clazz)
        val primaryKeyColumn = getPrimaryKeyName(clazz)

        sqlUpdate.append("delete from $tableName where $primaryKeyColumn = $id;")
        val sqlStatement = sqlUpdate.toString()

        println(sqlStatement)

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun query(q: String): Any {
        return q.execAndMap(::transform)
    }

    private fun transform(rs: ResultSet): Any {
        getTableName(clazz) // Check if entity class

        val values = clazz.primaryConstructor?.parameters?.associate { param ->
            val propName = param.name
            val prop = clazz.declaredMemberProperties.find { it.name == propName }
            if (prop?.let { isRelationalColumn(it) } == true)
                return@associate param to null

            if (prop?.let { getColumnName(it) } != null){
                param to rs.getObject(getColumnName(prop))}
            else
                param to null
        } ?: emptyMap()
        val newEntity = clazz.primaryConstructor?.callBy(values)

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }
    private fun findWithRelationsTransform(rs: ResultSet):Any {
        val values = clazz.primaryConstructor?.parameters?.associate { param ->
            val propName = param.name
            val prop = clazz.declaredMemberProperties.find { it.name == propName }
            if (prop?.let { isRelationalColumn(it) } == true) {
                val relationPair = prop.let { RelationsHandler(clazz, it, rs).handleRelation() }
                if (relationPair != null) return@associate param to relationPair.second
            }

            val columnName = prop?.let { getColumnName(it) }
            println(columnName)
            println(rs.getObject(columnName))
            // normal columns
            return@associate param to rs.getObject(columnName)
        } ?: emptyMap()
        val newEntity = clazz.primaryConstructor?.callBy(values)

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }

}