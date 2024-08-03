package rip.opencasket.stackexchange.token

import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import rip.opencasket.stackexchange.mail.MailSenderService
import rip.opencasket.stackexchange.user.UserRegistrationEvent

@Component
class TokenEventListener(
	private val tokenService: TokenService,
	private val mailSenderService: MailSenderService
) {
	@TransactionalEventListener(UserRegistrationEvent::class)
	@Async
	fun onUserRegistrationEvent(event: UserRegistrationEvent) {
		val email = event.user.email
		tokenService.createNewActivationToken(email)
	}

	@EventListener(ActivationTokenCreationEvent::class)
	@Async
	fun onActivationTokenCreationEvent(event: ActivationTokenCreationEvent) {
		mailSenderService.sendActivationTokenMail(event.email, event.token)
	}
}