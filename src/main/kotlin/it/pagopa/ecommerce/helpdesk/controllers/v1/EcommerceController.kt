package it.pagopa.ecommerce.helpdesk.controllers.v1

import it.pagopa.ecommerce.helpdesk.services.v1.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.api.EcommerceApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("EcommerceV1Controller")
class EcommerceController(@Autowired val ecommerceService: EcommerceService) : EcommerceApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun ecommerceSearchTransaction(
        @Min(0) pageNumber: Int,
        @Min(1) @Max(20) pageSize: Int,
        ecommerceSearchTransactionRequestDto: Mono<EcommerceSearchTransactionRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchTransactionResponseDto>> {
        logger.info("[HelpDesk controller] ecommerceSearchTransaction")
        return ecommerceSearchTransactionRequestDto
            .flatMap {
                ecommerceService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = it
                )
            }
            .map { ResponseEntity.ok(it) }
    }

    override fun ecommerceSearchDeadLetterEvents(
        @Min(0) pageNumber: Int,
        @Min(1) @Max(20) pageSize: Int,
        ecommerceSearchDeadLetterEventsRequestDto: Mono<EcommerceSearchDeadLetterEventsRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchDeadLetterEventResponseDto>> {
        logger.info("[HelpDesk controller] ecommerceSearchDeadLetterEvents")
        return ecommerceSearchDeadLetterEventsRequestDto
            .flatMap {
                ecommerceService.searchDeadLetterEvents(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchRequest = it
                )
            }
            .map { ResponseEntity.ok(it) }
    }

    override fun ecommerceSearchNpgOperationsPost(
        searchNpgOperationsRequestDto: Mono<SearchNpgOperationsRequestDto>?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<SearchNpgOperationsResponseDto>> {
        TODO("Not yet implemented")
    }
}
