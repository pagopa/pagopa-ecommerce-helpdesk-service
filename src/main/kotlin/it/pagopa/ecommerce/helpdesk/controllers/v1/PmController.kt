package it.pagopa.ecommerce.helpdesk.controllers.v1

import it.pagopa.ecommerce.helpdesk.services.v1.PmService
import it.pagopa.ecommerce.helpdesk.utils.PmProviderType
import it.pagopa.generated.ecommerce.helpdesk.api.PmApi
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchPaymentMethodRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchPaymentMethodResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("PmV1Controller")
class PmController(
    @Autowired val pmService: PmService,
    @Value("\${search.pm.in.ecommerce.history.enabled:false}")
    private val searchPmInEcommerceHistory: Boolean
) : PmApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun pmSearchTransaction(
        @Min(0) pageNumber: Int,
        @Min(1) @Max(20) pageSize: Int,
        pmSearchTransactionRequestDto: Mono<PmSearchTransactionRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchTransactionResponseDto>> {
        logger.info(
            "[PM V1 controller] SearchTransaction using ${if (searchPmInEcommerceHistory) "v2 (ecommerce history db)" else "v1 (pm legacy db)"} search"
        )
        return pmSearchTransactionRequestDto
            .flatMap {
                pmService.searchTransaction(
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    pmSearchTransactionRequestDto = it,
                    pmProviderType =
                        if (searchPmInEcommerceHistory) PmProviderType.ECOMMERCE_HISTORY
                        else PmProviderType.PM_LEGACY
                )
            }
            .map { ResponseEntity.ok(it) }
    }

    override fun pmSearchPaymentMethod(
        pmSearchPaymentMethodRequestDto: Mono<PmSearchPaymentMethodRequestDto>,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<SearchPaymentMethodResponseDto>> {
        logger.info("[HelpDesk controller] pmSearchPaymentMethod")
        return pmSearchPaymentMethodRequestDto
            .flatMap { pmService.searchPaymentMethod(pmSearchPaymentMethodRequestDto = it) }
            .map { ResponseEntity.ok(it) }
    }
}
