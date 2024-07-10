package it.pagopa.ecommerce.helpdesk.configurations

import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchEventSourceDto
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Dead letter search configuration */
@Configuration
class DeadLetterQueueSearchConfig {
    /** Dead letter event source dto to dead letter name mapping */
    @Bean
    fun deadLetterQueueMapping(
        @Value("#{\${deadLetter.queueMapping}}") deadLetterMapping: Map<String, String>
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

    /** Dead letter event source dto to dead letter name mapping */
    @Bean
    fun deadLetterQueueMappingV2(
        @Value("#{\${deadLetter.queueMapping}}") deadLetterMapping: Map<String, String>
    ): EnumMap<
        it.pagopa.generated.ecommerce.helpdesk.v2.model.DeadLetterSearchEventSourceDto, String
    > {
        val mapping:
            EnumMap<
                it.pagopa.generated.ecommerce.helpdesk.v2.model.DeadLetterSearchEventSourceDto,
                String
            > =
            EnumMap(
                it.pagopa.generated.ecommerce.helpdesk.v2.model
                        .DeadLetterSearchEventSourceDto::class
                    .java
            )
        deadLetterMapping.forEach { (k, v) ->
            mapping[
                it.pagopa.generated.ecommerce.helpdesk.v2.model.DeadLetterSearchEventSourceDto
                    .valueOf(k)] = v
        }
        val notConfiguredKeys =
            it.pagopa.generated.ecommerce.helpdesk.v2.model.DeadLetterSearchEventSourceDto.values()
                .filter { !mapping.containsKey(it) }
        check(notConfiguredKeys.isEmpty()) {
            "Misconfigured queue mapping, no mapping found for keys: $notConfiguredKeys"
        }
        return mapping
    }
}
