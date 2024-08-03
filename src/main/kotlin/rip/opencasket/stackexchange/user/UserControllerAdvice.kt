package rip.opencasket.stackexchange.user

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.sql.SQLException

/**
 * Exception handler for the UserController to manage constraint violations
 * and provide appropriate responses.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = [UserController::class])
class UserControllerAdvice {

	private val logger = LoggerFactory.getLogger(UserControllerAdvice::class.java)

	companion object {
		// @formatter:off
		private val INTERNAL_SERVER_ERROR_RESPONSE = ResponseEntity("Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR)
		private val USERNAME_ALREADY_IN_USE_RESPONSE = ResponseEntity("Username already in use.", HttpStatus.CONFLICT)
		private val EMAIL_ALREADY_IN_USE_RESPONSE = ResponseEntity("Email already in use.", HttpStatus.CONFLICT)
		private val USER_NOT_FOUND_RESPONSE = ResponseEntity("User not found.", HttpStatus.NOT_FOUND)
		// @formatter:on
	}

	@ExceptionHandler(UsernameAlreadyExistsException::class)
	@ResponseBody
	fun handleUsernameAlreadyExistsException(ex: UsernameAlreadyExistsException) = USERNAME_ALREADY_IN_USE_RESPONSE

	@ExceptionHandler(EmailAlreadyExistsException::class)
	@ResponseBody
	fun handleEmailAlreadyExistsException(ex: EmailAlreadyExistsException) = EMAIL_ALREADY_IN_USE_RESPONSE

	/**
	 * Handles DataIntegrityViolationException to manage unique constraint violations.
	 *
	 * PostgreSQL unique constraint error codes:
	 * - "23505" indicates a unique constraint violation
	 *
	 * This method handles cases where multiple requests might simultaneously
	 * attempt to create users with the same username or email, resulting in
	 * a unique constraint violation.
	 *
	 * @param ex the DataIntegrityViolationException thrown due to a unique constraint violation
	 * @return a ResponseEntity with an appropriate error message and HTTP status
	 */
	@ExceptionHandler(DataIntegrityViolationException::class)
	@ResponseBody
	fun handleDataIntegrityViolationException(ex: DataIntegrityViolationException): ResponseEntity<String> {
		val cause = ex.cause as? SQLException
		if (cause == null) {
			logger.error("Unexpected error", ex)
			return INTERNAL_SERVER_ERROR_RESPONSE
		}

		val sqlState = cause.sqlState
		val message = cause.message

		if (message == null) {
			logger.error("SQL error with missing message: SQLState = $sqlState", ex)
			return INTERNAL_SERVER_ERROR_RESPONSE
		}

		return when {
			sqlState == "23505" && message.contains("users_username_key") ->
				USERNAME_ALREADY_IN_USE_RESPONSE

			sqlState == "23505" && message.contains("users_email_key") ->
				EMAIL_ALREADY_IN_USE_RESPONSE

			else -> {
				logger.error("Unexpected SQL error: SQLState = $sqlState, Message = $message", ex)
				INTERNAL_SERVER_ERROR_RESPONSE
			}
		}
	}
}