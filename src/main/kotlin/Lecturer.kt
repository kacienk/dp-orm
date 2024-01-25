import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.SINGLE)
class Lecturer(
    id: Int,
    name: String,
    age: Int,
    email: String,
    @Column(nullable = true)
    val employeeId: String,
    @Column(nullable = true)
    val department: String,
    @Column(nullable = true)
    val teachingSubject: String
) : Person(id, name, age, email)