package orm;

import kotlin.reflect.KClass;
import kotlin.reflect.full.declaredMemberProperties;

class EntityManager {
    private val entities: MutableMap<KClass<*>, MutableList<Any>> = mutableMapOf()

    fun persist(entity: Any) {
        val entityList = entities.computeIfAbsent(entity::class) { mutableListOf() }
        entityList.add(entity)
    }

    fun find(entityClass: KClass<*>, id: Long): Any? {
        return entities[entityClass]?.firstOrNull { it::class.declaredMemberProperties.find { it.annotations.any { it is Id } }?.get(it) == id }
    }

    fun update(entity: Any) {
        val entityClass = entity::class
        val tableName = getTableName(entityClass)

        val entityId = entityClass.annotations.filterIsInstance<Id>().firstOrNull()?.let {
            entityClass.declaredMemberProperties.find { it.annotations.any { it is Id } }?.get(entity)
        } ?: throw IllegalArgumentException("Entity must have a property marked with @Id")

        entities[tableName]?.let { entityList ->
            val index = entityList.indexOfFirst { it::class == entityClass && entityClass.declaredMemberProperties.find { prop ->
                prop.annotations.any { it is Id } && prop.get(it) == entityId
            } != null }

            if (index != -1) {
                entityList[index] = entity
            }
        }
    }

    fun delete(entity: Any) {
        val entityClass = entity::class
        val tableName = getTableName(entityClass)

        val entityId = entityClass.annotations.filterIsInstance<Id>().firstOrNull()?.let {
            entityClass.declaredMemberProperties.find { it.annotations.any { it is Id } }?.get(entity)
        } ?: throw IllegalArgumentException("Entity must have a property marked with @Id")

        entities[tableName]?.let { entityList ->
            val index = entityList.indexOfFirst { it::class == entityClass && entityClass.declaredMemberProperties.find { prop ->
                prop.annotations.any { it is Id } && prop.get(it) == entityId
            } != null }

            if (index != -1) {
                entityList.removeAt(index)
            }
        }
    }

    private fun getTableName(entityClass: KClass<*>): String {
        return entityClass.annotations.filterIsInstance<Entity>().firstOrNull()?.tableName
            ?: entityClass.simpleName ?: throw IllegalArgumentException("Entity class must have a simple name.")
    }
}