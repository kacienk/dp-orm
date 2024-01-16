import orm.SqlTypeMapper
import orm.decorators.Column
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import orm.decorators.Entity
import orm.decorators.JoinColumn
import orm.decorators.PrimaryKey
import java.util.*
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

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

    val tableDefinitions = generateSqlTableDefinitions(entityClasses)
    createSql.append(tableDefinitions)
    val relationDefinitions = generateSqlRelationDefinitions(entityClasses)
    createSql.append(relationDefinitions)

    return createSql.toString()
}

fun generateSqlTableDefinitions(entityClasses: List<KClass<*>>): String {
    val tablesDefinitions = StringBuilder()

    for (entityClass in entityClasses) {
        val entityAnnotation = entityClass.findAnnotation<Entity>() ?: continue
        val tableName = entityAnnotation.tableName.ifEmpty { entityClass.simpleName }

        tablesDefinitions.append("create table $tableName (\n")

        val properties = entityClass.memberProperties
        for (prop in properties) {
            val sqlForProp = generateSqlForProp(prop)
            tablesDefinitions.append(sqlForProp)
        }

        val primaryKeySql = generateSqlForPrimaryKey(entityClass)
        tablesDefinitions.append(primaryKeySql)

        tablesDefinitions.append(");\n\n")
    }

    return tablesDefinitions.toString()
}

fun generateSqlForProp(prop: KProperty1<out Any, *>): String {
    val propSql = StringBuilder()
    val typeMapper = SqlTypeMapper()

    val columnAnnotation = prop.findAnnotation<Column>()
    val joinColumnAnnotation = prop.findAnnotation<JoinColumn>()
    if (columnAnnotation == null && joinColumnAnnotation == null)
        return propSql.toString()

    var nullInfo = ""
    var propName = ""
    if (joinColumnAnnotation != null) {
        nullInfo = if (joinColumnAnnotation.nullable) "" else " is not null"
        propName = joinColumnAnnotation.name.ifEmpty { prop.name }
    }
    else if (columnAnnotation != null)
    {
        nullInfo = if (columnAnnotation.nullable) "" else " is not null"
        propName = columnAnnotation.name.ifEmpty { prop.name }
    }
    if (propName.isEmpty()) throw IllegalArgumentException("There is no column name")

    val propType = typeMapper.mapToSqlType(prop.returnType.classifier as KClass<*>)

    propSql.append("  $propName $propType$nullInfo,\n")

    return propSql.toString()
}

fun generateSqlForPrimaryKey(entityClass: KClass<*>): String {
    val primaryKeyName = getPrimaryKeyName(entityClass)
    return "  primary key ($primaryKeyName)\n"
}

fun generateSqlRelationDefinitions(entityClasses: List<KClass<*>>): String {
    val relationDefinitions = StringBuilder()

    for (entityClass in entityClasses) {
        val entityAnnotation = entityClass.findAnnotation<Entity>() ?: continue
        val tableName = entityAnnotation.tableName.ifEmpty { entityClass.simpleName }

        val propsWithJoinColumn = entityClass.declaredMemberProperties.filter { prop ->
            prop.annotations.any { it is JoinColumn }
        }
        if (propsWithJoinColumn.isEmpty())
            continue

        for (joinProp in propsWithJoinColumn){
            relationDefinitions.append("alter table $tableName\n")
            val relationSql = generateSqlRelation(joinProp)
            relationDefinitions.append(relationSql)
        }
    }

    return relationDefinitions.toString()
}

fun generateSqlRelation(
    joinProp: KProperty1<out Any, *>
): String {
    val relation = StringBuilder()
    val joinColumnAnnotation = joinProp.findAnnotation<JoinColumn>()
        ?: throw IllegalArgumentException("Join property field must be marked with @JoinColumn")

    val constraintUuid = UUID.randomUUID()
    val constraintUuidAsString = constraintUuid.toString().replace('-', '_')
    relation.append("  add constraint $constraintUuidAsString\n")

    val foreignKeyColumnName = joinColumnAnnotation.name.ifEmpty { joinProp.name }
    relation.append("  foreign key ($foreignKeyColumnName)\n")

    val referencedClass: KClass<*> = joinProp.returnType.classifier as KClass<*>
    val referencedClassEntityAnnotation = referencedClass.findAnnotation<Entity>()
        ?: throw IllegalArgumentException("Type of the field marked with @JoinColumn must be marked with @Entity")
    val referencedClassName = referencedClassEntityAnnotation.tableName.ifEmpty { referencedClass.simpleName }
    val primaryKeyName = getPrimaryKeyName(referencedClass)
    relation.append("  references $referencedClassName ($primaryKeyName)\n\n")

    return relation.toString()
}

fun getPrimaryKeyName(entityClass: KClass<*>): String {
    val entityPrimaryKey = entityClass.declaredMemberProperties.find { prop ->
        prop.annotations.any { it is PrimaryKey }
    } ?: throw IllegalArgumentException("Entity must have a property marked with @PrimaryKey")
    val primaryKeyColumnAnnotation = entityPrimaryKey.findAnnotation<Column>()
        ?: throw IllegalArgumentException("Primary key field must be marked with @Column")
    return primaryKeyColumnAnnotation.name.ifEmpty { entityPrimaryKey.name }
}

fun writeToFile(fileName: String, content: String) {
    java.io.File(fileName).writeText(content)
}