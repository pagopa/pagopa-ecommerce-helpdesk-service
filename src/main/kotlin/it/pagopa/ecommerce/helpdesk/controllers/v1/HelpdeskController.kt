package it.pagopa.ecommerce.helpdesk.controllers.v1

import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.helpdesk.services.v1.HelpdeskService
import it.pagopa.ecommerce.helpdesk.services.v1.PmService
import it.pagopa.ecommerce.helpdesk.utils.v1.LogUtils
import it.pagopa.generated.ecommerce.helpdesk.api.HelpdeskApi
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchPaymentMethodRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchPaymentMethodResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("HelpdeskV1Controller")
class HelpdeskController(
    @Autowired val helpdeskService: HelpdeskService,
    @Autowired val pmService: PmService
) : HelpdeskApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun helpDeskSearchTransaction(
        @Min(0) pageNumber: Int,
        @Min(1) @Max(20) pageSize: Int,
        helpDeskSearchTransactionRequestDto: Mono<HelpDeskSearchTransactionRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchTransactionResponseDto>> =
        helpDeskSearchTransactionRequestDto.flatMap { dto ->
            // Add attributes based on the DTO's type to enrich the log context
            val logContextAttribute = LogUtils.extractContextAttributeFromDto(dto)

            helpdeskService
                .searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = dto
                )
                .map { ResponseEntity.ok(it) }
                .contextWrite { context ->
                    logContextAttribute?.let { (key, value) ->
                        LogTracingUtils.enrichContextForEvent(mapOf(key to value), context)
                    } ?: context
                }
        }

    override fun helpDeskSearchPaymentMethod(
        pmSearchPaymentMethodRequestDto: Mono<PmSearchPaymentMethodRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchPaymentMethodResponseDto>> {
        return pmSearchPaymentMethodRequestDto
            .flatMap { pmService.searchPaymentMethod(pmSearchPaymentMethodRequestDto = it) }
            .map { ResponseEntity.ok(it) }
    }
}
