package orm.tableInheritance

import orm.decorators.Entity
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.superclasses

interface ITableInheritanceMapper {
    fun insert(entity: Any): Boolean
    fun find(id: Long? = null): Any?
    fun update(entity: Any): Boolean
    fun remove(id: Long): Boolean
    fun query(q: String): Any? // for hardcore SQL queries like using WHERE, ORDER BY etc. - who needs them btw?
}