package repository

import entity.CallEvent
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class OutboxRepository : PanacheMongoRepository<CallEvent>