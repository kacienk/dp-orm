package orm.tableInheritance.mappers.cti

import orm.tableInheritance.ITableInheritanceMapper
import kotlin.reflect.KClass

class ConcreteTableInheritanceMapper(private val clazz: KClass<*>): ITableInheritanceMapper {
    override fun insert(entity: Any): Boolean {
        // Implementation for inserting a record
        throw NotImplementedError("This function is not implemented yet.")
    }

    override fun find(id: Long?): Any? {
        // Implementation for finding a record by ID
        throw NotImplementedError("This function is not implemented yet.")
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
}