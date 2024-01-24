import orm.decorators.Column
import orm.decorators.JoinColumn
import orm.decorators.OneToOne
import orm.decorators.PrimaryKey

class Profile(
    @Column
    @PrimaryKey
    val id: Int,

    @Column
    val gender: String,

    @Column
    val photo: String,

    @OneToOne
    @JoinColumn(name = "user_id", nullable = true)
    val user: User?
)