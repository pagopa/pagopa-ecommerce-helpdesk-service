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
        ecommerceSearchTransactionRequestDto: Mono<EcommerceSearchTransactionRequestDto>
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk ecommerce service] searchTransaction method")
        return ecommerceSearchTransactionRequestDto
            .doOnNext { logger.info("Search type: ${it.type}") }
            .map {
                SearchTransactionResponseDto()
                    .totalTransactionCount(1)
                    .transactions(
                        listOf(
                            TransactionResultDto()
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
