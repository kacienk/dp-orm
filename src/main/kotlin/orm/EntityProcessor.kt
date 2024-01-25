package orm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

import java.sql.ResultSet
import org.jetbrains.exposed.sql.transactions.TransactionManager
import orm.decorators.*
import kotlin.reflect.full.hasAnnotation


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

        val filteredMembers = entityClass.declaredMemberProperties.filter {
            prop -> getColumnName(prop) != null;
        }
        for ((index, prop) in filteredMembers.withIndex()) {
            val columnName = getColumnName(prop) ?: continue
            columnNamesBuilder.append(columnName)

            if (index + 1 < filteredMembers.size) {
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

    protected fun isRelationalColumn(property: KProperty1<out Any, *>): Boolean {
        return (property.hasAnnotation<OneToOne>()
                || property.hasAnnotation<OneToMany>()
                || property.hasAnnotation<ManyToOne>()
                || property.hasAnnotation<ManyToMany>())
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
        var newValue = value
        if(value != null) {
            val entity = value::class.findAnnotation<Entity>()
            if (entity != null) {
                newValue = getPrimaryKeyProp(value::class).call(value)
            }
        }
        return when (newValue) {
            is String -> "'$newValue'"
            is Number -> newValue.toString()
            is Boolean -> newValue.toString()
            // Add more cases as needed
            else -> "NULL" // TODO issue with relations
        }
    }
}