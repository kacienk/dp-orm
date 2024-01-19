package orm.tableInheritance

import kotlin.reflect.KClass
import orm.enums.InheritanceType
import orm.decorators.Inheritance
import orm.tableInheritance.mappers.NoInheritanceMapper
import orm.tableInheritance.mappers.cti.ConcreteTableInheritanceMapper
import orm.tableInheritance.mappers.sti.SingleTableInheritanceMapper

class TableInheritanceFactory {
    fun getMapper(clazz: KClass<*>): ITableInheritanceMapper {
        val strategy = detectStrategy(clazz)
        return when (strategy) {
            InheritanceType.SINGLE -> SingleTableInheritanceMapper(clazz)
            InheritanceType.CONCRETE -> ConcreteTableInheritanceMapper(clazz)
            else -> NoInheritanceMapper(clazz)
        }
    }

    private fun detectStrategy(clazz: KClass<*>): InheritanceType? {
        val tableInheritanceAnnotation = clazz.annotations.find { it is Inheritance }
        return when (tableInheritanceAnnotation) {
            is Inheritance -> tableInheritanceAnnotation.strategy
            else -> null
        }
    }
}

