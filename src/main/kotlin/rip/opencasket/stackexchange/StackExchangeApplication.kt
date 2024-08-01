package rip.opencasket.stackexchange

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StackExchangeApplication

fun main(args: Array<String>) {
	runApplication<StackExchangeApplication>(*args)
}
