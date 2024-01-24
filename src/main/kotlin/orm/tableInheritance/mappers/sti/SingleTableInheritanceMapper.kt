package orm.tableInheritance.mappers.sti

import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import orm.EntityProcessor
import orm.decorators.*
import orm.tableInheritance.ITableInheritanceMapper
import orm.tableInheritance.TableInheritanceFactory
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.*


class SingleTableInheritanceMapper(private val clazz: KClass<*>): ITableInheritanceMapper, ISingleTableInheritance, EntityProcessor() {
    override fun insert(entity: Any): Boolean {
        val sqlInsert = StringBuilder()
        val castedEntity = clazz.cast(entity)
        val castedEntityClass = castedEntity::class
        val mostBaseClass = this.extractMostBaseClass(castedEntityClass)
        val tableName = getTableName(mostBaseClass)

        val columnNamesAndValues = extractAllPropertiesWithInheritance(castedEntityClass).filter { prop ->
            getColumnName(prop) != null
        }.map { prop -> getColumnName(prop) to prop.call(castedEntity) }

        sqlInsert.append("INSERT INTO $tableName (\n")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { it.first!! })
        sqlInsert.append("\n) VALUES (\n")
        sqlInsert.append(columnNamesAndValues.joinToString(", ") { formatValue(it.second) })
        sqlInsert.append("\n);")

        val sqlStatement = sqlInsert.toString()

        println(sqlStatement)

//        transaction {
//            TransactionManager.current().exec(sqlStatement)
//        }

        return true
    }

    override fun findWithoutRelations(id: Long, entityClass: KClass<*>): Any? {
        val sqlSelect = StringBuilder("SELECT ")

        val columnNames = getColumnNamesWithInheritanceSql(clazz)
        sqlSelect.append("$columnNames\n")

        val mostBaseClass = this.extractMostBaseClass(clazz)
        val tableName = getTableName(mostBaseClass)
        sqlSelect.append("FROM $tableName\n")

        if (id != null) {
            val primaryKey = getPrimaryKeyName(mostBaseClass)
            sqlSelect.append("WHERE $primaryKey = $id\n")
        }

        sqlSelect.append(";")
        val sqlStatement = sqlSelect.toString()

        println(sqlStatement)
        return null
        return sqlStatement.execAndMap(::transform)
    }

    override fun find(id: Long?): Any? {
        val sqlSelect = StringBuilder("SELECT ")

        val columnNames = getColumnNamesWithInheritanceSql(clazz)
        sqlSelect.append("$columnNames\n")

        val mostBaseClass =this.extractMostBaseClass(clazz)
        val tableName = getTableName(mostBaseClass)
        sqlSelect.append("FROM $tableName\n")

        if (id != null) {
            val primaryKey = getPrimaryKeyName(mostBaseClass)
            sqlSelect.append("WHERE $primaryKey = $id\n")
        }

        sqlSelect.append(";")
        val sqlStatement = sqlSelect.toString()

        println(sqlStatement)
        return null
        return sqlStatement.execAndMap(::findTransform)
    }

    override fun update(entity: Any): Boolean {
        val sqlUpdate = StringBuilder()

        val castedEntity = clazz.cast(entity)
        val castedEntityClass = castedEntity::class

        val mostBaseClass = this.extractMostBaseClass(castedEntityClass)
        val tableName = getTableName(mostBaseClass)

        val primaryKeyProp = getPrimaryKeyProp(mostBaseClass)
        val primaryKey = getColumnName(primaryKeyProp) to (
                primaryKeyProp.call(castedEntity) ?: throw IllegalArgumentException(
                    "Updated entity must have value in field annotated with @PrimaryKey"
                )
                )

        val columnNamesAndValues = extractAllPropertiesWithInheritance(castedEntityClass).filter { prop ->
            getColumnName(prop) != null
        }.map { prop -> getColumnName(prop) to prop.call(castedEntity) }

        sqlUpdate.append("UPDATE $tableName SET\n")
        sqlUpdate.append(columnNamesAndValues.joinToString(", ") { "${it.first!!} = ${formatValue(it.second)}" })
        sqlUpdate.append("\nWHERE ${primaryKey.first} = ${formatValue(primaryKey.second)};")

        val sqlStatement = sqlUpdate.toString()

        println(sqlStatement)

//        transaction {
//            TransactionManager.current().exec(sqlStatement)
//        }

        return true
    }

    override fun remove(id: Long): Boolean {
        val sqlRemove = StringBuilder()
        val mostBaseClass = this.extractMostBaseClass(clazz)
        val tableName = getTableName(mostBaseClass)
        val primaryKeyColumn = getPrimaryKeyName(mostBaseClass)

        sqlRemove.append("DELETE FROM $tableName WHERE $primaryKeyColumn = $id;")
        val sqlStatement = sqlRemove.toString()

        println(sqlStatement)
//        transaction {
//            TransactionManager.current().exec(sqlStatement)
//        }

        return true
    }

    override fun query(q: String): Any {
        return q.execAndMap(::transform)
    }

    private fun extractAllPropertiesWithInheritance(clazz: KClass<*>): List<KProperty1<out Any, *>> {
        val mostBaseClass = extractMostBaseClass(clazz)
        val notEntityClass = mostBaseClass.superclasses.firstOrNull() ?: return emptyList()

        return clazz.memberProperties.filter { prop -> prop !in notEntityClass.memberProperties }
    }

    private fun getColumnNamesWithInheritanceSql(entityClass: KClass<*>): String {
        return this.getColumnNamesWithInheritance(entityClass).joinToString(", ")
    }

    private fun getColumnNamesWithInheritance(entityClass: KClass<*>): MutableList<String> {
        val columnNames = mutableListOf<String>()

        entityClass.declaredMemberProperties.filter {
            prop -> getColumnName(prop) != null
        }.forEach { prop -> columnNames.add(getColumnName(prop)!!) } // nulls filtered out

        entityClass.superclasses.forEach { superclass ->
            if (superclass.findAnnotation<Entity>() != null) {
                columnNames.addAll(this.getColumnNamesWithInheritance(superclass))
            }
        }

        return columnNames
    }

    private fun transform(rs: ResultSet): Any {
        getTableName(clazz) // Check if entity class

        val values = extractAllPropertiesWithInheritance(clazz).map { prop ->
            val columnName = getColumnName(prop)
            if (isRelationalColumn(prop)) return columnName to null
            return columnName to rs.getObject(columnName)
        }

        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }

    private fun findTransform(rs: ResultSet): Any {
        val props = extractAllPropertiesWithInheritance(clazz)


        val values = props.map { prop ->
            // OneToOne
            val oneToOneAnn = prop.findAnnotation<OneToOne>()
            if (oneToOneAnn != null) {
                val relationOtherSidePK = rs.getObject(getColumnName(prop)) as Long
                val otherSideMapper = TableInheritanceFactory().getMapper(prop.returnType::class) // TODO idk if that will work
                val otherSideObject = otherSideMapper.findWithoutRelations(relationOtherSidePK, clazz)
                return getColumnName(prop) to otherSideObject
            }

            // OneToMany
            val oneToManyAnn = prop.findAnnotation<OneToMany>()
            if (oneToManyAnn != null) {
                val pkValue = rs.getObject(getPrimaryKeyName(clazz)) as Long
                val otherSideClassType = prop.returnType.arguments.firstOrNull()?.type?.withNullability(false) // TODO idk if that will work

                if (otherSideClassType == null) {
                    println("otherSideClassType is null")
                    return getColumnName(prop) to null
                }

                val otherSideMapper = TableInheritanceFactory().getMapper(otherSideClassType::class) // TODO idk if that will work
                val otherSideObject = otherSideMapper.findWithoutRelations(pkValue, clazz)
                return getColumnName(prop) to otherSideObject
            }

            // ManyToOne
            val manyToOneAnn = prop.findAnnotation<ManyToOne>()
            if (manyToOneAnn != null) {
                val relationOtherSidePK = rs.getObject(getColumnName(prop)) as Long
                val otherSideMapper = TableInheritanceFactory().getMapper(prop.returnType::class) // TODO idk if that will work
                val otherSideObject = otherSideMapper.findWithoutRelations(relationOtherSidePK, clazz)
                return getColumnName(prop) to otherSideObject
            }

            // ManyToMany
            val manyToManyAnn = prop.findAnnotation<ManyToMany>()
            if (manyToManyAnn != null) {
                val pkValue = rs.getObject(getPrimaryKeyName(clazz)) as Long
                val otherSideClassType = prop.returnType.classifier as KClass<*> // TODO idk if that will work
                val otherSideClassPrimaryKeyName = getPrimaryKeyName(otherSideClassType)

                if (otherSideClassType == null) {
                    println("otherSideClassType is null")
                    return getColumnName(prop) to null
                }

                val firstTableName = getTableName(clazz)
                val secondTableName = getTableName(otherSideClassType::class)
                val middleTableName = if (firstTableName!! <= secondTableName!!)
                    "${firstTableName}_$secondTableName"
                else
                    "${secondTableName}_$firstTableName"

                val selectColumn = "${secondTableName}_$otherSideClassPrimaryKeyName"
                val middleQuery = "select $selectColumn from $middleTableName where ${getPrimaryKeyName(clazz)} = ${formatValue(pkValue)}"
                val objectsPksList = middleQuery.execAndMap { rs -> rs.getObject(selectColumn) as Long }

                val otherSideMapper = TableInheritanceFactory().getMapper(otherSideClassType::class) // TODO idk if that will work
                val otherSideObjectList = objectsPksList.forEach { pk -> otherSideMapper.findWithoutRelations(pk, clazz) }
                return getColumnName(prop) to otherSideObjectList
            }
        }

        val newEntity = clazz.primaryConstructor?.call(*values.toTypedArray())

        return newEntity ?: throw IllegalStateException("Failed to create an instance of $clazz")
    }

    private fun isRelationalColumn(property: KProperty1<out Any, *>): Boolean {
        return (property.hasAnnotation<OneToOne>()
                || property.hasAnnotation<OneToMany>()
                || property.hasAnnotation<ManyToOne>()
                || property.hasAnnotation<ManyToMany>())
    }

    private fun getRelationalProperties(): List<KProperty1<out Any, *>> {
        return extractAllPropertiesWithInheritance(clazz).filter { prop -> isRelationalColumn(prop) }
    }
}