package orm.tableInheritance.mappers.sti

import orm.EntityProcessor
import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.JoinColumn
import orm.tableInheritance.ITableInheritanceMapper
import java.sql.ResultSet
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

        return sqlOnlyEntitySelect.execAndMap(::findTransform)
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
        return this.getColumnNamesWithInheritance(entityClass).joinToString(", ")
    }

    private fun getColumnNamesWithInheritance(entityClass: KClass<*>): MutableList<String> {
        val columnNames = mutableListOf<String>()

        entityClass.declaredMemberProperties.filter {
            prop -> getColumnName(prop) != null
        }.forEach { prop -> columnNames.add(getColumnName(prop)!!) } // nulls filtered out

        entityClass.superclasses.forEach { superclass ->
            if (superclass.findAnnotation<Entity>() != null) {
                columnNames.addAll(this.getColumnNamesWithInheritance(superclass))
            }
        }

        return columnNames
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

    private fun findTransform(rs: ResultSet): Any {
        getTableName(clazz) // Check if entity class

        val values = this.getColumnNamesWithInheritance(clazz).map {
            prop -> prop to rs.getObject(prop)
        }

        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }
}