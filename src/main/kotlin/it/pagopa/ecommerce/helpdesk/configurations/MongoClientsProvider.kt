package it.pagopa.ecommerce.helpdesk.configurations

import com.mongodb.reactivestreams.client.MongoClient
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class MongoClientsProvider {

    @Inject
    lateinit var mongoClient: MongoClient

    @ConfigProperty(name = "quarkus.mongodb.database")
    lateinit var defaultDatabase: String

    @ConfigProperty(name = "mongodb.ecommerce_history.database")
    lateinit var historyDatabase: String

    fun getDefaultClient() = mongoClient

    fun getDefaultDatabaseName() = defaultDatabase

    fun getHistoryDatabaseName() = historyDatabase
}
