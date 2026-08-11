package it.pagopa.ecommerce.helpdesk.controllers.v2

import it.pagopa.ecommerce.commons.mdcutilities.LogTracingUtils
import it.pagopa.ecommerce.helpdesk.services.v2.EcommerceService
import it.pagopa.ecommerce.helpdesk.utils.v2.LogUtils
import it.pagopa.generated.ecommerce.helpdesk.v2.api.EcommerceApi
import it.pagopa.generated.ecommerce.helpdesk.v2.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchMetricsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionMetricsResponseDto
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("EcommerceV2Controller")
class EcommerceController(@Autowired val ecommerceService: EcommerceService) : EcommerceApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun ecommerceSearchTransaction(
        @Min(0) pageNumber: Int,
        @Min(1) @Max(20) pageSize: Int,
        ecommerceSearchTransactionRequestDto: Mono<EcommerceSearchTransactionRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<SearchTransactionResponseDto>> {
        return ecommerceSearchTransactionRequestDto.flatMap { dto ->
            // Add attributes based on the DTO's type to enrich the log context
            val logContextAttribute = LogUtils.extractContextAttributeFromDto(dto)

            ecommerceService
                .searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = dto
                )
                .map { ResponseEntity.ok(it) }
                .doOnSuccess {
                    LogTracingUtils.loggerTracingUtils()
                        .success()
                        .details(
                            mapOf(
                                "page_number" to pageNumber.toString(),
                                "page_size" to pageSize.toString()
                            )
                        )
                        .dependency(LogTracingUtils.MONGO_DEPENDENCY)
                        .logInfo(logger, "ecommerceSearchTransaction done successfully!")
                }
                .contextWrite { context ->
                    logContextAttribute?.let { (key, value) ->
                        LogTracingUtils.enrichContextForEvent(mapOf(key to value), context)
                    } ?: context
                }
        }
    }

    override fun ecommerceSearchMetrics(
        searchMetricsRequestDto: Mono<SearchMetricsRequestDto>,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<TransactionMetricsResponseDto>> {
        return searchMetricsRequestDto
            .flatMap { ecommerceService.searchMetrics(searchMetricsRequestDto = it) }
            .map { ResponseEntity.ok(it) }
            .doOnSuccess { _ ->
                LogTracingUtils.loggerTracingUtils()
                    .success()
                    .dependency(LogTracingUtils.MONGO_DEPENDENCY)
                    .logInfo(logger, "ecommerceSearchMetrics done successfully!")
            }
    }
}
