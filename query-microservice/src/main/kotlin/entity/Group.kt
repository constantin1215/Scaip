package entity

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId

@MongoEntity(collection = "Groups", database = "query")
class Group() {
    @BsonId
    lateinit var id : String
    lateinit var owner : GroupUser
    lateinit var title : String
    lateinit var description : String
    val members : MutableSet<GroupUser> = mutableSetOf()

    constructor(
        id : String,
        owner : GroupUser,
        title : String,
        description : String,
        members : Set<GroupUser>
    ) : this() {
        this.id = id
        this.owner = owner
        this.title = title
        this.description = description
        this.members.addAll(members)
    }

    override fun toString(): String {
        return "Group(id='$id', owner=$owner, title='$title', description='$description', members=$members)"
    }
}