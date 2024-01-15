package orm.decorators

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class JoinColumn(
    val name: String = "",
    val nullable: Boolean = true
)
