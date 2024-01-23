package orm.tableInheritance.mappers.sti

import generateSqlForPrimaryKey
import orm.tableInheritance.TableSqlGenerator
import kotlin.reflect.KClass

class SingleTableInheritanceTableGenerator: TableSqlGenerator() {
    private val inheritanceMap: MutableMap<KClass<*>, MutableList<KClass<*>>> = mutableMapOf()

    override fun add(clazz: KClass<*>) {
        if (inheritanceMap.containsKey(clazz))
            return

        val mostBaseClass = extractMostBaseClass(clazz)

        if (inheritanceMap.containsKey(mostBaseClass)) {
            inheritanceMap[mostBaseClass]!!.add(clazz)
        }
        else {
            inheritanceMap[mostBaseClass] = mutableListOf(clazz)
        }
    }

    override fun parse(): String {
        val allTablesBuilder = StringBuilder()
        inheritanceMap.forEach { (baseClass, classList) -> allTablesBuilder.append(createTable(baseClass, classList)) }
        return allTablesBuilder.toString()
    }

    private fun createTable(baseClass: KClass<*>, classList: MutableList<KClass<*>>): String {
        val tableBuilder = StringBuilder()

        val tableName = getTableName(baseClass)

        tableBuilder.append("create table $tableName (\n")

        val allPropsSql = generateSqlForAllProps(baseClass)
        tableBuilder.append(allPropsSql)

        for (clazz in classList) {
            val classAllPropsSql = generateSqlForAllProps(clazz)
            tableBuilder.append(classAllPropsSql)
        }

        val classTypePropSql = getClassTypePropSql()
        tableBuilder.append(classTypePropSql)

        val primaryKeySql = generateSqlForPrimaryKey(baseClass)
        tableBuilder.append(primaryKeySql)

        tableBuilder.append(");\n\n")

        return tableBuilder.toString()
    }

    private fun getClassTypePropSql(): String {
        return "  class_type VARCHAR (255) NOT NULL,\n"
    }
}