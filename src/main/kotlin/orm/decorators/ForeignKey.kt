package orm.decorators;

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ForeignKey(
    val name: String = "",
    val foreignKeyDefinition = ""
)

