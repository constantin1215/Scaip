package repository

import com.mongodb.client.model.Aggregates
import entity.Group
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped
import org.bson.Document
import org.bson.conversions.Bson

@ApplicationScoped
class GroupRepository : PanacheMongoRepository<Group> {
    fun findById(id : String) = find("_id", id).firstResult()

    fun fetchMessagesFromGroup(groupId: String, timestamp : Long) : String {
        val match : Bson = Document("\$match", Document("_id", groupId))
        val unwind : Bson = Document("\$unwind", "\$messages")
        val match2 : Bson = Document("\$match", Document("messages.timestamp", Document("\$lt", timestamp)))
        val sort : Bson = Document("\$sort", Document("messages.timestamp", -1))
        val group : Bson = Document("\$group", Document(mapOf("_id" to "\$_id", "recentMessages" to Document("\$push", "\$messages"))))
        val project : Bson = Document("\$project", Document(mapOf("_id" to 1, "recentMessages" to Document("\$slice", listOf("\$recentMessages", 20)))))

        return mongoDatabase().getCollection("Group").aggregate(listOf(match, unwind, match2, sort, group, project)).map { it.toJson() }.toList()[0]
    }
}