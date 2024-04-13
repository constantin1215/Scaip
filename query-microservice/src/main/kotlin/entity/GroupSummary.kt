package entity

import entity.UserSummary
import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.common.ProjectionFor
import org.bson.codecs.pojo.annotations.BsonId

@ProjectionFor(Group::class)
class GroupSummary() {
    @BsonId
    lateinit var id : String
    lateinit var owner : UserSummary
    lateinit var title : String
    lateinit var description : String
    var lastMessage : Message? = null

    constructor(
        id : String,
        owner : UserSummary,
        title : String,
        description : String,
        lastMessage: Message
    ) : this() {
        this.id = id
        this.owner = owner
        this.title = title
        this.description = description
        this.lastMessage = lastMessage
    }

    constructor(
        group: Group
    ) : this() {
        this.id = group.id
        this.owner = group.owner
        this.title = group.title
        this.description = group.description
        this.lastMessage = group.lastMessage
    }

    override fun toString(): String {
        return "GroupSummary(id='$id', owner=$owner, title='$title', description='$description')"
    }
}