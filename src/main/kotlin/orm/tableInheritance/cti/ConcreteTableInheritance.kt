package orm.tableInheritance.cti

import orm.tableInheritance.TableInheritanceMapper

class ConcreteTableInheritanceMapper: TableInheritanceMapper {
    override fun createTable(): Boolean {
        // Implementation for creating a table (if needed)
        throw NotImplementedError("This function is not implemented yet.")
    }

    override fun insert(entity: Any): Boolean {
        // Implementation for inserting a record
        throw NotImplementedError("This function is not implemented yet.")
    }

    override fun find(id: Long): Any? {
        // Implementation for finding a record by ID
        throw NotImplementedError("This function is not implemented yet.")
    }

    override fun findAll(): List<Any> {
        // Implementation for retrieving all records
        throw NotImplementedError("This function is not implemented yet.")
    }

    override fun count(): Long {
        // Implementation for getting the total number of records
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