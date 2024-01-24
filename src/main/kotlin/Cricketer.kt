import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.decorators.PrimaryKey
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.CONCRETE)
open class Cricketer(
    baseId: Int,
    name: String,
    @Column
    val battingAverage: Double
) : Player(baseId, name)