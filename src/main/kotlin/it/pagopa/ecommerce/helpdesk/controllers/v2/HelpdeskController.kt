package it.pagopa.ecommerce.helpdesk.controllers.v2

import it.pagopa.ecommerce.helpdesk.services.v2.HelpdeskService
import it.pagopa.generated.ecommerce.helpdesk.v2.api.HelpdeskApi
import it.pagopa.generated.ecommerce.helpdesk.v2.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("HelpdeskV2Controller")
class HelpdeskController(
    @Autowired val helpdeskService: HelpdeskService,
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
}
