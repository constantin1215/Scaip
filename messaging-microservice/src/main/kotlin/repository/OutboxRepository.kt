package repository

import entity.Group
import entity.MessageEvent
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class OutboxRepository : PanacheMongoRepository<MessageEvent>