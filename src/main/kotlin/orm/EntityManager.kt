package orm;

import kotlin.reflect.KClass;
import kotlin.reflect.full.declaredMemberProperties;
import orm.decorators.*;

class EntityManager {
    private val entities: MutableMap<KClass<*>, MutableList<Any>> = mutableMapOf()

    fun persist(entity: Any) {
        val entityList = entities.computeIfAbsent(entity::class) { mutableListOf() }
        entityList.add(entity)
    }

    fun find(entityClass: KClass<*>, id: Long): Any? {
        return entities[entityClass]?.firstOrNull { entity -> entity::class.declaredMemberProperties.find {
            prop -> prop.annotations.any { it is PrimaryKey } }?.call(entity) == id
        }
    }

    fun update(entity: Any) {
        val entityClass = entity::class
        val entityPrimaryKey = entityClass.annotations.filterIsInstance<PrimaryKey>().firstOrNull()?.let {
            entityClass.declaredMemberProperties.find { it.annotations.any { it is PrimaryKey } }
        } ?: throw IllegalArgumentException("Entity must have a property marked with @Id")

        entities[entityClass]?.let { entityList ->
            val index = entityList.indexOfFirst { entity ->
                entity::class == entityClass && entityClass.declaredMemberProperties.find { prop ->
                prop.annotations.any { it is PrimaryKey } && prop.call(entity) == entityPrimaryKey
            } != null }

            if (index != -1) {
                entityList[index] = entity
            }
        }
    }

    fun delete(entity: Any) {
        val entityClass = entity::class
        val entityPrimaryKey = entityClass.annotations.filterIsInstance<PrimaryKey>().firstOrNull()?.let {
            entityClass.declaredMemberProperties.find { prop -> prop.annotations.any { it is PrimaryKey } }
        } ?: throw IllegalArgumentException("Entity must have a property marked with @Id")

        entities[entityClass]?.let { entityList ->
            val index = entityList.indexOfFirst { entity ->
                entity::class == entityClass && entityClass.declaredMemberProperties.find { prop ->
                    prop.annotations.any { it is PrimaryKey } && prop.call(entity) == entityPrimaryKey
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