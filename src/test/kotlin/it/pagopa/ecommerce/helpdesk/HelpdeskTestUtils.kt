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

    fun buildSearchRequestByRptId(): EcommerceSearchTransactionRequestRptIdDto =
        EcommerceSearchTransactionRequestRptIdDto()
            .rptId(TransactionTestUtils.RPT_ID)
            .type("RPT_ID")

    fun buildSearchRequestByTransactionId(): EcommerceSearchTransactionRequestTransactionIdDto =
        EcommerceSearchTransactionRequestTransactionIdDto()
            .transactionId(TransactionTestUtils.TRANSACTION_ID)
            .type("TRANSACTION_ID")

    fun buildSearchRequestByPaymentToken(): EcommerceSearchTransactionRequestPaymentTokenDto =
        EcommerceSearchTransactionRequestPaymentTokenDto()
            .paymentToken(TransactionTestUtils.PAYMENT_TOKEN)
            .type("PAYMENT_TOKEN")

    fun buildSearchRequestByFiscalCode(): PmSearchTransactionRequestFiscalCodeDto =
        PmSearchTransactionRequestFiscalCodeDto()
            .userFiscalCode("AAABBB99A01A000A")
            .type("USER_FISCAL_CODE")

    fun buildSearchRequestByUserMail(): PmSearchTransactionRequestEmailDto =
        PmSearchTransactionRequestEmailDto().userEmail("test@test.it").type("USER_EMAIL")
}
