package orm.tableInheritance.mappers.cti

import orm.decorators.Entity
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.superclasses

interface IConcreteTableInheritance {
    fun extractMostBaseClass(clazz: KClass<*>): KClass<*> {
        val baseClass = clazz.superclasses.firstOrNull() ?: return clazz
        baseClass.findAnnotation<Entity>() ?: return clazz
        return extractMostBaseClass(baseClass)
    } // TODO decide if this is necessary
}
