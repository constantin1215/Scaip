package entity

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId

@MongoEntity(collection = "Groups", database = "query")
class Group() {
    @BsonId
    lateinit var id : String
    lateinit var owner : UserSummary
    lateinit var title : String
    lateinit var description : String
    lateinit var members : MutableSet<UserSummary>
    var lastMessage : Message? = null

    constructor(
        id : String,
        owner : UserSummary,
        title : String,
        description : String,
        members : MutableSet<UserSummary>
    ) : this() {
        this.id = id
        this.owner = owner
        this.title = title
        this.description = description
        this.members = members
    }

    override fun toString(): String {
        return "Group(id='$id', owner=$owner, title='$title', description='$description', members=$members)"
    }
}