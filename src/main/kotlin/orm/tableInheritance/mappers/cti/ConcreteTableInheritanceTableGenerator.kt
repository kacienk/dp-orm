package orm.tableInheritance.mappers.cti

import generateSqlForPrimaryKey
import orm.tableInheritance.TableSqlGenerator
import kotlin.reflect.KClass

class ConcreteTableInheritanceTableGenerator: TableSqlGenerator() {
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
        inheritanceMap.forEach { (baseClass, classList) -> classList.forEach {
            childClass -> allTablesBuilder.append(createTable(baseClass, childClass)) }
        }
        return allTablesBuilder.toString()
    }

    private fun createTable(baseClass: KClass<*>, childClass: KClass<*>): String {
        val tableBuilder = StringBuilder()

        val tableName = getTableName(childClass)

        tableBuilder.append("create table $tableName (\n")

        val allPropsSql = generateSqlForAllProps(baseClass)
        tableBuilder.append(allPropsSql)

        val classAllPropsSql = generateSqlForAllProps(childClass)
        tableBuilder.append(classAllPropsSql)

        val primaryKeySql = generateSqlForPrimaryKey(baseClass)
        tableBuilder.append(primaryKeySql)

        tableBuilder.append(");\n\n")

        return tableBuilder.toString()
    }
}