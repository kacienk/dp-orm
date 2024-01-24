package orm.tableInheritance

import kotlin.reflect.KClass

interface ITableInheritanceMapper {
    fun insert(entity: Any): Boolean
    fun find(id: Long? = null): Any?
    fun update(entity: Any): Boolean
    fun remove(id: Long): Boolean
    fun query(q: String): Any? // for hardcore SQL queries like using WHERE, ORDER BY etc. - who needs them btw?
//    fun find_without_relations(id: Long, entityClass: KClass<*>): Any?
}