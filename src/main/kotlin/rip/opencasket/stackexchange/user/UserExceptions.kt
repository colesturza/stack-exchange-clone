package rip.opencasket.stackexchange.user

class UsernameAlreadyExistsException(message: String) : RuntimeException(message)
class EmailAlreadyExistsException(message: String) : RuntimeException(message)
class UserNotFoundException(message: String) : RuntimeException(message)