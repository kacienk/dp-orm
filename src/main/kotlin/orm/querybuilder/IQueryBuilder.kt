package orm.querybuilder
import orm.enums.QueryOrder

interface IQueryBuilder {
    val query: String

    fun createQueryBuilder(tablename: String, parameters: Array<String> = emptyArray()): QueryBuilder
    fun innerJoin(tablename: String, parameterName: String): QueryBuilder
    fun leftJoin(tablename: String, parameterName: String): QueryBuilder
    fun where(condition: String): QueryBuilder
    fun andWhere(condition: String): QueryBuilder
    fun orderBy(parameterName: String, order: QueryOrder): QueryBuilder
    fun retrieveQuery(): String
}
