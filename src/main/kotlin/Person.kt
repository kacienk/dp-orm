import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.decorators.PrimaryKey
import orm.enums.InheritanceType

@Entity(tableName = "persons")
@Inheritance(strategy = InheritanceType.SINGLE)
open class Person(
    @Column
    @PrimaryKey
    val id: Int,
    @Column
    val name: String,
    @Column
    val age: Int,
    @Column
    val email: String
)