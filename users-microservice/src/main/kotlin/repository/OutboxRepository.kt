package repository

import entity.UserEvent
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class OutboxRepository : PanacheRepository<UserEvent>