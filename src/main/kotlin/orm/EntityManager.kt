package orm;

import kotlin.reflect.KClass;
import kotlin.reflect.full.declaredMemberProperties;
import orm.decorators.*;
import orm.tableInheritance.TableInheritanceFactory

class EntityManager(private val entityClass: KClass<*>) {
    private val mapper = TableInheritanceFactory().getMapper(entityClass);

    fun persist(entity: Any): Boolean {
        return mapper.insert(entity);
    }

    fun find(id: Int): Any? {
        return mapper.find(id);
    }

    fun update(entity: Any): Boolean {
        return mapper.update(entity);
    }

    fun delete(id: Int): Boolean {
        return mapper.remove(id);
    }
    fun query(q: String): Any? {
        return mapper.query(q);
    }
}