package orm.tableInheritance

import orm.SqlTypeMapper
import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.JoinColumn
import orm.decorators.ManyToMany
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*

interface ITableGenerator {
    fun add(clazz: KClass<*>)
    fun parse(): String

    fun generateSqlForAllProps(clazz: KClass<*>): String {
        val allPropsSql = StringBuilder()
        for (prop in clazz.declaredMemberProperties) {
            val sqlForProp = generateSqlForProp(prop)
            allPropsSql.append(sqlForProp)
        }
        return allPropsSql.toString()
    }

    fun generateSqlForProp(prop: KProperty1<out Any, *>): String {
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

    fun generateSqlForPrimaryKey(primaryKey: String): String {
        return "  primary key ($primaryKey)\n"
    }




//     TODO move here functions from GenerateDatabase.kt to
//      be able to create NoInheritanceTableGenerator and
//      use methods from base (TableSqlGenerator) class
}