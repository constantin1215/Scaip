package entity

import jakarta.persistence.*
import java.time.Instant


@Entity
@Table(name = "\"Outbox\"")
class UserEvent() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id : String
    val timestamp : Long = Instant.now().toEpochMilli()
    @Column(nullable = false)
    lateinit var originalEvent : String
    @Column(nullable = false)
    lateinit var generatedEvent : String
    @Column(nullable = false, length = 5096)
    lateinit var data : String
    lateinit var details : String
    lateinit var sessionId : String

    constructor(originalEvent : String, generatedEvent : String, data : String, sessionId : String) : this() {
        this.originalEvent = originalEvent
        this.generatedEvent = generatedEvent
        this.data = data
        this.sessionId = sessionId
    }

    constructor(originalEvent : String, generatedEvent : String, data : String, details : String, sessionId : String) : this() {
        this.originalEvent = originalEvent
        this.generatedEvent = generatedEvent
        this.data = data
        this.details = details
        this.sessionId = sessionId
    }
}