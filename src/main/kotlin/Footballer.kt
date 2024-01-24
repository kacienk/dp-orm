import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.decorators.PrimaryKey
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.CONCRETE)
class Footballer(
    baseId: Int,
    name: String,
    @Column
    val club: String
) : Player(baseId, name)