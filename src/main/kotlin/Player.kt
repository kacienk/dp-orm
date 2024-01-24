import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.decorators.PrimaryKey
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.CONCRETE)
abstract class Player(
    @PrimaryKey
    @Column
    val baseId: Int,
    @Column
    val name: String
)