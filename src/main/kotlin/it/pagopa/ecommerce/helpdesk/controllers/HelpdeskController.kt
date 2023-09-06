package it.pagopa.ecommerce.helpdesk.controllers

import it.pagopa.ecommerce.helpdesk.services.HelpdeskService
import it.pagopa.generated.ecommerce.helpdesk.api.HelpdeskApi
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchPaymentMethodsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchPaymentMethodResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class HelpdeskController(@Autowired val helpdeskService: HelpdeskService) : HelpdeskApi {

    override fun helpDeskSearchTransaction(
        pageNumber: Int,
        pageSize: Int,
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

    override fun helpDeskSearchPaymentMethods(
        pmSearchPaymentMethodsRequestDto: Mono<PmSearchPaymentMethodsRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchPaymentMethodResponseDto>> {
        TODO("Not yet implemented")
    }
}
