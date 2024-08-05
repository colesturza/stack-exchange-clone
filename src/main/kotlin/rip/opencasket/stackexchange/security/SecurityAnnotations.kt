package rip.opencasket.stackexchange.security

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.annotation.CurrentSecurityContext

/**
 * Annotation to inject the currently authenticated [UserDetailsImpl] object into a method parameter.
 *
 * This annotation is used to access the currently authenticated user details within a method parameter.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@AuthenticationPrincipal
annotation class CurrentUser

/**
 * Annotation to inject the user ID of the currently authenticated user into a method parameter.
 *
 * This annotation extracts the `id` property from the [UserDetailsImpl] object associated with the
 * current authentication context. The `id` should be a property of your custom [UserDetailsImpl] implementation.
 *
 * **Usage Note:** When using this annotation, ensure that the method parameter is of a nullable type (`Long?`).
 * This is necessary because if you use a non-nullable type (`Long`), Kotlin will use a primitive representation,
 * which will result in a `NullPointerException` when Spring attempts to load the value.
 *
 * Example:
 * ```kotlin
 * @GetMapping("/user")
 * fun getUser(@CurrentUserId userId: Long?): ResponseEntity<String> {
 *     // Handle the user ID
 *     return ResponseEntity.ok("User ID is $userId")
 * }
 * ```
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@CurrentSecurityContext(expression = "authentication.principal.id")
annotation class CurrentUserId

/**
 * Annotation to secure a method by checking if the currently authenticated user's email is verified.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("authentication.principal.isEmailVerified")
annotation class CurrentUserHasVerifiedEmail