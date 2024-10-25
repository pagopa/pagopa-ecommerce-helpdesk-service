package it.pagopa.ecommerce.helpdesk.configurations

import com.mongodb.reactivestreams.client.MongoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MongoPMConfigTest {
    private val mongoPMConfig = MongoPMConfig()
    private val uri = "mongodb://localhost:27017/test"
    private val mongoClient: MongoClient = mongoPMConfig.pmReactiveMongoClient(uri)
    val database = "pm"

    @Test
    fun `pmReactiveMongoClient should create MongoClient with correct settings`() {
        assertThat(mongoClient).isNotNull
    }

    @Test
    fun `pmReactiveMongoTemplate should create ReactiveMongoTemplate with correct client and database`() {
        val reactiveMongoTemplate = mongoPMConfig.pmReactiveMongoTemplate(mongoClient, database)

        assertThat(reactiveMongoTemplate).isNotNull
        assertThat(reactiveMongoTemplate.mongoDatabaseFactory.mongoDatabase.block()?.name)
            .isEqualTo(database)
    }
}
