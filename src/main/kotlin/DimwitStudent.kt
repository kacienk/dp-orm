import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.SINGLE)
class DimwitStudent(
    id: Int,
    name: String,
    age: Int,
    email: String,
    studentId: String,
    grade: String,
    enrolled: Boolean,
    @Column
    val failedCoursesNumber: Int,
    @Column
    val isConflictedWithLecturer: Boolean
): Student(id, name, age, email, studentId, grade, enrolled)