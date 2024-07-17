package it.pagopa.ecommerce.helpdesk.utils.v2

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationCompletedData
import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v1.TransactionUserReceiptData
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.domain.v1.TransactionWithClosureError
import it.pagopa.ecommerce.commons.domain.v1.pojos.*
import it.pagopa.ecommerce.commons.generated.server.model.AuthorizationResultDto
import it.pagopa.ecommerce.commons.utils.v1.TransactionUtils.getTransactionFee
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import it.pagopa.generated.ecommerce.nodo.v2.model.UserDto
import java.time.ZonedDateTime
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ResponseBuilderV1Logger {
    val logger: Logger = LoggerFactory.getLogger(ResponseBuilderV1Logger::class.java)
}

fun buildTransactionSearchResponse(
    currentPage: Int,
    totalCount: Int,
    pageSize: Int,
    results: List<TransactionResultDto>
): SearchTransactionResponseDto =
    SearchTransactionResponseDto()
        .page(
            PageInfoDto()
                .current(currentPage)
                .total(calculateTotalPages(totalCount = totalCount, pageSize = pageSize))
                .results(results.size)
        )
        .transactions(results)

fun baseTransactionToTransactionInfoDtoV1(
    baseTransaction: BaseTransaction,
    email: Optional<Email>,
    events: List<BaseTransactionEvent<Any>>
): TransactionResultDto {
    val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
    val fee = getTransactionFees(baseTransaction).orElse(0)
    val totalAmount = amount.plus(fee)
    val transactionAuthorizationRequestData = getTransactionAuthRequestedData(baseTransaction)
    val transactionAuthorizationCompletedData = getTransactionAuthCompletedData(baseTransaction)
    val transactionUserReceiptData = getTransactionUserReceiptData(baseTransaction)

    // get event info list
    val eventInfoList =
        events.map { event ->
            EventInfoDto()
                .creationDate(ZonedDateTime.parse(event.creationDate).toOffsetDateTime())
                .eventCode(event.eventCode)
        }

    // Build user info

    val userInfo =
        UserInfoDto()
            .notificationEmail(email.map { it.value }.orElse("N/A"))
            // TODO this field is statically valued with GUEST eCommerce side into Nodo ClosePayment
            // requests. Must be populated dinamically when logic will be updated eCommerce side
            // (event-dispatcher/transactions-service)
            .authenticationType(UserDto.TypeEnum.GUEST.toString())
    // build transaction info
    val transactionInfo =
        TransactionInfoDto()
            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
            .status(getTransactionDetailsStatus(baseTransaction))
            .statusDetails(transactionAuthorizationCompletedData?.errorCode)
            .events(eventInfoList)
            .eventStatus(TransactionStatusDto.valueOf(baseTransaction.status.toString()))
            .amount(amount)
            .fee(fee)
            .grandTotal(totalAmount)
            .rrn(transactionAuthorizationCompletedData?.rrn)
            .authorizationCode(transactionAuthorizationCompletedData?.authorizationCode)
            .paymentMethodName(transactionAuthorizationRequestData?.paymentMethodName)
            .brand(transactionAuthorizationRequestData?.brand?.toString())
            .authorizationRequestId(transactionAuthorizationRequestData?.authorizationRequestId)
            .paymentGateway(transactionAuthorizationRequestData?.paymentGateway?.toString())
    // build payment info
    val paymentInfo =
        PaymentInfoDto()
            .origin(baseTransaction.clientId.toString())
            .idTransaction(baseTransaction.transactionId.value())
            .details(
                baseTransaction.paymentNotices.map {
                    PaymentDetailInfoDto()
                        .subject(it.transactionDescription.value)
                        .rptId(it.rptId.value)
                        .amount(it.transactionAmount.value)
                        .paymentToken(it.paymentToken.value)
                        // TODO here set only the first into transferList or take it from rptId
                        // object?
                        .paFiscalCode(it.transferList[0].paFiscalCode)
                        .creditorInstitution(transactionUserReceiptData?.receivingOfficeName)
                }
            )
    // build psp info
    val pspInfo =
        PspInfoDto()
            .pspId(transactionAuthorizationRequestData?.pspId)
            .idChannel(transactionAuthorizationRequestData?.pspChannelCode)
            .businessName(transactionAuthorizationRequestData?.pspBusinessName)
    return TransactionResultDto()
        .product(ProductDto.ECOMMERCE)
        .userInfo(userInfo)
        .transactionInfo(transactionInfo)
        .paymentInfo(paymentInfo)
        .pspInfo(pspInfo)
}

private fun getTransactionFees(baseTransaction: BaseTransaction): Optional<Int> =
    when (baseTransaction) {
        is BaseTransactionExpired -> getTransactionFee(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithClosureError ->
            getTransactionFee(baseTransaction.transactionAtPreviousState)
        else -> getTransactionFee(baseTransaction)
    }

private fun getTransactionAuthRequestedData(
    baseTransaction: BaseTransaction
): TransactionAuthorizationRequestData? =
    when (baseTransaction) {
        is BaseTransactionExpired ->
            getTransactionAuthRequestedData(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithClosureError ->
            getTransactionAuthRequestedData(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithRequestedAuthorization ->
            baseTransaction.transactionAuthorizationRequestData
        else -> null
    }

private fun getTransactionAuthCompletedData(
    baseTransaction: BaseTransaction
): TransactionAuthorizationCompletedData? =
    when (baseTransaction) {
        is BaseTransactionExpired ->
            getTransactionAuthCompletedData(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithClosureError ->
            getTransactionAuthCompletedData(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithRefundRequested ->
            getTransactionAuthCompletedData(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithCompletedAuthorization ->
            baseTransaction.transactionAuthorizationCompletedData
        else -> null
    }

private fun getTransactionUserReceiptData(
    baseTransaction: BaseTransaction
): TransactionUserReceiptData? =
    when (baseTransaction) {
        is BaseTransactionExpired ->
            getTransactionUserReceiptData(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithRefundRequested ->
            getTransactionUserReceiptData(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithRequestedUserReceipt -> baseTransaction.transactionUserReceiptData
        else -> null
    }

private fun getTransactionDetailsStatus(baseTransaction: BaseTransaction): String =
    when (getAuthorizationOutcomeV1(baseTransaction)) {
        AuthorizationResultDto.OK -> "Confermato"
        AuthorizationResultDto.KO -> "Rifiutato"
        else -> "Cancellato"
    }

fun getAuthorizationOutcomeV1(baseTransaction: BaseTransaction): AuthorizationResultDto? =
    when (baseTransaction) {
        is BaseTransactionExpired ->
            getAuthorizationOutcomeV1(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithRefundRequested ->
            getAuthorizationOutcomeV1(baseTransaction.transactionAtPreviousState)
        is TransactionWithClosureError ->
            getAuthorizationOutcomeV1(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithCompletedAuthorization ->
            baseTransaction.transactionAuthorizationCompletedData.authorizationResultDto
        else -> null
    }

private fun calculateTotalPages(totalCount: Int, pageSize: Int) =
    if (totalCount % pageSize == 0) {
        totalCount / pageSize
    } else {
        totalCount / pageSize + 1
    }
