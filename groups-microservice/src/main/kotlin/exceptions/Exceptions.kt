package exceptions

class EntityAlreadyInCollection(val entityId : String) : RuntimeException()
class NecessaryDataMissing : RuntimeException()
class UserNotFound(val userId : String, val event : String) : RuntimeException()
