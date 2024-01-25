package orm.tableInheritance

import kotlin.reflect.KClass

interface ITableInheritanceMapper {
    fun insert(entity: Any): Boolean
    fun find(id: Int? = null): Any?
    fun update(entity: Any): Boolean
    fun remove(id: Int): Boolean
    fun query(q: String): Any? // for hardcore SQL queries like using WHERE, ORDER BY etc. - who needs them btw?
    fun findWithoutRelations(id: Int, entityClass: KClass<*>): Any?
}