import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import orm.EntityManager
import orm.querybuilder.QueryBuilder
import orm.tableInheritance.DatabaseGenerator

fun main(args: Array<String>) {
    val dbName = "postgres"
    val dbUser = "postgres"
    val dbPassword = "password"
    val dbPort = 5432
    val jdbcUrl = "jdbc:postgresql://localhost:$dbPort/$dbName"
    Database.connect(url = jdbcUrl, driver = "org.postgresql.Driver", user = dbUser, password = dbPassword)

    /** Generate database from the ground */
//    val database = DatabaseGenerator(true).generateDatabase(listOf(
//        /** No inheritance */
//        User::class,
//        Guild::class,
//        /** Single Table Inheritance */
//        Person::class,
//        Student::class,
//        DimwitStudent::class,
//        Lecturer::class,
//        /** Concrete Table Inheritance */
//        Player::class,
//        Footballer::class,
//        Cricketer::class,
//        Bowler::class
//    ))
//    val queries = database.split(";")
//    transaction {
//        for (query in queries) {
//            exec(query.trim())
//        }
//    }

    /** No inheritance */
//    val guildEM = EntityManager(Guild::class)
//    val userEM = EntityManager(User::class)
//
//    /** Insert */
//    val guild = Guild(id = 1, name = "Guild1", users = mutableListOf())
//    guildEM.persist(guild)
//
//    val user = User(id = 1, name = "User1", guild = guild)
//    userEM.persist(user)
//
//    /** Update */
//    guildEM.update(Guild(id = 1, name = "Guild1Changed", users = mutableListOf(user)))
//
//    /** Select */
//    val foundUser = userEM.find(1) as User
//    println("Found user: ${foundUser.name}")
//    val foundGuild = guildEM.find(1) as Guild
//    println("Found guild: ${foundGuild.name}")
//
//    /** Delete */
//    userEM.delete(1)

    /** Single Table Inheritance */
//    val studentEM = EntityManager(Student::class)
//    val lecturerEM = EntityManager(Lecturer::class)
//
//    /** Insert */
//    val student = Student(id = 1, name = "Student1", age = 1, email = "emailStudent1", studentId = "s1", grade = "A", enrolled = true)
//    studentEM.persist(student)
//
//    val lecturer = Lecturer(id = 2, name = "Lecturer1", age = 50, email = "emailLecturer1", employeeId = "l1", department = "DEP", teachingSubject = "IT")
//    lecturerEM.persist(lecturer)
//
//    /** Update */
//    studentEM.update(Student(id = 1, name = "Student1Changed", age = 2, email = "emailStudent1", studentId = "s1", grade = "A", enrolled = true))
//
//    /** Select */
//    val foundStudent = studentEM.find(1) as Student
//    println("Found student: ${foundStudent.name}")
//    val foundLecturer = lecturerEM.find(2) as Lecturer
//    println("Found lecturer: ${foundLecturer.employeeId}")
//
//    /** Delete */
//    studentEM.delete(1)

    /** Concrete Table Inheritance */
//    val bowlerEM = EntityManager(Bowler::class)
//    val footballerEM = EntityManager(Footballer::class)
//
//    /** Insert */
//    val bowler = Bowler(baseId = 1, name = "Bowler1", battingAverage = 3.1, bowlingAverage = 2.1)
//    bowlerEM.persist(bowler)
//    val footballer = Footballer(baseId = 2, name = "Footballer1", club = "Club1")
//    footballerEM.persist(footballer)
//
//    /** Update */
//    bowlerEM.update(Bowler(baseId = 1, name = "Bowler1", battingAverage = 1.1, bowlingAverage = 1.1))
//
//    /** Select */
//    val foundBowler = bowlerEM.find(1) as Bowler
//    println("Found bowler: ${foundBowler.name}")
//    val foundFootballer = footballerEM.find(2) as Footballer
//    println("Found footballer: ${foundFootballer.name}")
//
//    /** Delete */
//    footballerEM.delete(2)


    /** Query builder */
//    val query = QueryBuilder()
//        .createQueryBuilder(tablename = "users", parameters = arrayOf("id", "custom_name", "guild_id"))
//        .where("id = 1")
//        .retrieveQuery()
//
//    val userEM2 = EntityManager(User::class)
//
//    transaction {
//        val user2 = userEM2.query(query) as User
//        println("user2 name: ${user2.name}")
//    }

}