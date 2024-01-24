import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.CONCRETE)
open class Cricketer(
    name: String,
    @Column
    val battingAverage: Double
) : Player(name)