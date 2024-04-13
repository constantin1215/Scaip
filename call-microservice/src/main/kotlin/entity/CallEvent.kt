package entity

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId
import java.time.Instant
import java.util.*

@MongoEntity(collection = "Outbox", database = "calls")
class CallEvent() {
    @BsonId
    val id : String = UUID.randomUUID().toString()
    val timestamp : Long = Instant.now().toEpochMilli()
    lateinit var originalEvent : String
    lateinit var generatedEvent : String
    lateinit var data : Call
    var details : String? = null
    lateinit var sessionId : String

    constructor(originalEvent : String, generatedEvent : String, data : Call, sessionId : String) : this() {
        this.originalEvent = originalEvent
        this.generatedEvent = generatedEvent
        this.data = data
        this.sessionId = sessionId
    }

    constructor(originalEvent : String, generatedEvent : String, data : Call, details : String, sessionId : String) : this() {
        this.originalEvent = originalEvent
        this.generatedEvent = generatedEvent
        this.data = data
        this.details = details
        this.sessionId = sessionId
    }
}