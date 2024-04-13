package exceptions

class MissingFieldException(msg : String) : RuntimeException(msg)
class UnauthorizedAction(msg : String) : RuntimeException(msg)
class SessionExpired : RuntimeException()