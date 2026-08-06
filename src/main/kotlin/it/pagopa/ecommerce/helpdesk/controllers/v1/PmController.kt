package it.pagopa.ecommerce.helpdesk.controllers.v1

import io.swagger.v3.oas.annotations.Parameter
import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.helpdesk.services.v1.PmService
import it.pagopa.generated.ecommerce.helpdesk.api.PmApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController("PmV1Controller")
class PmController(@Autowired val pmService: PmService) : PmApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun pmSearchTransaction(
        @Min(0) pageNumber: Int,
        @Min(1) @Max(20) pageSize: Int,
        pmSearchTransactionRequestDto: Mono<PmSearchTransactionRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchTransactionResponseDto>> {
        return pmSearchTransactionRequestDto
            .flatMap {
                pmService.searchTransaction(
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    pmSearchTransactionRequestDto = it
                )
            }
            .map { ResponseEntity.ok(it) }
            .doOnSuccess { _ ->
                LogTracingUtils.withContextDetailsMdc(
                    mapOf(
                        "page_number" to pageNumber,
                        "page_size" to pageSize,
                    ),
                    mapOf(
                        LogTracingUtils.TracingEntry.DEPENDENCY.key to "PM_database",
                        LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to "success"
                    )
                ) {
                    logger.info("[HelpDesk controller] pmSearchTransaction done!")
                }
            }
    }

    override fun pmSearchPaymentMethod(
        pmSearchPaymentMethodRequestDto: Mono<PmSearchPaymentMethodRequestDto>,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<SearchPaymentMethodResponseDto>> {
        return pmSearchPaymentMethodRequestDto
            .flatMap { pmService.searchPaymentMethod(pmSearchPaymentMethodRequestDto = it) }
            .map { ResponseEntity.ok(it) }
            .doOnSuccess { _ ->
                LogTracingUtils.withContextDetailsMdc(
                    null,
                    mapOf(
                        LogTracingUtils.TracingEntry.DEPENDENCY.key to "PM_database",
                        LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to "success"
                    )
                ) {
                    logger.info("[HelpDesk controller] pmSearchPaymentMethod done!")
                }
            }
    }

    override fun pmSearchBulkTransaction(
        @Parameter(description = "", name = "PmSearchBulkTransactionRequestDto", required = true)
        @Valid
        @RequestBody
        pmSearchBulkTransactionRequestDto: @Valid Mono<PmSearchBulkTransactionRequestDto>,
        @Parameter(hidden = true) exchange: ServerWebExchange
    ): Mono<ResponseEntity<Flux<TransactionBulkResultDto>>> {
        return pmSearchBulkTransactionRequestDto
            .flatMap { pmService.searchBulkTransaction(it) }
            .map { ResponseEntity.ok(Flux.fromIterable(it)) }
            .doOnSuccess { _ ->
                LogTracingUtils.withContextDetailsMdc(
                    null,
                    mapOf(
                        LogTracingUtils.TracingEntry.DEPENDENCY.key to "PM_database",
                        LogTracingUtils.TracingEntry.EVENT_OUTCOME.key to "success"
                    )
                ) {
                    logger.info("[HelpDesk controller] pmSearchBulkTransaction done!")
                }
            }
    }
}
