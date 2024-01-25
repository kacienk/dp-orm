package orm

import orm.decorators.Entity
import orm.decorators.PrimaryKey
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

class SqlTypeMapper {
    fun mapToSqlType(kotlinType: KClass<*>): String {
        if (kotlinType.findAnnotation<Entity>() != null)
        {
            val entityPrimaryKey = kotlinType.declaredMemberProperties.find { prop ->
                prop.annotations.any { it is PrimaryKey }
            } ?: throw IllegalArgumentException("Entity must have a property marked with @PrimaryKey")
            return mapToSqlType(entityPrimaryKey.returnType.classifier as KClass<*>)
        }

        return when (kotlinType) {
            String::class -> "varchar(255)"
            Int::class -> "integer"
            Int::class -> "bigint"
            Double::class -> "double"
            Boolean::class -> "boolean"
            else -> throw IllegalArgumentException("Unsupported Kotlin type: $kotlinType")
        }
    }
}