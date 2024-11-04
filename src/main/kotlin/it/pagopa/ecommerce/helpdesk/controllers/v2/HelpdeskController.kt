package it.pagopa.ecommerce.helpdesk.controllers.v2

import it.pagopa.ecommerce.helpdesk.services.v2.HelpdeskService
import it.pagopa.ecommerce.helpdesk.utils.PmProviderType
import it.pagopa.generated.ecommerce.helpdesk.v2.api.HelpdeskApi
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("HelpdeskV2Controller")
class HelpdeskController(
    @Autowired val helpdeskService: HelpdeskService,
    @Value("\${search.pm.in.ecommerce.history.enabled:false}")
    private val searchPmInEcommerceHistory: Boolean
) : HelpdeskApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    override fun helpDeskSearchTransaction(
        @Min(0) pageNumber: Int,
        @Min(1) @Max(20) pageSize: Int,
        helpDeskSearchTransactionRequestDto: Mono<HelpDeskSearchTransactionRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchTransactionResponseDto>> {
        logger.info(
            "[HelpDesk V2 controller] SearchTransaction using ${if (searchPmInEcommerceHistory) "v2 (ecommerce history db)" else "v1 (pm legacy db)"} search"
        )
        return helpDeskSearchTransactionRequestDto
            .flatMap {
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = it,
                    pmProviderType =
                        if (searchPmInEcommerceHistory) PmProviderType.ECOMMERCE_HISTORY
                        else PmProviderType.PM_LEGACY
                )
            }
            .map { ResponseEntity.ok(it) }
    }
}
