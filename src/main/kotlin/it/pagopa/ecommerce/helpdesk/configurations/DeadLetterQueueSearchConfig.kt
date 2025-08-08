package it.pagopa.ecommerce.helpdesk.configurations

import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchEventSourceDto
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Dead letter search configuration */
@ApplicationScoped
class DeadLetterQueueSearchConfig {
    /** Dead letter event source dto to dead letter name mapping */
    @Produces
    @ApplicationScoped
    fun deadLetterQueueMapping(
        @ConfigProperty(name = "deadLetter.queueMapping") deadLetterMapping: Map<String, String>
    ): EnumMap<DeadLetterSearchEventSourceDto, String> {
        val mapping: EnumMap<DeadLetterSearchEventSourceDto, String> =
            EnumMap(DeadLetterSearchEventSourceDto::class.java)
        deadLetterMapping.forEach { (k, v) ->
            mapping[DeadLetterSearchEventSourceDto.valueOf(k)] = v
        }
        val notConfiguredKeys =
            DeadLetterSearchEventSourceDto.values().filter { !mapping.containsKey(it) }
        check(notConfiguredKeys.isEmpty()) {
            "Misconfigured queue mapping, no mapping found for keys: $notConfiguredKeys"
        }
        return mapping
    }
}
