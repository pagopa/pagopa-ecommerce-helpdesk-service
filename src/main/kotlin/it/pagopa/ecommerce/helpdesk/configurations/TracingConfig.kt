package it.pagopa.ecommerce.helpdesk.configurations

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TracingConfig {
    @Bean
    fun openTelemetry(): OpenTelemetry {
        return GlobalOpenTelemetry.get()
    }

    @Bean
    fun tracer(openTelemetry: OpenTelemetry): Tracer {
        return openTelemetry.getTracer("pagopa-ecommerce-helpdesk-service")
    }
}
