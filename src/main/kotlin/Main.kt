import orm.tableInheritance.DatabaseGenerator
import orm.tableInheritance.mappers.noi.NoInheritanceMapper
import orm.tableInheritance.mappers.sti.SingleTableInheritanceMapper
import kotlin.reflect.full.findAnnotation
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

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
    val dbName = "PatternDatabase"
    val dbUser = "postgresCont"
    val dbPassword = "password postgres"
    val dbPort = 5432
    val jdbcUrl = "jdbc:postgresql://localhost:$dbPort/$dbName"
    Database.connect(url = jdbcUrl, driver = "org.postgresql.Driver", user = dbUser, password = dbPassword)

    val databse = DatabaseGenerator(true).generateDatabase(listOf(User::class, Guild::class))



    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}