package orm.decorators;

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GeneratedField(
    val generator: String = ""
)

