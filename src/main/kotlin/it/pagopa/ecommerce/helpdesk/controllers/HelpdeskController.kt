package it.pagopa.ecommerce.helpdesk.controllers

import it.pagopa.ecommerce.helpdesk.services.HelpdeskService
import it.pagopa.ecommerce.helpdesk.services.PmService
import it.pagopa.generated.ecommerce.helpdesk.api.HelpdeskApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
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
        helpDeskSearchTransactionRequestDto
            .flatMap {
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = it
                )
            }
            .map { ResponseEntity.ok(it) }

    override fun helpDeskSearchPaymentMethod(
        pmSearchPaymentMethodRequestDto: Mono<PmSearchPaymentMethodRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchPaymentMethodResponseDto>> {
        logger.info("[HelpDesk controller] pmSearchPaymentMethod")
        return pmSearchPaymentMethodRequestDto
            .flatMap { pmService.searchPaymentMethod(pmSearchPaymentMethodRequestDto = it) }
            .map { ResponseEntity.ok(it) }
    }
}
