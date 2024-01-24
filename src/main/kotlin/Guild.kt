import orm.decorators.Column
import orm.decorators.Entity
import orm.decorators.OneToMany
import orm.decorators.PrimaryKey

@Entity
class Guild (
    @PrimaryKey
    @Column
    val id: Int,
    @Column
    val name: String,
    @OneToMany(mappedBy = "guild_id")
    val users: List<User>?
)