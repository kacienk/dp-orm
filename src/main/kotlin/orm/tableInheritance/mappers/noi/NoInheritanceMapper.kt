package orm.tableInheritance.mappers.noi

import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import orm.EntityProcessor
import orm.tableInheritance.ITableInheritanceMapper
import java.lang.StringBuilder
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

class NoInheritanceMapper(private val clazz: KClass<*>): ITableInheritanceMapper, EntityProcessor() {
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

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun find(id: Long?): Any? {
        val sqlSelect = StringBuilder()
        val tableName = getTableName(clazz)
        val primaryKeyName = getPrimaryKeyName(clazz)

        sqlSelect.append("select ${getColumnNamesSql(clazz)}")
        sqlSelect.append(" from $tableName")
        sqlSelect.append(" where $primaryKeyName = $id")
        val sqlStatement = sqlSelect.toString()

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

        transaction {
            TransactionManager.current().exec(sqlStatement)
        }

        return true
    }

    override fun remove(id: Long): Boolean {
        val sqlUpdate = StringBuilder()
        val tableName = getTableName(clazz)
        val primaryKeyColumn = getPrimaryKeyName(clazz)

        sqlUpdate.append("delete from $tableName where $primaryKeyColumn = $id")
        val sqlStatement = sqlUpdate.toString()

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

        val values = clazz.declaredMemberProperties.filter {
            prop -> getColumnName(prop) != null
        }.map { prop -> getColumnName(prop) to rs.getObject(getColumnName(prop)) }
        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }
}