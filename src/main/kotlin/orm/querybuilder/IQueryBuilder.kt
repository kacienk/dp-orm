package orm.querybuilder
import orm.enums.QueryOrder

interface IQueryBuilder {
    val query: String

    fun createQueryBuilder(tablename: String, parameters: Array<String> = emptyArray()): QueryBuilder
    fun where(condition: String): QueryBuilder
    fun andWhere(condition: String): QueryBuilder
    fun join(joinTable: String, condition: String): QueryBuilder
    fun leftJoin(joinTable: String, condition: String): QueryBuilder
    fun orderBy(parameterName: String, order: QueryOrder): QueryBuilder
    fun retrieveQuery(): String
}
