import orm.tableInheritance.DatabaseGenerator
import orm.tableInheritance.mappers.noi.NoInheritanceMapper
import orm.tableInheritance.mappers.sti.SingleTableInheritanceMapper
import kotlin.reflect.full.findAnnotation
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import orm.EntityManager

fun main(args: Array<String>) {
    println("Hello World!")

//    val STImapper = SingleTableInheritanceMapper(Student::class)
//    STImapper.find(5)
//    STImapper.insert(Student(id = 1, name = "imie", age = 99, email = "email@gmail.com", enrolled = true, grade = "3", studentId = "111"))
//    STImapper.update(Student(id = 1, name = "imie", age = 99, email = "email@gmail.com", enrolled = true, grade = "3", studentId = "111"))
//    STImapper.remove(9)
//    val NoIMapper = NoInheritanceMapper(User::class)
//    NoIMapper.find(5)
//    NoIMapper.insert(User(id = 1, name = "imie", Guild(id = 1, name = "imie", users = listOf())))
//    NoIMapper.update(User(id = 1, name = "imie", Guild(id = 1, name = "imie", users = listOf())))
//    NoIMapper.remove(9)
    val dbName = "postgres"
    val dbUser = "postgres"
    val dbPassword = "password"
    val dbPort = 5432
    val jdbcUrl = "jdbc:postgresql://localhost:$dbPort/$dbName"
    Database.connect(url = jdbcUrl, driver = "org.postgresql.Driver", user = dbUser, password = dbPassword)

//    val database = DatabaseGenerator(true).generateDatabase(listOf(User::class, Guild::class))
//    val queries = database.split(";")
//    transaction {
//        for (query in queries) {
//            exec(query.trim())
//        }
//     }
    val userEntityManager = EntityManager(User::class)
    val guildEntityManager = EntityManager(Guild::class)
//    guildEntityManager.persist(Guild(id = 1, name = "GILDIA", users = listOf()))
//    userEntityManager.persist(User(id = 2, name = "Bartek", Guild(id = 1, name = "GILDIA", users = listOf())))
    userEntityManager.update(User(id = 2, name = "Czarek", Guild(id = 1, name = "GILDIA", users = listOf())))
    userEntityManager.find(2)



    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}