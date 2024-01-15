package orm.tableInheritance

import kotlin.reflect.KClass
import orm.enums.InheritanceType
import orm.decorators.Inheritance
import orm.tableInheritance.cti.ConcreteTableInheritanceMapper
import orm.tableInheritance.sti.SingleTableInheritanceMapper

interface TableInheritanceMapper {
    // Add common methods for SingleTableInheritanceMapper and ConcreteTableInheritanceMapper here
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