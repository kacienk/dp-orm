package orm.tableInheritance

import orm.EntityProcessor
import orm.SqlTypeMapper
import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.JoinColumn
import orm.decorators.ManyToMany
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*


class DatabaseGenerator: EntityProcessor() {

    fun generateDatabase(entities: List<KClass<*>>) {
        if (entities.isEmpty())
            return println("No classes with @Entity annotation found.")

        val createSql = generateCreateSql(entities)
        println(createSql)
        writeToFile("create.sql", createSql)
        println("create.sql file generated successfully.")
    }

    private fun writeToFile(fileName: String, content: String) {
        java.io.File(fileName).writeText(content)
    }

    private fun generateCreateSql(entityClasses: List<KClass<*>>): String {
        val createSql = StringBuilder()

        val tableDefinitions = generateSqlTableDefinitions(entityClasses)
        createSql.append(tableDefinitions)
        val manyToManyTableDefinitions = generateSqlForManyToManyTableDefinitions(entityClasses)
        createSql.append(manyToManyTableDefinitions)
        val relationDefinitions = generateSqlRelationDefinitions(entityClasses)
        createSql.append(relationDefinitions)

        return createSql.toString()
    }

    private fun generateSqlTableDefinitions(entityClasses: List<KClass<*>>): String {
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

    private fun generateSqlForProp(prop: KProperty1<out Any, *>): String {
        val propSql = StringBuilder()
        val typeMapper = SqlTypeMapper()

        val columnAnnotation = prop.findAnnotation<Column>()
        val joinColumnAnnotation = prop.findAnnotation<JoinColumn>()
        if (columnAnnotation == null && joinColumnAnnotation == null)
            return propSql.toString()

        var nullInfo = ""
        var propName = ""
        if (joinColumnAnnotation != null) {
            nullInfo = if (joinColumnAnnotation.nullable) "" else " not null"
            propName = joinColumnAnnotation.name.ifEmpty { prop.name }
        }
        else if (columnAnnotation != null)
        {
            nullInfo = if (columnAnnotation.nullable) "" else " not null"
            propName = columnAnnotation.name.ifEmpty { prop.name }
        }
        if (propName.isEmpty()) throw IllegalArgumentException("There is no column name")

        val propType = typeMapper.mapToSqlType(prop.returnType.classifier as KClass<*>)

        propSql.append("  $propName $propType$nullInfo,\n")

        return propSql.toString()
    }

    private fun generateSqlForPrimaryKey(entityClass: KClass<*>): String {
        val primaryKeyName = getPrimaryKeyName(entityClass)
        return "  primary key ($primaryKeyName)\n"
    }

    private fun generateSqlForManyToManyTableDefinitions(entityClasses: List<KClass<*>>): String {
        val manyToManyTableDefinitions = StringBuilder()

        val listOfRelatedEntities = getListOfSetsForManyToManyRelations(entityClasses)
        for (relatedEntities in listOfRelatedEntities) {
            val manyToManyTableSql = generateSqlForManyToManyTable(relatedEntities)
            manyToManyTableDefinitions.append(manyToManyTableSql)
        }

        return manyToManyTableDefinitions.toString()
    }

    private fun getListOfSetsForManyToManyRelations(entityClasses: List<KClass<*>>): List<Set<KClass<*>>> {
        val listOfRelations: MutableList<Set<KClass<*>>> = mutableListOf()

        for (entityClass in entityClasses) {
            val propsWithManyToMany = entityClass.declaredMemberProperties.filter { prop ->
                prop.annotations.any { it is ManyToMany }
            }

            for (prop in propsWithManyToMany)
            {
                val manyToManyRelationSet = getManyToManyRelationSet(entityClass, prop)
                if (!listOfRelations.contains(manyToManyRelationSet))
                    listOfRelations.add(manyToManyRelationSet)
            }
        }

        return listOfRelations
    }

    private fun getManyToManyRelationSet(entityClass: KClass<*>, prop: KProperty1<out Any, *>): Set<KClass<*>> {
        if (!prop.returnType.isSubtypeOf(Collection::class.createType()))
            throw IllegalArgumentException("Field marked with @ManyToMany must be of type Collection" +
                    " (${entityClass.simpleName}.${prop.name})")
        val propClassTypeArgument = prop.returnType.arguments.firstOrNull()
            ?: throw IllegalArgumentException("Type of the field marked with @ManyToMany must have an argument" +
                    " (${entityClass.simpleName}.${prop.name})")
        val relatedFieldType = propClassTypeArgument.type
            ?: throw  IllegalArgumentException(
                "Type of the field marked with @ManyToMany cannot have a star projection argument" +
                        " (${entityClass.simpleName}.${prop.name})"
            )
        val relatedFieldClass = relatedFieldType.classifier as KClass<*>
        relatedFieldClass.findAnnotation<Entity>() ?: throw  IllegalArgumentException(
            "Argument of the type of the field marked with @ManyToMany must be annotated with @Entity" +
                    " (${entityClass.simpleName}.${prop.name})"
        )
        relatedFieldClass.declaredMemberProperties.find { relatedClassProp ->
            relatedClassProp.annotations.any { it is ManyToMany }
                    && relatedClassProp.returnType.isSubtypeOf(Collection::class.createType())
                    && relatedClassProp.returnType.arguments.firstOrNull()?.type?.classifier as KClass<*> == entityClass
        } ?: throw IllegalArgumentException(
            "Related field $relatedFieldClass does not have proper corresponding field marked with @ManyToMany" +
                    " (${entityClass.simpleName}.${prop.name})"
        )

        return setOf(entityClass, relatedFieldClass)
    }

    private fun generateSqlForManyToManyTable(relatedEntities: Set<KClass<*>>): String {
        val manyToManyTable = StringBuilder()

        val firstTable = relatedEntities.first()
        val secondTable = relatedEntities.last()
        val firstTableEntity = firstTable.findAnnotation<Entity>()
            ?: throw IllegalArgumentException("Related class must be marked with @Entity (${firstTable.simpleName})")
        val secondTableEntity = secondTable.findAnnotation<Entity>()
            ?: throw IllegalArgumentException("Related class must be marked with @Entity (${secondTable.simpleName})")
        val firstTableName = firstTableEntity.tableName.ifEmpty { firstTable.simpleName }
        val secondTableName = secondTableEntity.tableName.ifEmpty { secondTable.simpleName }
        manyToManyTable.append("create table ${firstTableName}_$secondTableName (\n")

        val firstTablePrimaryKeyName = getPrimaryKeyName(firstTable)
        val secondTablePrimaryKeyName = getPrimaryKeyName(secondTable)
        manyToManyTable.append(
            "  primary key (${firstTableName}_$firstTablePrimaryKeyName, ${secondTableName}_$secondTablePrimaryKeyName),\n"
        )
        manyToManyTable.append(
            "  foreign key (${firstTableName}_$firstTablePrimaryKeyName)" +
                    " references $firstTableName($firstTablePrimaryKeyName),\n"
        )
        manyToManyTable.append(
            "  foreign key (${secondTableName}_$secondTablePrimaryKeyName)" +
                    " references $secondTableName($secondTablePrimaryKeyName)\n"
        )
        manyToManyTable.append(");\n\n")

        return manyToManyTable.toString()
    }

    private fun generateSqlRelationDefinitions(entityClasses: List<KClass<*>>): String {
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
                val relationSql = generateSqlRelation(entityClass, joinProp)
                relationDefinitions.append(relationSql)
            }
        }

        return relationDefinitions.toString()
    }

    private fun generateSqlRelation(
        entityClass: KClass<*>, joinProp: KProperty1<out Any, *>
    ): String {
        val relation = StringBuilder()
        val joinColumnAnnotation = joinProp.findAnnotation<JoinColumn>()
            ?: throw IllegalArgumentException(
                "Join property field must be marked with @JoinColumn (${entityClass.simpleName}.${joinProp.name})"
            )

        val constraintUuid = UUID.randomUUID()
        val constraintUuidAsString = constraintUuid.toString().replace('-', '_')
        relation.append("  add constraint $constraintUuidAsString\n")

        val foreignKeyColumnName = joinColumnAnnotation.name.ifEmpty { joinProp.name }
        relation.append("  foreign key ($foreignKeyColumnName)\n")

        val referencedClass: KClass<*> = joinProp.returnType.classifier as KClass<*>
        val referencedClassEntityAnnotation = referencedClass.findAnnotation<Entity>()
            ?: throw IllegalArgumentException(
                "Type of the field marked with @JoinColumn must be marked with " +
                        "@Entity (${entityClass.simpleName}.${joinProp.name})"
            )
        val referencedClassName = referencedClassEntityAnnotation.tableName.ifEmpty { referencedClass.simpleName }
        val primaryKeyName = getPrimaryKeyName(referencedClass)
        relation.append("  references $referencedClassName ($primaryKeyName)\n\n")

        return relation.toString()
    }
}