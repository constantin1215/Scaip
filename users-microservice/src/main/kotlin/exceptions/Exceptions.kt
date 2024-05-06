package exceptions

class UsernameAlreadyExists : RuntimeException()
class EmailAlreadyExists : RuntimeException()
class IdNotProvided : RuntimeException()
class UserDoesNotExist : RuntimeException()
class ConstraintViolated(msg : String) : RuntimeException(msg)