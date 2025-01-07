package it.pagopa.ecommerce.helpdesk.controllers.v1

import io.swagger.v3.oas.annotations.Parameter
import it.pagopa.ecommerce.helpdesk.services.v1.PmService
import it.pagopa.generated.ecommerce.helpdesk.api.PmApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.OffsetDateTime
import java.util.*
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
        logger.info("[HelpDesk controller] pmSearchTransaction")
        return pmSearchTransactionRequestDto
            .flatMap {
                pmService.searchTransaction(
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    pmSearchTransactionRequestDto = it
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

    override fun pmSearchBulkTransaction(
        @Parameter(description = "", name = "PmSearchBulkTransactionRequestDto", required = true)
        @Valid
        @RequestBody
        pmSearchBulkTransactionRequestDto: @Valid Mono<PmSearchBulkTransactionRequestDto>?,
        @Parameter(hidden = true) exchange: ServerWebExchange?
    ): Mono<ResponseEntity<Flux<TransactionBulkResultDto>>>? {
        val mockedResults: Flux<TransactionBulkResultDto> =
            Flux.range(1, 3).map { index ->
                TransactionBulkResultDto()
                    .id(UUID.randomUUID().toString())
                    .transactionInfo(
                        TransactionInfoDto()
                            .creationDate(OffsetDateTime.now().minusDays(index.toLong()))
                            .status(if (index % 2 == 0) "COMPLETED" else "PENDING")
                            .statusDetails(
                                if (index % 2 == 0) "Payment successful"
                                else "Awaiting confirmation"
                            )
                            .eventStatus(TransactionStatusDto.NOTIFIED_OK)
                            .amount((index + 1) * 1000)
                            .fee(100)
                            .grandTotal((index + 1) * 1000 + 100)
                            .rrn("RRN_$index")
                            .authorizationCode("AUTH_$index")
                            .paymentMethodName(if (index % 3 == 0) "CARD" else "BANK_TRANSFER")
                            .brand(if (index % 3 == 0) "VISA" else "MASTERCARD")
                            .authorizationRequestId("REQ_$index")
                            .paymentGateway("GATEWAY_$index")
                            .correlationId(UUID.randomUUID())
                            .gatewayAuthorizationStatus(
                                if (index % 2 == 0) "AUTHORIZED" else "PENDING"
                            )
                            .gatewayErrorCode(if (index % 2 == 0) null else "ERR_$index"),
                    )
                    .userInfo(
                        UserInfoBulkDto()
                            .userFiscalCode("FISCALCODE$index".padEnd(16, 'X'))
                            .notificationEmail("user$index@example.com")
                            .authenticationType("SPID")
                    )
            }

        return Mono.just(ResponseEntity.ok(mockedResults))
    }
}
