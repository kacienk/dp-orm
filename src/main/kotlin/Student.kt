import Person
import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.enums.InheritanceType

@Entity()
@Inheritance(strategy = InheritanceType.SINGLE)
open class Student(
    id: Int,
    name: String,
    age: Int,
    email: String,
    @Column(nullable = true)
    val studentId: String,
    @Column(nullable = true)
    val grade: String,
    @Column(nullable = true)
    val enrolled: Boolean
) : Person(id, name, age, email)