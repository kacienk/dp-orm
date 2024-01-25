import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import orm.EntityManager
import orm.tableInheritance.DatabaseGenerator

fun main(args: Array<String>) {
    val dbName = "postgres"
    val dbUser = "postgres"
    val dbPassword = "password"
    val dbPort = 5432
    val jdbcUrl = "jdbc:postgresql://localhost:$dbPort/$dbName"
    Database.connect(url = jdbcUrl, driver = "org.postgresql.Driver", user = dbUser, password = dbPassword)

    /** Generate database from the ground */
    val database = DatabaseGenerator(true).generateDatabase(listOf(
        /** No inheritance */
        User::class,
        Guild::class,
        /** Single Table Inheritance */
        Person::class,
        Student::class,
        DimwitStudent::class,
        Lecturer::class,
        /** Concrete Table Inheritance */
        Player::class,
        Footballer::class,
        Cricketer::class,
        Bowler::class
    ))
    val queries = database.split(";")
    transaction {
        for (query in queries) {
            exec(query.trim())
        }
    }

    /** No inheritance */

    /** Single Table Inheritance */

    /** Concrete Table Inheritance */









//    val dogEM = EntityManager(Dog::class)
//    dogEM.persist(Dog(id = 1, breed = "breed1", color = "color1", owner = null))

//    val em = EntityManager(Lecturer::class)
//    em.persist(Lecturer(id = 1, name = "A B", age = 30, email = "dhd@dkdk.kkd", dog = Dog(id = 1, breed = "breed1", color = "color1", owner = null), employeeId = "nfbdvsc", department = "AGH", teachingSubject = "dkjbsvkjb"))
//    dogEM.update(Dog(id = 1, breed = "breed1", color = "color1", owner = Lecturer(id = 1, name = "A B", age = 30, email = "dhd@dkdk.kkd", dog = null, employeeId = "nfbdvsc", department = "AGH", teachingSubject = "dkjbsvkjb")))

//    println((dogEM.find(1) as Dog).breed)
//    println((em.find(1) as Lecturer).name)

//    val STImapper = SingleTableInheritanceMapper(Student::class)
//    val student = STImapper.find(1) as Student
//    STImapper.insert(Student(id = 3, name = "imie", age = 99, email = "email@gmail.com", enrolled = true, grade = "3", studentId = "111"))
//    STImapper.update(Student(id = 1, name = "zmiana", age = 11, email = "hhh@gmail.com", enrolled = false, grade = "ttt", studentId = "dsfghgj"))
//    STImapper.remove(9)
//    val NoIMapper = NoInheritanceMapper(User::class)
//    NoIMapper.find(5)
//    NoIMapper.insert(User(id = 1, name = "imie", Guild(id = 1, name = "imie", users = listOf())))
//    NoIMapper.update(User(id = 1, name = "imie", Guild(id = 1, name = "imie", users = listOf())))
//    NoIMapper.remove(9)






    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}