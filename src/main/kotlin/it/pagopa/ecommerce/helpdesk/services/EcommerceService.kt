package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class EcommerceService {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        ecommerceSearchTransactionRequestDto: EcommerceSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk ecommerce service] searchTransaction method")
        return Mono.just(
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
                                listOf(
                                    PaymentDetailInfoDto()
                                        .iuv("IUV")
                                        .rptId("rptId1")
                                        .idTransaction("paymentContextCode")
                                        .paymentToken("paymentToken")
                                        .creditorInstitution("creditor institution")
                                        .paFiscalCode("77777777777")
                                )
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
        )
    }
}
