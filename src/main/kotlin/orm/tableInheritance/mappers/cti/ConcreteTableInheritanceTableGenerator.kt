package orm.tableInheritance.mappers.cti

import orm.EntityProcessor
import orm.tableInheritance.ITableGenerator
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses

class ConcreteTableInheritanceTableGenerator: ITableGenerator, IConcreteTableInheritance, EntityProcessor() {
    private val concreteClasses: MutableList<KClass<*>> = mutableListOf()
    private val abstractClasses: MutableList<KClass<*>> = mutableListOf()
    override fun add(clazz: KClass<*>) {
        if (!clazz.isAbstract) concreteClasses.add(clazz) else abstractClasses.add(clazz)
    }

    override fun parse(): String {
        val allTablesBuilder = StringBuilder()
        abstractClasses.forEach { concreteClass -> allTablesBuilder.append(createKeysTable(concreteClass))}
        concreteClasses.forEach { abstractClass -> allTablesBuilder.append(createConcreteTable(abstractClass))}
        return allTablesBuilder.toString()
    }

    private fun createKeysTable (abstractClass: KClass<*>): String {
        val tableName = getTableName(abstractClass) + "Keys"

        val tableBuilder = StringBuilder()

        tableBuilder.append("CREATE TABLE $tableName (\n")

        val primaryKeyName = getPrimaryKeyName(abstractClass)
        tableBuilder.append("$primaryKeyName int NOT NULL,\n")

        val primaryKeySql = generateSqlForPrimaryKey(primaryKeyName)
        tableBuilder.append(primaryKeySql)
        tableBuilder.append(");\n\n")
        return tableBuilder.toString()
    }

    private fun extractAllPropertiesWithInheritance(clazz: KClass<*>): List<KProperty1<out Any, *>> {
        val mostBaseClass = extractMostBaseClass(clazz)
        val notEntityClass = mostBaseClass.superclasses.firstOrNull() ?: return emptyList()

        return clazz.memberProperties.filter { prop -> prop !in notEntityClass.memberProperties }
    }

    private fun createConcreteTable(concreteClass: KClass<*>): String {
        val tableBuilder = StringBuilder()

        val tableName = getTableName(concreteClass)

        tableBuilder.append("create table $tableName (\n")

        extractAllPropertiesWithInheritance(concreteClass).forEach { prop ->
            tableBuilder.append(generateSqlForProp(prop))
        }

        val mostBaseClass = extractMostBaseClass(concreteClass)
        val primaryKeyName = getPrimaryKeyName(mostBaseClass)
        val primaryKeySql = generateSqlForPrimaryKey(primaryKeyName) + ","
        tableBuilder.append(primaryKeySql)

        tableBuilder.append("FOREIGN KEY ($primaryKeyName) REFERNCES ${getTableName(mostBaseClass)}Keys ($primaryKeyName)")

        tableBuilder.append(");\n\n")

        return tableBuilder.toString()
    }
}