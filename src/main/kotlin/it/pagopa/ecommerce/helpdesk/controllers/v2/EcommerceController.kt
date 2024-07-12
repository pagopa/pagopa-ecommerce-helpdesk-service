package it.pagopa.ecommerce.helpdesk.controllers.v2

import it.pagopa.ecommerce.helpdesk.services.v2.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.v2.api.V2Api
import it.pagopa.generated.ecommerce.helpdesk.v2.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("EcommerceV2Controller")
class EcommerceController(@Autowired val ecommerceService: EcommerceService) : V2Api {
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
}
