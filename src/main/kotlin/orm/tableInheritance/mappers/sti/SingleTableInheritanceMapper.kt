package orm.tableInheritance.mappers.sti

import orm.EntityProcessor
import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.JoinColumn
import orm.tableInheritance.ITableInheritanceMapper
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*


class SingleTableInheritanceMapper(private val clazz: KClass<*>): ITableInheritanceMapper, EntityProcessor() {
    override fun insert(entity: Any): Boolean {
        // Implementation for inserting a record
        throw NotImplementedError("This function is not implemented yet.")
    }

    override fun find(id: Long?): Any? {
        val sqlOnlyEntitySelect = findOnlyEntitySql(id)
        // execute select on only entity query
        sqlOnlyEntitySelect.execAndMap { resultSet ->
            // TODO map to instance of clazz
        }

        return null
    }

    override fun update(entity: Any): Boolean {
        // Implementation for updating a record
        throw NotImplementedError("This function is not implemented yet.")
    }

    override fun remove(id: Long): Boolean {
        // Implementation for removing a record by ID
        throw NotImplementedError("This function is not implemented yet.")
    }

    override fun query(q: String): Any? {
        // Implementation for executing custom queries
        throw NotImplementedError("This function is not implemented yet.")
    }

    private fun extractMostBaseClass(clazz: KClass<*>): KClass<*> {
        val baseClass = clazz.superclasses.firstOrNull() ?: return clazz
        baseClass.findAnnotation<Entity>() ?: return clazz
        return extractMostBaseClass(baseClass)
    }

    private fun getColumnNamesWithInheritanceSql(entityClass: KClass<*>): String {
        val columnNamesBuilder = StringBuilder()

        columnNamesBuilder.append(getColumnNamesSql(entityClass))

        entityClass.superclasses.forEach { superclass ->
            if (superclass.findAnnotation<Entity>() != null) {
                columnNamesBuilder.append(", ")
                columnNamesBuilder.append(this.getColumnNamesWithInheritanceSql(superclass))
            }
        }

        return columnNamesBuilder.toString()
    }

    private fun findOnlyEntitySql(id: Long?): String {
        val selectBuilder = StringBuilder("SELECT ")

        val columnNames = getColumnNamesWithInheritanceSql(clazz)
        selectBuilder.append("$columnNames\n")

        val mostBaseClass =this.extractMostBaseClass(clazz)
        val tableName = getTableName(mostBaseClass)
        selectBuilder.append("FROM $tableName\n")

        if (id != null) {
            val primaryKey = getPrimaryKeyName(mostBaseClass)
            selectBuilder.append("WHERE $primaryKey = $id\n")
        }

        selectBuilder.append(";")
        return selectBuilder.toString()
    }
}