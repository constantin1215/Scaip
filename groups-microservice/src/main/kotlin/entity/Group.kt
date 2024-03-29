package entity

import jakarta.persistence.*

@Entity
@Table(name = "\"Group\"")
class Group() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id : String

    @ManyToOne
    @JoinColumn(name = "owner", referencedColumnName = "id", nullable = false)
    lateinit var owner : User

    @Column(nullable = false, unique = false, length = 30)
    lateinit var title : String

    @Column(nullable = false, unique = false, length = 300)
    lateinit var description : String

    @Column(nullable = true, unique = false)
    var photoId : String? = null

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_user",
        joinColumns = [JoinColumn(name = "group_id", referencedColumnName = "id")],
        inverseJoinColumns = [JoinColumn(name = "member_id", referencedColumnName = "id")]
    )
    val members : MutableSet<User> = mutableSetOf()

    constructor(owner: User, title: String, description: String, initialMembers : Set<User>) : this() {
        this.owner = owner
        this.title = title
        this.description = description

        for (member in initialMembers)
            this.members.add(member)
    }

    override fun toString(): String {
        return "Group(id='$id', owner=$owner, title='$title', description='$description', photoId='$photoId', members=$members)"
    }


}