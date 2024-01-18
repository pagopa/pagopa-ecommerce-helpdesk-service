package it.pagopa.ecommerce.helpdesk

import org.junit.Assert
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.properties"])
class PagopaEcommerceHelpdeskServiceApplicationKtTest {

    @Test
    fun contextLoads() {
        Assert.assertTrue(true)
    }
}
