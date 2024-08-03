package rip.opencasket.stackexchange.token

class InvalidCredentialsException(message: String) : RuntimeException(message)
class TokenNotFoundException(message: String) : RuntimeException(message)
class TokenExpiredException(message: String) : RuntimeException(message)