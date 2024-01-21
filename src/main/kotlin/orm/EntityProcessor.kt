package orm

import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.JoinColumn
import orm.decorators.PrimaryKey
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

import java.sql.ResultSet
import org.jetbrains.exposed.sql.transactions.TransactionManager


abstract class EntityProcessor {
    protected fun getPrimaryKeyProp(entityClass: KClass<*>): KProperty1<out Any, *> {
        return entityClass.declaredMemberProperties.find { prop ->
            prop.annotations.any { it is PrimaryKey }
        } ?: throw IllegalArgumentException("Entity must have a property marked with @PrimaryKey ($entityClass)")
    }

    protected fun getPrimaryKeyName(entityClass: KClass<*>): String {
        val entityPrimaryKey = getPrimaryKeyProp(entityClass)
        val primaryKeyColumnAnnotation = entityPrimaryKey.findAnnotation<Column>()
            ?: throw IllegalArgumentException(
                "Primary key field must be marked with @Column ($entityClass.${entityPrimaryKey.name})"
            )
        return primaryKeyColumnAnnotation.name.ifEmpty { entityPrimaryKey.name }
    }

    protected fun getTableName(entityClass: KClass<*>): String? {
        val entityAnnotation = entityClass.findAnnotation<Entity>()
            ?: throw IllegalArgumentException("Entity must be marked with @Entity annotation ($entityClass)")
        return entityAnnotation.tableName.ifEmpty { entityClass.simpleName }
    }

    protected fun getColumnNamesSql(entityClass: KClass<*>): String {
        val columnNamesBuilder = StringBuilder()

        for ((index, prop) in entityClass.declaredMemberProperties.withIndex()) {
            val columnName = getColumnName(prop) ?: continue
            columnNamesBuilder.append(columnName)

            if (index + 1 < entityClass.declaredMemberProperties.size) {
                columnNamesBuilder.append(", ")
            }
            else {
                columnNamesBuilder.append(" ")
            }
        }

        return columnNamesBuilder.toString()
    }

    protected fun getColumnName(prop: KProperty1<out Any, *>): String? {
        val columnAnnotation = prop.findAnnotation<Column>()
        val joinColumnAnnotation = prop.findAnnotation<JoinColumn>()
        if (columnAnnotation == null && joinColumnAnnotation == null)
            return null

        return joinColumnAnnotation?.name?.ifEmpty { prop.name } ?: columnAnnotation?.name?.ifEmpty { prop.name }
    }

    protected fun <T:Any> String.execAndMap(transform : (ResultSet) -> T) : List<T> {
        val result = arrayListOf<T>()
        TransactionManager.current().exec(this) { rs ->
            while (rs.next()) {
                result += transform(rs)
            }
        }
        return result
    }

    protected fun formatValue(value: Any?): String {
        // Implement logic to format the value based on its type
        // This is just a simple example, you may need to handle different types appropriately
        return when (value) {
            is String -> "'$value'"
            is Number -> value.toString()
            is Boolean -> value.toString()
            // Add more cases as needed
            else -> "NULL"
        }
    }
}