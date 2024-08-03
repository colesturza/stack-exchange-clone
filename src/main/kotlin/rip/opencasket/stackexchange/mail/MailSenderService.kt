package rip.opencasket.stackexchange.mail

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import rip.opencasket.stackexchange.token.TokenDto

@Service
class MailSenderService(
	private val mailSender: JavaMailSender,
	private val templateEngine: SpringTemplateEngine
) {

	private val from = "no-reply@stackexchange.opencasket.rip"

	private val logger = LoggerFactory.getLogger(MailSenderService::class.java)

	fun sendActivationTokenMail(to: String, activationToken: TokenDto) {

		val context = Context().apply {
			setVariable("activationToken", activationToken.plaintext)
		}

		val htmlContent = templateEngine.process("activation-token-email", context)
		val textContent = templateEngine.process("activation-token-email.txt", context)

		val message: MimeMessage = mailSender.createMimeMessage()
		val helper = MimeMessageHelper(message, true)

		helper.setTo(to)
		helper.setFrom(from)
		helper.setSubject("Your Activation Token")
		helper.setText(textContent, htmlContent)

		try {
			mailSender.send(message)
		} catch (ex: Exception) {
			logger.error("Failed to send activation token email", ex)
		}
	}
}