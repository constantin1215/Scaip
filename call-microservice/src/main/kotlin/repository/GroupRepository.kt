package repository

import com.mongodb.client.model.Aggregates
import entity.Group
import exceptions.GroupNotFound
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped
import org.bson.Document
import org.bson.conversions.Bson
import javax.print.Doc

@ApplicationScoped
class GroupRepository : PanacheMongoRepository<Group> {
    fun findById(id : String) = find("_id", id).firstResult()

    fun fetchCallChannel(groupId: String, callId : String) : String {
        val match : Bson = Document("\$match", Document("_id", groupId))
        val project1 : Bson = Document("\$project", Document("calls", 1))
        val unwind : Bson = Document("\$unwind", "\$calls")
        val match2 : Bson = Document("\$match", Document(mapOf("calls._id" to callId, "calls.status" to Document("\$ne", "FINISHED"))))
        val project2 : Bson = Document("\$project", Document(mapOf(
            "channel" to "\$calls.channel",
            "callId" to "\$calls._id",
            "status" to "\$calls.status",
            "type" to "\$calls.type",
            "scheduledTime" to "\$calls.scheduledTime"
        )))

        val list = mongoDatabase().getCollection("Group").aggregate(listOf(match, project1, unwind, match2, project2)).map { it.toJson() }.toList()

        if (list.isEmpty()) {
            return Document(mapOf("_id" to groupId, "channel" to "")).toJson()
        }
        return list[0]
    }

    fun addToJoinedVideo(callId: String, userId: String) {
        val match : Bson = Document("calls._id", callId)
        val update : Bson = Document("\$addToSet", Document("calls.\$.joinedVideo", userId))

        mongoDatabase().getCollection("Group").updateOne(match, update)
    }

    fun removeFromJoinedVideo(callId: String, userId: String) {
        val match : Bson = Document("calls._id", callId)
        val update : Bson = Document("\$pull", Document("calls.\$.joinedVideo", userId))

        mongoDatabase().getCollection("Group").updateOne(match, update)
    }

    fun addToJoinedAudio(callId: String, userId: String) {
        val match : Bson = Document("calls._id", callId)
        val update : Bson = Document("\$addToSet", Document("calls.\$.joinedAudio", userId))

        mongoDatabase().getCollection("Group").updateOne(match, update)
    }

    fun removeFromJoinedAudio(callId: String, userId: String) {
        val match : Bson = Document("calls._id", callId)
        val update : Bson = Document("\$pull", Document("calls.\$.joinedAudio", userId))

        mongoDatabase().getCollection("Group").updateOne(match, update)
    }

    fun isCallEmpty(callId: String) : Boolean {
        val match : Bson = Document("calls",
            Document("\$elemMatch",
                Document(mapOf("_id" to callId,
                    "joinedVideo" to Document("\$eq", arrayListOf<String>()),
                    "joinedAudio" to Document("\$eq", arrayListOf<String>())
                ))))

        val result = mongoDatabase().getCollection("Group").find(match).map { it.toJson() }.toList()

        return result.isEmpty()
    }

    fun finishCall(callId: String) {
        val match : Bson = Document("calls._id", callId)
        val update : Bson = Document("\$set", Document("calls.\$.status", "FINISHED"))

        mongoDatabase().getCollection("Group").updateOne(match, update)
    }

    fun getGroupIdByCallId(callId: String) : String {
        val match : Bson = Document("\$match", Document("calls._id", callId))
        val project : Bson = Document("\$project", Document("_id", 1))

        val list = mongoDatabase().getCollection("Group").aggregate(listOf(match, project)).map { it.toJson() }.toList()

        if (list.isEmpty())
            throw GroupNotFound("Unknown", "JOIN/LEAVE AUDIO/VIDEO")

        return list[0]
    }
}