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

    fun fetchCallChannel(groupId: String, callId : String) : String {
        val match : Bson = Document("\$match", Document("_id", groupId))
        val project1 : Bson = Document("\$project", Document("calls", 1))
        val unwind : Bson = Document("\$unwind", "\$calls")
        val match2 : Bson = Document("\$match", Document("calls._id", callId))
        val project2 : Bson = Document("\$project", Document("channel", "\$calls.channel"))

        return mongoDatabase().getCollection("Group").aggregate(listOf(match, project1, unwind, match2, project2)).map { it.toJson() }.toList()[0]
    }
}