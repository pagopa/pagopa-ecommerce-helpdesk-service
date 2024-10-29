package it.pagopa.ecommerce.helpdesk.configurations

import com.mongodb.reactivestreams.client.MongoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MongoEcommerceConfigTest {

    private val mongoEcommerceConfig = MongoEcommerceConfig()
    private val uri = "mongodb://localhost:27017/test"
    private val mongoClient: MongoClient = mongoEcommerceConfig.ecommerceReactiveMongoClient(uri)
    val database = "ecommerce"

    @Test
    fun `ecommerceReactiveMongoClient should create MongoClient with correct settings`() {
        assertThat(mongoClient).isNotNull
    }

    @Test
    fun `ecommerceReactiveMongoTemplate should create ReactiveMongoTemplate with correct client and database`() {
        val reactiveMongoTemplate =
            mongoEcommerceConfig.ecommerceReactiveMongoTemplate(mongoClient, database)

        assertThat(reactiveMongoTemplate).isNotNull
        assertThat(reactiveMongoTemplate.mongoDatabaseFactory.mongoDatabase.block()?.name)
            .isEqualTo(database)
    }
}
