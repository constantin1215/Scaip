package repository

import entity.Group
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

        if (result.isEmpty()) {
            return Document(mapOf("_id" to id, "members" to listOf<String>())).toJson()
        }

        return result[0]
    }
}