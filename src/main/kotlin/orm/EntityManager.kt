package orm;

import kotlin.reflect.KClass;
import kotlin.reflect.full.declaredMemberProperties;
import orm.decorators.*;
import orm.tableInheritance.TableInheritanceFactory

class EntityManager(private val entityClass: KClass<*>) {
    private val mapper = TableInheritanceFactory().getMapper(entityClass);

    fun persist(entity: Any) {
        mapper.insert(entity);
    }

    fun find(id: Long): Any? {
        return mapper.find(id);
    }

    fun update(entity: Any) {
        mapper.update(entity);
    }

    fun delete(id: Long) {
        mapper.remove(id);
    }
    fun query(q: String) {
        mapper.query(q);
    }

    private fun getTableName(entityClass: KClass<*>): String {
        return entityClass.annotations.filterIsInstance<Entity>().firstOrNull()?.tableName
            ?: entityClass.simpleName ?: throw IllegalArgumentException("Entity class must have a simple name.")
    }
}