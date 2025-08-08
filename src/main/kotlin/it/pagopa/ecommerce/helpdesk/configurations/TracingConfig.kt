package it.pagopa.ecommerce.helpdesk.configurations

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ApplicationScoped
class TracingConfig {
    @Produces
    @ApplicationScoped
    fun openTelemetry(): OpenTelemetry {
        return GlobalOpenTelemetry.get()
    }

    @Produces
    @ApplicationScoped
    fun tracer(openTelemetry: OpenTelemetry): Tracer {
        return openTelemetry.getTracer("pagopa-ecommerce-helpdesk-service")
    }
}
