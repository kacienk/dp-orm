import orm.decorators.*
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import orm.tableInheritance.DatabaseGenerator

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: kotlin -classpath <path-to-kotlin-runtime> -script script.kts <kotlin-files>")
        return
    }

    val files = args[0].split(",").map { it.trim() }

    val entityClasses = scanForEntities(files)

    DatabaseGenerator(false).generateDatabase(entityClasses)
}

fun scanForEntities(files: List<String>): List<KClass<*>> {
    val entityClasses = mutableListOf<KClass<*>>()

    for (file in files) {
        try {
            val kClazz = Class.forName(file).kotlin
            if (kClazz.findAnnotation<Entity>() != null) {
                entityClasses.add(kClazz)
            }
        } catch (e: ClassNotFoundException) {
            println("Error: Class not found for $file")
        }
    }

    return entityClasses
}

fun getPrimaryKeyName(entityClass: KClass<*>): String {
    val entityPrimaryKey = entityClass.declaredMemberProperties.find { prop ->
        prop.annotations.any { it is PrimaryKey }
    } ?: throw IllegalArgumentException("Entity must have a property marked with @PrimaryKey ($entityClass)")
    val primaryKeyColumnAnnotation = entityPrimaryKey.findAnnotation<Column>()
        ?: throw IllegalArgumentException(
            "Primary key field must be marked with @Column ($entityClass.${entityPrimaryKey.name})"
        )
    return primaryKeyColumnAnnotation.name.ifEmpty { entityPrimaryKey.name }
}

