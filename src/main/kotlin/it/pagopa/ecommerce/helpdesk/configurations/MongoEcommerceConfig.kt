package it.pagopa.ecommerce.helpdesk.configurations

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import it.pagopa.ecommerce.helpdesk.utils.EcommerceMongoProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@Configuration
class MongoEcommerceConfig(private val ecommerceMongoProperties: EcommerceMongoProperties) {
    @Bean(name = ["ecommerceReactiveMongoClient"])
    fun ecommerceReactiveMongoClient(): MongoClient {
        val connectionString = ConnectionString(ecommerceMongoProperties.uri)
        val settings = MongoClientSettings.builder().applyConnectionString(connectionString).build()
        return MongoClients.create(settings)
    }

    @Bean(name = ["ecommerceReactiveMongoTemplate"])
    fun ecommerceReactiveMongoTemplate(
        @Qualifier("ecommerceReactiveMongoClient") mongoClient: MongoClient
    ): ReactiveMongoTemplate {
        return ReactiveMongoTemplate(mongoClient, ecommerceMongoProperties.database)
    }
}
