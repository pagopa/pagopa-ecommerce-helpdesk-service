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
        pmSearchTransactionRequestDto: PmSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk pm service] searchTransaction method")

        return Flux.usingWhen(
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
            .collectList()
            .map {
                SearchTransactionResponseDto()
                    .page(PageInfoDto().current(0).results(3).total(1))
                    .transactions(
                        listOf(
                            TransactionResultDto()
                                .product(ProductDto.ECOMMERCE)
                                .userInfo(
                                    UserInfoDto()
                                        .userFiscalCode("userFiscalCode")
                                        .notificationEmail("notificationEmail")
                                        .surname("surname")
                                        .name("name")
                                        .username("username")
                                        .authenticationType("auth type")
                                )
                                .transactionInfo(
                                    TransactionInfoDto()
                                        .amount(100)
                                        .fee(100)
                                        .creationDate(OffsetDateTime.now())
                                        .status("status")
                                        .statusDetails("status details")
                                        .grandTotal(200)
                                        .rrn("rrn")
                                        .authotizationCode("authCode")
                                        .paymentMethodName("paymentMethodName")
                                        .brand("brand")
                                )
                                .paymentDetailInfo(
                                    PaymentDetailInfoDto()
                                        .iuv("IUV")
                                        .rptIds(listOf("rptId1", "rptId2"))
                                        .idTransaction("paymentContextCode")
                                        .paymentToken("paymentToken")
                                        .creditorInstitution("creditor institution")
                                        .paFiscalCode("77777777777")
                                )
                                .paymentInfo(PaymentInfoDto().origin("origin").subject("subject"))
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
