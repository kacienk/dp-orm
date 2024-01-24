package orm.tableInheritance.mappers.noi

import orm.EntityProcessor
import orm.tableInheritance.ITableGenerator
import kotlin.reflect.KClass

class NoInheritanceTableGenerator: ITableGenerator, EntityProcessor() {
    val classList: MutableList<KClass<*>> = mutableListOf()

    override fun add(clazz: KClass<*>) {
        classList.add(clazz)
    }

    override fun parse(): String {
        val allTablesBuilder = StringBuilder()
        classList.forEach { clazz -> allTablesBuilder.append(createTable(clazz)) }
        return allTablesBuilder.toString()
    }

    private fun createTable(clazz: KClass<*>): String {
        val tableBuilder = StringBuilder()

        val tableName = getTableName(clazz)

        tableBuilder.append("create table $tableName (\n")

        val allPropsSql = generateSqlForAllProps(clazz)

        tableBuilder.append(allPropsSql)

        val primaryKeyName = getPrimaryKeyName(clazz)
        val primaryKeySql = generateSqlForPrimaryKey(primaryKeyName)
        tableBuilder.append(primaryKeySql)

        tableBuilder.append(");\n\n")

        return tableBuilder.toString()
    }
}