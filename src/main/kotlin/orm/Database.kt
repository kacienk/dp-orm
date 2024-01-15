package orm
import java.sql.Connection
import java.sql.DriverManager

class Database(private val jdbcUrl: String, private val jdbcUser: String, private val jdbcPassword: String) {

    private var connection: Connection? = null

    fun connect() {
        connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)
    }

    fun disconnect() {
        connection?.close()
    }

    fun getConnection(): Connection {
        return connection ?: throw IllegalStateException("Connection is not open.")
    }

    fun createTable(tableDefinition: String) {
        executeUpdate(tableDefinition)
    }

    fun executeUpdate(query: String) {
        connection?.prepareStatement(query)?.use { statement ->
            statement.executeUpdate()
        }
    }

    fun executeQuery(query: String): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()

        connection?.prepareStatement(query)?.use { statement ->
            val resultSet = statement.executeQuery()
            val metaData = resultSet.metaData
            val columnCount = metaData.columnCount

            while (resultSet.next()) {
                val row = mutableMapOf<String, Any>()
                for (i in 1..columnCount) {
                    row[metaData.getColumnName(i)] = resultSet.getObject(i)
                }
                result.add(row)
            }
        }

        return result
    }
}