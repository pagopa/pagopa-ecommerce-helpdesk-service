package it.pagopa.ecommerce.helpdesk

import it.pagopa.ecommerce.commons.documents.DeadLetterEvent
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import java.util.*
import org.springframework.http.HttpStatus

object HelpdeskTestUtils {

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

    fun buildSearchRequestByUserFiscalCode(
        fiscalCode: String
    ): SearchTransactionRequestFiscalCodeDto =
        SearchTransactionRequestFiscalCodeDto().userFiscalCode(fiscalCode).type("USER_FISCAL_CODE")

    fun buildSearchRequestByUserMail(email: String): SearchTransactionRequestEmailDto =
        SearchTransactionRequestEmailDto().userEmail(email).type("USER_EMAIL")

    fun buildPaymentMethodSearchRequestByUserFiscalCode(
        fiscalCode: String
    ): SearchPaymentMethodRequestFiscalCodeDto =
        SearchPaymentMethodRequestFiscalCodeDto()
            .userFiscalCode(fiscalCode)
            .type("USER_FISCAL_CODE")

    fun buildPaymentMethodSearchRequestByUserEmail(
        userEmail: String
    ): SearchPaymentMethodRequestEmailDto =
        SearchPaymentMethodRequestEmailDto().userEmail(userEmail).type("USER_EMAIL")

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
                    .details(
                        listOf(
                            PaymentDetailInfoDto()
                                .iuv("IUV")
                                .rptId(null)
                                .idTransaction(TransactionTestUtils.TRANSACTION_ID)
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

    fun buildDeadLetterEvent(queueName: String, data: String) =
        DeadLetterEvent(
            UUID.randomUUID().toString(),
            queueName,
            OffsetDateTime.now().toString(),
            data
        )
}
