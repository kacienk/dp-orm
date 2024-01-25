package orm.tableInheritance

import orm.EntityProcessor
import orm.decorators.ManyToMany
import orm.decorators.ManyToOne
import orm.decorators.OneToMany
import orm.decorators.OneToOne
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.withNullability

class RelationsHandler(val clazz: KClass<*>, val prop: KProperty1<out Any, *>, val rs: ResultSet): EntityProcessor() {
    // when mapper stumbles upon a relation:
    // invoked by some mapper's find method
    // it resolves relation by calling appropiate new mapper
    // for the entity which is on the other side of the relation
    // and invoking findWithoutRelations on this new entity
    
    fun handleRelation(): Pair<String, Any?>? {
        // OneToOne
        val oneToOne = handleFindOneToOneRelation()
        if (oneToOne != null) return oneToOne

        // OneToMany
        val oneToMany = handleFindOneToManyRelation()
        if (oneToMany != null) return oneToMany

        // ManyToOne
        val manyToOne = handleFindManyToOneRelation()
        if (manyToOne != null) return manyToOne

        // ManyToMany
        val manyToMany = handleFindManyToManyRelation()
        if (manyToMany != null) return manyToMany
        
        // this should never happen since this method shouldn't get invoked without relation annotation
        return null
    }

    private fun handleFindOneToOneRelation(): Pair<String, Any?>? {
        prop.findAnnotation<OneToOne>() ?: return null

        val relationOtherSidePK = rs.getObject(getColumnName(prop)) as Long
        val otherSideMapper = TableInheritanceFactory().getMapper(prop.returnType::class) // TODO idk if that will work
        val otherSideObject = otherSideMapper.findWithoutRelations(relationOtherSidePK, clazz)
        return getColumnName(prop)!! to otherSideObject
    }

    private fun handleFindOneToManyRelation(): Pair<String, Any?>? {
        prop.findAnnotation<OneToMany>() ?: return null

        val pkValue = rs.getObject(getPrimaryKeyName(clazz)) as Long
        val otherSideClassType = prop.returnType.arguments.firstOrNull()?.type?.withNullability(false) // TODO idk if that will work

        if (otherSideClassType == null) {
            println("otherSideClassType is null")
            return getColumnName(prop)!! to null
        }

        val otherSideMapper = TableInheritanceFactory().getMapper(otherSideClassType::class) // TODO idk if that will work
        val otherSideObject = otherSideMapper.findWithoutRelations(pkValue, clazz)
        return getColumnName(prop)!! to otherSideObject
    }

    private fun handleFindManyToOneRelation(): Pair<String, Any?>? {
        prop.findAnnotation<ManyToOne>() ?: return null

        val relationOtherSidePK = rs.getObject(getColumnName(prop)) as Long
        val otherSideMapper = TableInheritanceFactory().getMapper(prop.returnType::class) // TODO idk if that will work
        val otherSideObject = otherSideMapper.findWithoutRelations(relationOtherSidePK, clazz)
        return getColumnName(prop)!! to otherSideObject
    }

    private fun handleFindManyToManyRelation(): Pair<String, Any?>? {
        prop.findAnnotation<ManyToMany>() ?: return null

        val pkValue = rs.getObject(getPrimaryKeyName(clazz)) as Long
        val otherSideClassType = prop.returnType.classifier as KClass<*> // TODO idk if that will work
        val otherSideClassPrimaryKeyName = getPrimaryKeyName(otherSideClassType)

        if (otherSideClassType == null) {
            println("otherSideClassType is null")
            return getColumnName(prop)!! to null
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
        return getColumnName(prop)!! to otherSideObjectList
    }
}