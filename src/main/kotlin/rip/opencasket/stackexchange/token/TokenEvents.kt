package rip.opencasket.stackexchange.token

data class ActivationTokenCreationEvent(val email: String, val token: TokenDto)

data class PasswordResetTokenCreationEvent(val email: String, val token: TokenDto)