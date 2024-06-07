package repository

import entity.Group
import entity.Message
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped
import org.bson.Document
import org.bson.conversions.Bson

@ApplicationScoped
class GroupRepository : PanacheMongoRepository<Group> {
    fun findById(id : String) = find("_id", id).firstResult()

    fun fetchMembers(id : String) : String {
        val match : Bson = Document("\$match", Document("_id", id))
        val project : Bson = Document("\$project", Document("members", 1))

        val result =  mongoDatabase().getCollection("Groups").aggregate(listOf(match, project)).map { it.toJson() }.toList()

        if (result.isEmpty())
            return Document(mapOf("_id" to id, "members" to listOf<String>())).toJson()

        return result[0]
    }

    fun updateGroupLastMessage(groupId : String, message: Message) {
        val filter : Bson = Document("_id", groupId)
        val update : Bson = Document("\$set", Document("lastMessage", message))

        mongoCollection().updateOne(filter, update)
    }

    fun fetchGroupDetails(id : String) : String {
        val match : Bson = Document("\$match", Document("_id", id))
        val project : Bson = Document("\$project", Document(mapOf("_id" to 1, "title" to 1, "owner" to 1, "description" to 1, "lastMessage" to 1)))
        val result =  mongoDatabase().getCollection("Groups").aggregate(listOf(match, project)).map { it.toJson() }.toList()

        if (result.isEmpty())
            return Document(mapOf("message" to "Group not found!")).toJson()

        return result[0]
    }

    fun fetchGroupCalls(id : String) : String {
        val match : Bson = Document("\$match", Document("_id", id))
        Document("\$filter", Document(mapOf("input" to "\$calls", "as" to "call", "cond" to Document("\$ne", arrayOf("\$\$call.status", "FINISHED")))))
        val project : Bson = Document("\$project", Document(mapOf("_id" to 1, "calls" to Document("\$filter", Document(mapOf("input" to "\$calls", "as" to "call", "cond" to Document("\$ne", arrayListOf("\$\$call.status", "FINISHED"))))))))
        val result =  mongoDatabase().getCollection("Groups").aggregate(listOf(match, project)).map { it.toJson() }.toList()

        if (result.isEmpty())
            return Document(mapOf("message" to "Group not found!")).toJson()

        return result[0]
    }
}