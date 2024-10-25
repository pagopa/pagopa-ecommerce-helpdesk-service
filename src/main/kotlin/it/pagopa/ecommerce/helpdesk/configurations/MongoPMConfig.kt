package it.pagopa.ecommerce.helpdesk.configurations

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@Configuration
class MongoPMConfig {
    @Bean(name = ["pmReactiveMongoClient"])
    fun pmReactiveMongoClient(
        @Value("\${pm.mongodb.uri}") uri: String,
    ): MongoClient {
        val connectionString = ConnectionString(uri)
        val settings = MongoClientSettings.builder().applyConnectionString(connectionString).build()
        return MongoClients.create(settings)
    }

    @Bean(name = ["pmReactiveMongoTemplate"])
    fun pmReactiveMongoTemplate(
        @Qualifier("pmReactiveMongoClient") mongoClient: MongoClient,
        @Value("\${pm.mongodb.database}") database: String
    ): ReactiveMongoTemplate {
        return ReactiveMongoTemplate(mongoClient, database)
    }
}
