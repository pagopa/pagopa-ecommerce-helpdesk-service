package it.pagopa.ecommerce.helpdesk

import it.pagopa.ecommerce.commons.documents.v2.TransactionEvent
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.*
import org.springframework.http.HttpStatus

object HelpdeskTestUtilsV2 {

    fun convertEventsToEventInfoList(events: List<TransactionEvent<Any>>): List<EventInfoDto> =
        events.map {
            EventInfoDto()
                .eventCode(it.eventCode)
                .creationDate(ZonedDateTime.parse(it.creationDate).toOffsetDateTime())
        }
    fun buildProblemJson(
        httpStatus: HttpStatus,
        title: String,
        description: String
    ): ProblemJsonDto = ProblemJsonDto().status(httpStatus.value()).detail(description).title(title)

    fun buildSearchRequestByRptId(): SearchTransactionRequestRptIdDto =
        SearchTransactionRequestRptIdDto().rptId(TransactionTestUtils.RPT_ID).type("RPT_ID")

    fun buildSearchRequestByTransactionId(): SearchTransactionRequestTransactionIdDto =
        SearchTransactionRequestTransactionIdDto()
            .transactionId(TransactionTestUtils.TRANSACTION_ID)
            .type("TRANSACTION_ID")

    fun buildSearchRequestByPaymentToken(): SearchTransactionRequestPaymentTokenDto =
        SearchTransactionRequestPaymentTokenDto()
            .paymentToken(TransactionTestUtils.PAYMENT_TOKEN)
            .type("PAYMENT_TOKEN")

    fun buildSearchRequestByUserMail(email: String): SearchTransactionRequestEmailDto =
        SearchTransactionRequestEmailDto().userEmail(email).type("USER_EMAIL")

    fun buildSearchRequestByUserFiscalCode(
        fiscalCode: String
    ): SearchTransactionRequestFiscalCodeDto =
        SearchTransactionRequestFiscalCodeDto().userFiscalCode(fiscalCode).type("USER_FISCAL_CODE")

    fun buildTransactionResultDto(
        creationDate: OffsetDateTime,
        product: ProductDto
    ): TransactionResultDto =
        TransactionResultDto()
            .userInfo(
                UserInfoDto()
                    .userFiscalCode("user fiscal code")
                    .notificationEmail(TransactionTestUtils.EMAIL_STRING)
                    .surname("surname")
                    .name("name")
                    .username("username")
                    .authenticationType("auth type")
            )
            .transactionInfo(
                TransactionInfoDto()
                    .creationDate(creationDate)
                    .status("status")
                    .statusDetails("status detail")
                    .amount(500)
                    .fee(200)
                    .grandTotal(700)
                    .rrn("rrn")
                    .authorizationCode("authorization code")
                    .paymentMethodName("payment method name")
                    .brand(null)
            )
            .paymentInfo(
                PaymentInfoDto()
                    .origin("origin")
                    .idTransaction(TransactionTestUtils.TRANSACTION_ID)
                    .details(
                        listOf(
                            PaymentDetailInfoDto()
                                .iuv("IUV")
                                .rptId(null)
                                .amount(500)
                                .paymentToken(null)
                                .creditorInstitution("creditor institution")
                                .paFiscalCode(TransactionTestUtils.PA_FISCAL_CODE)
                        )
                    )
            )
            .pspInfo(
                PspInfoDto()
                    .pspId(TransactionTestUtils.PSP_ID)
                    .businessName(TransactionTestUtils.PSP_BUSINESS_NAME)
                    .idChannel(TransactionTestUtils.PSP_CHANNEL_CODE)
            )
            .product(product)
}
