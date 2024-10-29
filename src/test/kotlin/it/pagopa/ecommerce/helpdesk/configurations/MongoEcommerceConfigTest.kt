package it.pagopa.ecommerce.helpdesk.configurations

import com.mongodb.reactivestreams.client.MongoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MongoEcommerceConfigTest {

    private val mongoEcommerceConfig = MongoEcommerceConfig()
    private val uri = "mongodb://localhost:27017/test"
    private val mongoClient: MongoClient = mongoEcommerceConfig.ecommerceReactiveMongoClient(uri)
    val database_ecommerce = "ecommerce"
    val database_ecommerce_history = "ecommerce-history"

    @Test
    fun `ecommerceReactiveMongoClient should create MongoClient with correct settings`() {
        assertThat(mongoClient).isNotNull
    }

    @Test
    fun `ecommerceReactiveMongoTemplate should create ReactiveMongoTemplate with correct client and database`() {
        val reactiveMongoTemplate =
            mongoEcommerceConfig.ecommerceReactiveMongoTemplate(mongoClient, database_ecommerce)

        assertThat(reactiveMongoTemplate).isNotNull
        assertThat(reactiveMongoTemplate.mongoDatabaseFactory.mongoDatabase.block()?.name)
            .isEqualTo(database_ecommerce)
    }

    @Test
    fun `ecommerceHistoryReactiveMongoTemplate should create ReactiveMongoTemplate with correct client and database`() {
        val reactiveMongoTemplate =
            mongoEcommerceConfig.ecommerceHistoryReactiveMongoTemplate(
                mongoClient,
                database_ecommerce_history
            )

        assertThat(reactiveMongoTemplate).isNotNull
        assertThat(reactiveMongoTemplate.mongoDatabaseFactory.mongoDatabase.block()?.name)
            .isEqualTo(database_ecommerce_history)
    }
}
