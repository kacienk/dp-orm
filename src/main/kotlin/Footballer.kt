import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.Inheritance
import orm.enums.InheritanceType

@Entity
@Inheritance(strategy = InheritanceType.CONCRETE)
class Footballer(
    name: String,
    @Column
    val club: String
) : Player(name)