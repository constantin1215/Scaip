package repository

import entity.GroupEvent
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class OutboxRepository : PanacheRepository<GroupEvent>