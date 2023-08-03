package it.pagopa.ecommerce.helpdesk.controllers

import it.pagopa.ecommerce.helpdesk.services.EcommerceService
import it.pagopa.ecommerce.helpdesk.services.PmService
import it.pagopa.generated.ecommerce.helpdesk.api.HelpdeskApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class HelpdeskController(
    @Autowired val ecommerceService: EcommerceService,
    @Autowired val pmService: PmService
) : HelpdeskApi {

    override fun helpDeskSearchTransaction(
        pageNumber: Int,
        pageSize: Int,
        helpDeskSearchTransactionRequestDto: Mono<HelpDeskSearchTransactionRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchTransactionResponseDto>> =
        helpDeskSearchTransactionRequestDto.flatMap {
            when (it) {
                is EcommerceSearchTransactionRequestDto ->
                    ecommerceService
                        .searchTransaction(
                            pageNumber = pageNumber,
                            pageSize = pageSize,
                            ecommerceSearchTransactionRequestDto = Mono.just(it)
                        )
                        .map { response -> ResponseEntity.ok(response) }
                is PmSearchTransactionRequestDto ->
                    pmService
                        .searchTransaction(
                            pageNumber = pageNumber,
                            pageSize = pageSize,
                            pmSearchTransactionRequestDto = Mono.just(it)
                        )
                        .map { response -> ResponseEntity.ok(response) }
                else -> Mono.error(RuntimeException("Unknown search criteria"))
            }
        }
}
