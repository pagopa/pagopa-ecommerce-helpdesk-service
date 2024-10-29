package it.pagopa.ecommerce.helpdesk.configurations

import com.mongodb.reactivestreams.client.MongoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MongoEcommerceHistoryConfigTest {

    private val mongoEcommerceHistoryConfig = MongoEcommerceHistoryConfig()
    private val uri = "mongodb://localhost:27017/test"
    private val mongoClient: MongoClient =
        mongoEcommerceHistoryConfig.ecommerceHistoryReactiveMongoClient(uri)
    val database = "ecommerce-history"

    @Test
    fun `ecommerceHistoryReactiveMongoClient should create MongoClient with correct settings`() {
        assertThat(mongoClient).isNotNull
    }

    @Test
    fun `ecommerceHistoryReactiveMongoTemplate should create ReactiveMongoTemplate with correct client and database`() {
        val reactiveMongoTemplate =
            mongoEcommerceHistoryConfig.ecommerceHistoryReactiveMongoTemplate(mongoClient, database)

        assertThat(reactiveMongoTemplate).isNotNull
        assertThat(reactiveMongoTemplate.mongoDatabaseFactory.mongoDatabase.block()?.name)
            .isEqualTo(database)
    }
}
