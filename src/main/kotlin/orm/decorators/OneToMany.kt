package orm.decorators

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class OneToMany(
    val mappedBy: String
)
