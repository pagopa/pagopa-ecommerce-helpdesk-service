package it.pagopa.ecommerce.helpdesk.controllers

import it.pagopa.ecommerce.helpdesk.services.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.api.EcommerceApi
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchDeadLetterEventsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchDeadLetterEventResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class EcommerceController(@Autowired val ecommerceService: EcommerceService) : EcommerceApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun ecommerceSearchTransaction(
        pageNumber: Int,
        pageSize: Int,
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
        pageNumber: Int,
        pageSize: Int,
        ecommerceSearchDeadLetterEventsRequestDto: Mono<EcommerceSearchDeadLetterEventsRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchDeadLetterEventResponseDto>> {
        TODO("Not yet implemented")
    }
}
