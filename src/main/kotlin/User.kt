import orm.decorators.*

@Entity(tableName = "users")
class User(
    @Column
    @PrimaryKey
    val id: Int,
    @Column(name = "custom_name")
    val name: String,
    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = false)
    val guild: Guild
)