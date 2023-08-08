package it.pagopa.ecommerce.helpdesk.services

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class PmService(@Autowired val connectionFactory: ConnectionFactory) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        pmSearchTransactionRequestDto: Mono<PmSearchTransactionRequestDto>
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk pm service] searchTransaction method")

        return pmSearchTransactionRequestDto
            .doOnNext { logger.info("Search type: ${it.type}") }
            .flatMapMany {
                Flux.usingWhen(
                    connectionFactory.create(),
                    { connection ->
                        Flux.from(
                                connection
                                    .createStatement("SELECT 'Hello, Oracle' FROM sys.dual")
                                    .execute()
                            )
                            .flatMap { result -> result.map { row -> row[0, String::class.java] } }
                            .doOnNext { logger.info("Read from DB: $it") }
                    },
                    { it.close() }
                )
            }
            .collectList()
            .map {
                SearchTransactionResponseDto()
                    .page(PageInfoDto().current(0).results(3).total(1))
                    .transactions(
                        listOf(
                            TransactionResultDto()
                                .product(ProductDto.PM)
                                .transactionInfo(
                                    TransactionInfoDto()
                                        .amount(100)
                                        .fee(100)
                                        .creationDate(OffsetDateTime.now())
                                        .status("TEST")
                                        .grandTotal(200)
                                )
                                .paymentDetailInfo(
                                    PaymentDetailInfoDto()
                                        .paymentContextCode("paymentContextCode")
                                        .amount(100)
                                        .iuv("IUV")
                                        .creditorInstitution("creditor institution")
                                        .paFiscalCode("77777777777")
                                )
                                .paymentInfo(
                                    PaymentInfoDto().amount(100).origin("origin").subject("subject")
                                )
                                .pspInfo(
                                    PspInfoDto()
                                        .pspId("pspId")
                                        .businessName("business name")
                                        .idChannel("id channel")
                                )
                        )
                    )
            }
    }
}
