package orm.decorators

import orm.enums.InheritanceType

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Inheritance(
    val strategy: InheritanceType = InheritanceType.SINGLE
)