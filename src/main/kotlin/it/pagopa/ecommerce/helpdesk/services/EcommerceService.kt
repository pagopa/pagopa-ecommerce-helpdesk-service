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
                            ),
                        TransactionResultDto()
                            .product(ProductDto.ECOMMERCE)
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
                            ),
                        TransactionResultDto()
                            .product(ProductDto.ECOMMERCE)
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
        )
    }
}
