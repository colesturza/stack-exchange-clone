package rip.opencasket.stackexchange

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import rip.opencasket.stackexchange.token.InvalidCredentialsException
import rip.opencasket.stackexchange.token.TokenExpiredException
import rip.opencasket.stackexchange.token.TokenNotFoundException
import rip.opencasket.stackexchange.user.UserAlreadyActiveException
import java.time.LocalDateTime

@ControllerAdvice
class GlobalControllerExceptionHandler {

	private val logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler::class.java)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidationExceptions(
		ex: MethodArgumentNotValidException,
		request: WebRequest
	): ResponseEntity<ErrorResponse> {
		logger.error("Validation error: {}", ex.message, ex)
//		val errors = ex.bindingResult.allErrors.map { it.defaultMessage }.joinToString(", ")
		val errorResponse = ErrorResponse(
			timestamp = LocalDateTime.now(),
			status = HttpStatus.BAD_REQUEST.value(),
			error = HttpStatus.BAD_REQUEST.reasonPhrase,
			message = "Validation failed for one or more fields.",
			path = request.getDescription(false)
		)
		return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
	}

	@ExceptionHandler(BindException::class)
	fun handleBindException(
		ex: BindException,
		request: WebRequest
	): ResponseEntity<ErrorResponse> {
		logger.error("Binding error: {}", ex.message, ex)
		val errorResponse = ErrorResponse(
			timestamp = LocalDateTime.now(),
			status = HttpStatus.BAD_REQUEST.value(),
			error = HttpStatus.BAD_REQUEST.reasonPhrase,
			message = "Request binding failed.",
			path = request.getDescription(false)
		)
		return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
	}

	@ExceptionHandler(UsernameNotFoundException::class)
	fun handleUsernameNotFound(ex: UsernameNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
		logger.error("Username not found: {}", ex.message, ex)
		val errorResponse = ErrorResponse(
			timestamp = LocalDateTime.now(),
			status = HttpStatus.NOT_FOUND.value(),
			error = HttpStatus.NOT_FOUND.reasonPhrase,
			message = "User not found.",
			path = request.getDescription(false)
		)
		return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
	}

	@ExceptionHandler(InvalidCredentialsException::class)
	fun handleInvalidCredentials(ex: InvalidCredentialsException, request: WebRequest): ResponseEntity<ErrorResponse> {
		logger.error("Invalid credentials: {}", ex.message, ex)
		val errorResponse = ErrorResponse(
			timestamp = LocalDateTime.now(),
			status = HttpStatus.UNAUTHORIZED.value(),
			error = HttpStatus.UNAUTHORIZED.reasonPhrase,
			message = "Invalid username or password.",
			path = request.getDescription(false)
		)
		return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
	}

	@ExceptionHandler(TokenNotFoundException::class)
	fun handleTokenNotFound(ex: TokenNotFoundException, request: WebRequest): ResponseEntity<ErrorResponse> {
		logger.error("Token not found: {}", ex.message, ex)
		val errorResponse = ErrorResponse(
			timestamp = LocalDateTime.now(),
			status = HttpStatus.NOT_FOUND.value(),
			error = HttpStatus.NOT_FOUND.reasonPhrase,
			message = "Token not found.",
			path = request.getDescription(false)
		)
		return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
	}

	@ExceptionHandler(TokenExpiredException::class)
	fun handleTokenExpired(ex: TokenExpiredException, request: WebRequest): ResponseEntity<ErrorResponse> {
		logger.error("Token expired: {}", ex.message, ex)
		val errorResponse = ErrorResponse(
			timestamp = LocalDateTime.now(),
			status = HttpStatus.UNAUTHORIZED.value(),
			error = HttpStatus.UNAUTHORIZED.reasonPhrase,
			message = "The token has expired.",
			path = request.getDescription(false)
		)
		return ResponseEntity(errorResponse, HttpStatus.UNAUTHORIZED)
	}

	@ExceptionHandler(UserAlreadyActiveException::class)
	fun handleUserAlreadyActiveException(ex: UserAlreadyActiveException): ResponseEntity<String> {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.message)
	}

	@ExceptionHandler(Exception::class)
	fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
		logger.error("Unhandled exception: {}", ex.message, ex)
		val errorResponse = ErrorResponse(
			timestamp = LocalDateTime.now(),
			status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
			error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
			message = "An unexpected error occurred. Please try again later.",
			path = request.getDescription(false)
		)
		return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
	}
}