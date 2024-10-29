package it.pagopa.ecommerce.helpdesk.controllers.v1

import it.pagopa.ecommerce.helpdesk.services.v1.HelpdeskService
import it.pagopa.ecommerce.helpdesk.services.v1.PmService
import it.pagopa.ecommerce.helpdesk.utils.PmProviderType
import it.pagopa.generated.ecommerce.helpdesk.api.HelpdeskApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("HelpdeskV1Controller")
class HelpdeskController(
    @Autowired val helpdeskService: HelpdeskService,
    @Autowired val pmService: PmService,
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
            "[HelpDesk V1 controller] SearchTransaction using ${if (searchPmInEcommerceHistory) "v2 (ecommerce history db)" else "v1 (pm legacy db)"} search"
        )
        return if (searchPmInEcommerceHistory) {
            helpDeskSearchTransactionRequestDto
                .flatMap {
                    helpdeskService.searchTransaction(
                        pageNumber = pageNumber,
                        pageSize = pageSize,
                        searchTransactionRequestDto = it,
                        pmProviderType = PmProviderType.ECOMMERCE_HISTORY
                    )
                }
                .map { ResponseEntity.ok(it) }
        } else {
            helpDeskSearchTransactionRequestDto
                .flatMap {
                    helpdeskService.searchTransaction(
                        pageNumber = pageNumber,
                        pageSize = pageSize,
                        searchTransactionRequestDto = it,
                        pmProviderType = PmProviderType.PM_LEGACY
                    )
                }
                .map { ResponseEntity.ok(it) }
        }
    }

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
