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
    val dbName = "PatternDatabase"
    val dbUser = "postgresCont"
    val dbPassword = "password postgres"
    val dbPort = 5432
    val jdbcUrl = "jdbc:postgresql://localhost:$dbPort/$dbName"
    Database.connect(url = jdbcUrl, driver = "org.postgresql.Driver", user = dbUser, password = dbPassword)

    val database = DatabaseGenerator(true).generateDatabase(listOf(User::class, Guild::class))
    val queries = database.split(";")
    transaction { 
        for (query in queries) {
            exec(query)
        }
     }
    val userEntityManager =  EntityManager(User::class)
    val guildEntityManager = EntityManager(Guild::class)
    userEntityManager.persist(User(id = 1, name = "Antek", guild = Guild(id = 1, name = "GildiaAntos", users = listOf())))
    userEntityManager.persist(User(id = 2, name = "Bartek", guild = Guild(id = 1, name = "GildiaAntos", users = listOf())))
    userEntityManager.persist(User(id = 3, name = "Czarek", guild = Guild(id = 1, name = "GildiaAntos", users = listOf())))
    userEntityManager.persist(User(id = 4, name = "Dawid", guild = Guild(id = 1, name = "GildiaAntos", users = listOf())))
    userEntityManager.persist(User(id = 5, name = "Edward", guild = Guild(id = 1, name = "GildiaAntos", users = listOf())))
    guildEntityManager.persist(Guild(id = 2, name = "GildiaBartus", users = listOf()))
    guildEntityManager.persist(Guild(id = 3, name = "GildiaCzarka", users = listOf()))
    guildEntityManager.persist(Guild(id = 4, name = "GildiaDawida", users = listOf()))
    val studentEntityManager = EntityManager(Student::class)
    studentEntityManager.persist(Student(id = 1, name = "Andrzej", age = 19, email = "email@.com", enrolled = true, grade = "3", studentId = "111"))
    studentEntityManager.persist(Student(id = 2, name = "Barbara", age = 20, email = "email@.com", enrolled = true, grade = "3", studentId = "112"))
    studentEntityManager.persist(Student(id = 3, name = "Cezary", age = 21, email = "email@.com", enrolled = true, grade = "3", studentId = "113"))
    studentEntityManager.persist(Student(id = 4, name = "Dawid", age = 22, email = "email@.com", enrolled = true, grade = "3", studentId = "114"))

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}