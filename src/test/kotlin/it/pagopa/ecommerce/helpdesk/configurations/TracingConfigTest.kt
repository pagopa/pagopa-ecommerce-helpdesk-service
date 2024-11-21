package it.pagopa.ecommerce.helpdesk.configurations

import io.opentelemetry.api.trace.Tracer
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import org.mockito.kotlin.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TracingConfigTest {

    @Bean @Primary fun testTracer(): Tracer = mock()

    @Bean @Primary fun npgApiKeyConfiguration(): NpgApiKeyConfiguration = mock()
}
