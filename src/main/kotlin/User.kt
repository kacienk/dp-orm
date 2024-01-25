import orm.decorators.*

@Entity(tableName = "users")
class User(
    @Column
    @PrimaryKey
    val id: Int,
    @Column(name = "custom_name")
    val name: String,
    @ManyToOne
    @JoinColumn(name = "guild_id", nullable = true)
    val guild: Guild?,

//    @OneToOne
//    @JoinColumn(name = "profile_id", nullable = true)
//    val profile: Profile?,
)