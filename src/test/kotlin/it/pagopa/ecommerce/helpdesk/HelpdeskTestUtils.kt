package it.pagopa.ecommerce.helpdesk

import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import it.pagopa.generated.ecommerce.helpdesk.model.*
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

    fun buildSearchRequestByFiscalCode(): SearchTransactionRequestFiscalCodeDto =
        SearchTransactionRequestFiscalCodeDto()
            .userFiscalCode("AAABBB99A01A000A")
            .type("USER_FISCAL_CODE")

    fun buildSearchRequestByUserMail(): SearchTransactionRequestEmailDto =
        SearchTransactionRequestEmailDto().userEmail("test@test.it").type("USER_EMAIL")
}
