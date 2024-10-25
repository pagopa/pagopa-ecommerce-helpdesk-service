package it.pagopa.ecommerce.helpdesk

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [MongoReactiveDataAutoConfiguration::class])
class PagopaEcommerceHelpdeskServiceApplication

fun main(args: Array<String>) {
    runApplication<PagopaEcommerceHelpdeskServiceApplication>(*args)
}
