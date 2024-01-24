import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.decorators.PrimaryKey
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.CONCRETE)
class Bowler(
    baseId: Int,
    name: String,
    battingAverage: Double,
    @Column
    val bowlingAverage: Double
) : Cricketer(baseId, name, battingAverage)