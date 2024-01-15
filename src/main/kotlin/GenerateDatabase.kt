import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import orm.decorators.Entity

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Usage: kotlin -classpath <path-to-kotlin-runtime> -script script.kts <kotlin-files>")
        return
    }

    val files = args[0].split(",").map { it.trim() }

    val entityClasses = scanForEntities(files)

    if (entityClasses.isNotEmpty()) {
        val createSql = generateCreateSql(entityClasses)
        println(createSql)
        writeToFile("create.sql", createSql)
        println("create.sql file generated successfully.")
    } else {
        println("No classes with @Entity annotation found.")
    }
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

fun generateCreateSql(entityClasses: List<KClass<*>>): String {
    val createSql = StringBuilder()

    for (entityClass in entityClasses) {
        val tableName = entityClass.findAnnotation<Entity>()?.tableName ?: entityClass.simpleName

        createSql.append("CREATE TABLE $tableName (\n")

        val properties = entityClass.memberProperties
        for ((index, prop) in properties.withIndex()) {
            val propName = prop.name
            val propType = prop.returnType.toString().substringAfterLast(".")
            createSql.append("  $propName $propType")

            if (index < properties.size - 1) {
                createSql.append(",\n")
            } else {
                createSql.append("\n")
            }
        }

        createSql.append(");\n\n")
    }

    return createSql.toString()
}

fun writeToFile(fileName: String, content: String) {
    java.io.File(fileName).writeText(content)
}