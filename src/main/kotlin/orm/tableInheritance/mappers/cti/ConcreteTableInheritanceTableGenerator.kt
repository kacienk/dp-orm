package orm.tableInheritance.mappers.cti

import orm.EntityProcessor
import orm.tableInheritance.ITableGenerator
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

class ConcreteTableInheritanceTableGenerator: ITableGenerator, IConcreteTableInheritance, EntityProcessor() {
    private val concreteClassList: MutableList<KClass<*>> = mutableListOf()
    override fun add(clazz: KClass<*>) {
        if (!clazz.isAbstract) {
            concreteClassList.add(clazz)
        }
    }

    override fun parse(): String {
        val allTablesBuilder = StringBuilder()
        concreteClassList.forEach { concreteClass -> allTablesBuilder.append(createTable(concreteClass))}
        return allTablesBuilder.toString()
    }

    private fun createTable(concreteClass: KClass<*>): String {
        val tableBuilder = StringBuilder()

        val tableName = getTableName(concreteClass)

        tableBuilder.append("create table $tableName (\n")

        val baseClass = concreteClass.superclasses.firstOrNull()

        if (baseClass != null) {
            val allPropsSql = generateSqlForAllProps(baseClass)
            tableBuilder.append(allPropsSql)
        }

        val classAllPropsSql = generateSqlForAllProps(concreteClass)
        tableBuilder.append(classAllPropsSql)

        val primaryKeyName = getPrimaryKeyName(concreteClass)
        val primaryKeySql = generateSqlForPrimaryKey(primaryKeyName)
        tableBuilder.append(primaryKeySql)

        tableBuilder.append(");\n\n")

        return tableBuilder.toString()
    }
}