import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.CONCRETE)
class Bowler(
    name: String,
    battingAverage: Double,
    @Column
    val bowlingAverage: Double
) : Cricketer(name, battingAverage)