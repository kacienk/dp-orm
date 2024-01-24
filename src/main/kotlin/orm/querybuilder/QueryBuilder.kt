package orm.querybuilder

import orm.enums.QueryOrder

class QueryBuilder : IQueryBuilder  {
    override var query: String = ""

        override fun createQueryBuilder(tablename: String, parameters: Array<String>): QueryBuilder {
            if (parameters.isNotEmpty()){
                parameters.forEachIndexed { index, param ->
                    parameters[index] = "$tablename.$param"
                }
                query = "SELECT ${parameters.joinToString(", ")} FROM $tablename"
            }else{
                query = "SELECT * FROM $tablename"
            }
            return this
        }

    override fun where(condition: String): QueryBuilder{
        var whereParams = condition.split("(?<=[<>!=])|(?=[<>!=])".toRegex())
        query += " WHERE ${prepareCondition(condition)}"
        return this
    }
    override fun andWhere(condition: String): QueryBuilder {
        query += " AND ${prepareCondition(condition)}"
        return this
    }   
    override fun join(joinTable: String, condition: String): QueryBuilder {
        var whereParams = condition.split("(?<=[<>!=])|(?=[<>!=])".toRegex())
        query += " INNER JOIN $joinTable ON ${whereParams.get(0)}${whereParams.get(1)}${whereParams.get(2)}"
        return this
    }
    override fun leftJoin(joinTable: String, condition: String): QueryBuilder {
        var whereParams = condition.split("(?<=[<>!=])|(?=[<>!=])".toRegex())
        query += " LEFT JOIN $joinTable ON ${whereParams.get(0)} ${whereParams.get(1)} ${whereParams.get(2)}"
        return this
    }
    override fun orderBy(parameterName: String, order: QueryOrder): QueryBuilder {
        if(order == QueryOrder.ASCENING) {
            query += " ORDER BY $parameterName ASC"
        } else if(order == QueryOrder.DESCENDING) {
            query += " ORDER BY $parameterName DESC"
        }else{
            throw Exception("Invalid QueryOrder")
        }
        return this
    }

    override fun retrieveQuery():String {
        query += ";"
        return query
    }
    private fun prepareCondition(condition: String): String {
        var whereParams = condition.split("(?<=[<>!=])|(?=[<>!=])".toRegex())
        try {
            whereParams.get(2).toInt()
            return "${whereParams.get(0)} ${whereParams.get(1)} ${whereParams.get(2)}"

        } catch (e: NumberFormatException) {
            return "${whereParams.get(0)} ${whereParams.get(1)} '${whereParams.get(2)}'"
        }
    }
}