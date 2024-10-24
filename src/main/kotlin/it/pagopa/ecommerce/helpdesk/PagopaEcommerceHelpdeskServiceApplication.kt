package it.pagopa.ecommerce.helpdesk

import it.pagopa.ecommerce.helpdesk.utils.EcommerceMongoProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [MongoAutoConfiguration::class, MongoReactiveAutoConfiguration::class]
)
@EnableConfigurationProperties(EcommerceMongoProperties::class)
class PagopaEcommerceHelpdeskServiceApplication

fun main(args: Array<String>) {
    runApplication<PagopaEcommerceHelpdeskServiceApplication>(*args)
}
