package it.pagopa.ecommerce.helpdesk

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [MongoReactiveAutoConfiguration::class])
class PagopaEcommerceHelpdeskServiceApplication

fun main(args: Array<String>) {
    runApplication<PagopaEcommerceHelpdeskServiceApplication>(*args)
}
