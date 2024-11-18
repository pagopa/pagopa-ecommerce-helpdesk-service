package it.pagopa.ecommerce.helpdesk

import it.pagopa.ecommerce.helpdesk.configurations.TracingConfigTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.properties"])
@Import(TracingConfigTest::class)
class PagopaEcommerceHelpdeskServiceApplicationKtTest {

    @Test
    fun contextLoads() {
        Assertions.assertTrue(true)
    }
}
