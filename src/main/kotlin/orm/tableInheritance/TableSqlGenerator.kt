package orm.tableInheritance

import orm.SqlTypeMapper
import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.JoinColumn
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses

abstract class TableSqlGenerator {
    abstract fun add(clazz: KClass<*>)

    abstract fun parse(): String

    protected fun extractMostBaseClass(clazz: KClass<*>): KClass<*> {
        val baseClass = clazz.superclasses.firstOrNull() ?: return clazz
        baseClass.findAnnotation<Entity>() ?: return clazz
        return extractMostBaseClass(baseClass)
    }

    protected fun getTableName(clazz: KClass<*>): String? {
        return clazz.findAnnotation<Entity>()?.tableName?.ifEmpty { clazz.simpleName }
    }

    protected fun generateSqlForAllProps(clazz: KClass<*>): String {
        val allPropsSql = StringBuilder()
        val baseClass = clazz.superclasses.firstOrNull() ?: clazz
        val onlyClassProps = clazz.memberProperties.filter {
                prop -> prop.name !in baseClass.memberProperties.map { it.name }
        }
        for (prop in clazz.declaredMemberProperties) {
            val sqlForProp = generateSqlForProp(prop)
            allPropsSql.append(sqlForProp)
        }
        return allPropsSql.toString()
    }

    protected fun generateSqlForProp(prop: KProperty1<out Any, *>): String {
        val propSql = StringBuilder()
        val typeMapper = SqlTypeMapper()

        val columnAnnotation = prop.findAnnotation<Column>()
        val joinColumnAnnotation = prop.findAnnotation<JoinColumn>()
        if (columnAnnotation == null && joinColumnAnnotation == null)
            return propSql.toString()

        var nullInfo = ""
        var propName = ""
        if (joinColumnAnnotation != null) {
            nullInfo = if (joinColumnAnnotation.nullable) "" else " not null"
            propName = joinColumnAnnotation.name.ifEmpty { prop.name }
        }
        else if (columnAnnotation != null)
        {
            nullInfo = if (columnAnnotation.nullable) "" else " not null"
            propName = columnAnnotation.name.ifEmpty { prop.name }
        }
        if (propName.isEmpty()) throw IllegalArgumentException("There is no column name")

        val propType = typeMapper.mapToSqlType(prop.returnType.classifier as KClass<*>)

        propSql.append("  $propName $propType$nullInfo,\n")

        return propSql.toString()
    }

//     TODO move here functions from GenerateDatabase.kt to
//      be able to create NoInheritanceTableGenerator and
//      use methods from base (TableSqlGenerator) class
}