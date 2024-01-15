package orm.tableInheritance

import kotlin.reflect.KClass
import orm.enums.InheritanceType
import orm.decorators.Inheritance
import orm.tableInheritance.cti.ConcreteTableInheritanceMapper
import orm.tableInheritance.sti.SingleTableInheritanceMapper

interface TableInheritanceMapper {
    fun createTable(): Boolean
    fun insert(entity: Any): Boolean
    fun find(id: Long): Any?
    fun findAll(): List<Any>
    fun count(): Long
    fun update(entity: Any): Boolean
    fun remove(id: Long): Boolean
    fun query(q: String): Any? // for hardcore SQL queries like using WHERE, ORDER BY etc. - who needs them btw?
}

class TableInheritanceFactory {
    fun createMapper(strategy: InheritanceType): TableInheritanceMapper {
        return when (strategy) {
            InheritanceType.SINGLE -> SingleTableInheritanceMapper()
            InheritanceType.CONCRETE -> ConcreteTableInheritanceMapper()
            else -> NoInheritanceMapper()
        }
    }

    fun detectStrategy(clazz: KClass<*>): InheritanceType? {
        val tableInheritanceAnnotation = clazz.annotations.find { it is Inheritance }
        return when (tableInheritanceAnnotation) {
            is Inheritance -> tableInheritanceAnnotation.strategy
            else -> null
        }
    }
}