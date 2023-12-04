package it.pagopa.ecommerce.helpdesk.configurations

import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchEventSourceDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class DeadLetterQueueSearchConfigTest {

    private val confMap =
        DeadLetterSearchEventSourceDto.values().associate { Pair(it.toString(), it.toString()) }

    private val deadLetterQueueSearchConfig = DeadLetterQueueSearchConfig()

    @Test
    fun `Should build configuration map successfully`() {
        assertDoesNotThrow { deadLetterQueueSearchConfig.deadLetterQueueMapping(confMap) }
    }

    @Test
    fun `Should throw Exception for missing configuration key`() {
        val exception =
            assertThrows<IllegalStateException> {
                deadLetterQueueSearchConfig.deadLetterQueueMapping(mapOf())
            }
        assertEquals(
            "Misconfigured queue mapping, no mapping found for keys: [ALL, ECOMMERCE, NOTIFICATIONS_SERVICE]",
            exception.message
        )
    }

    @Test
    fun `Should throw Exception for invalid configuration key`() {
        val wrongConfMap = confMap.toMutableMap()
        wrongConfMap["FAKE"] = "FAKE"
        val exception =
            assertThrows<IllegalArgumentException> {
                deadLetterQueueSearchConfig.deadLetterQueueMapping(wrongConfMap)
            }
        assertTrue(exception.message!!.contains("FAKE"))
    }
}
