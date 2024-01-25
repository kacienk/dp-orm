package orm.tableInheritance.mappers.sti

import orm.EntityProcessor
import orm.tableInheritance.ITableGenerator
import kotlin.reflect.KClass

class SingleTableInheritanceTableGenerator: ITableGenerator, ISingleTableInheritance, EntityProcessor() {
    private val inheritanceMap: MutableMap<KClass<*>, MutableSet<KClass<*>>> = mutableMapOf()

    override fun add(clazz: KClass<*>) {
        if (inheritanceMap.containsKey(clazz)) {
            inheritanceMap[clazz]!!.add(clazz)
            return
        }

        val mostBaseClass = extractMostBaseClass(clazz)

        if (inheritanceMap.containsKey(mostBaseClass)) {
            inheritanceMap[mostBaseClass]!!.add(clazz)
        }
        else {
            inheritanceMap[mostBaseClass] = mutableSetOf(clazz)
        }
    }

    override fun parse(): String {
        val allTablesBuilder = StringBuilder()
        inheritanceMap.forEach { (baseClass, classList) -> allTablesBuilder.append(createTable(baseClass, classList)) }
        return allTablesBuilder.toString()
    }

    private fun createTable(baseClass: KClass<*>, classList: MutableSet<KClass<*>>): String {
        val tableBuilder = StringBuilder()

        val tableName = getTableName(baseClass)

        tableBuilder.append("create table $tableName (\n")

        for (clazz in classList) {
            val classAllPropsSql = generateSqlForAllProps(clazz)
            tableBuilder.append(classAllPropsSql)
        }

        val classTypePropSql = getClassTypePropSql()
        tableBuilder.append(classTypePropSql)

        val primaryKeyName = getPrimaryKeyName(baseClass)
        val primaryKeySql = generateSqlForPrimaryKey(primaryKeyName)
        tableBuilder.append(primaryKeySql)

        tableBuilder.append(");\n\n")

        return tableBuilder.toString()
    }

    private fun getClassTypePropSql(): String {
        return "  class_type VARCHAR (255) NOT NULL,\n"
    }
}