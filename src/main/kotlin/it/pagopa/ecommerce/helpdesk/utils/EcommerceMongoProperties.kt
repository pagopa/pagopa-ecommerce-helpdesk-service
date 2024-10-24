package it.pagopa.ecommerce.helpdesk.utils

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "ecommerce.mongodb")
class EcommerceMongoProperties {
    lateinit var uri: String
    lateinit var database: String
}
