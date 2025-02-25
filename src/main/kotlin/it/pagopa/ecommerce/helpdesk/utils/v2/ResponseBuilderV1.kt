package it.pagopa.ecommerce.helpdesk.utils.v2

import io.r2dbc.spi.Result
import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationCompletedData
import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v1.TransactionUserReceiptData
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.domain.v1.TransactionWithClosureError
import it.pagopa.ecommerce.commons.domain.v1.pojos.*
import it.pagopa.ecommerce.commons.generated.server.model.AuthorizationResultDto
import it.pagopa.ecommerce.commons.utils.v1.TransactionUtils.getTransactionFee
import it.pagopa.ecommerce.helpdesk.documents.AccountingStatus
import it.pagopa.ecommerce.helpdesk.documents.PaymentStatus
import it.pagopa.ecommerce.helpdesk.documents.PmTransactionHistory
import it.pagopa.ecommerce.helpdesk.documents.UserStatus
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import it.pagopa.generated.ecommerce.nodo.v2.model.UserDto
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import org.reactivestreams.Publisher
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

fun resultToTransactionInfoDto(
    result: Result
): Publisher<it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionResultDto> =
    result.map { row ->
        it.pagopa.generated.ecommerce.helpdesk.v2.model
            .TransactionResultDto()
            .userInfo(
                it.pagopa.generated.ecommerce.helpdesk.v2.model
                    .UserInfoDto()
                    .userFiscalCode(row[0, String::class.java])
                    .notificationEmail(row[1, String::class.java])
                    .surname(row[2, String::class.java])
                    .name(row[3, String::class.java])
                    .username(row[4, String::class.java])
                    .authenticationType(row[5, String::class.java])
            )
            .transactionInfo(
                it.pagopa.generated.ecommerce.helpdesk.v2.model
                    .TransactionInfoDto()
                    .creationDate(row[6, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2")))
                    .status(row[7, String::class.java])
                    .statusDetails(row[8, String::class.java])
                    .amount(row[10, BigDecimal::class.java]?.toInt())
                    .fee(row[11, BigDecimal::class.java]?.toInt())
                    .grandTotal(row[12, BigDecimal::class.java]?.toInt())
                    .rrn(row[13, String::class.java])
                    .authorizationCode(row[14, String::class.java])
                    .paymentMethodName(row[15, String::class.java])
                    .brand(null)
            )
            .paymentInfo(
                it.pagopa.generated.ecommerce.helpdesk.v2.model
                    .PaymentInfoDto()
                    .origin(row[9, String::class.java])
                    .idTransaction(row[18, String::class.java])
                    .details(
                        listOf(
                            it.pagopa.generated.ecommerce.helpdesk.v2.model
                                .PaymentDetailInfoDto()
                                .subject(row[16, String::class.java])
                                .iuv(row[17, String::class.java])
                                .rptId(null)
                                .amount(row[24, BigDecimal::class.java]?.toInt())
                                .paymentToken(null)
                                .creditorInstitution(row[19, String::class.java])
                                .paFiscalCode(row[20, String::class.java])
                        )
                    )
            )
            .pspInfo(
                it.pagopa.generated.ecommerce.helpdesk.v2.model
                    .PspInfoDto()
                    .pspId(row[21, String::class.java])
                    .businessName(row[22, String::class.java])
                    .idChannel(row[23, String::class.java])
            )
            .product(it.pagopa.generated.ecommerce.helpdesk.v2.model.ProductDto.PM)
    }

fun pmTransactionToTransactionInfoDtoV2(
    pmTransactionHistory: PmTransactionHistory
): TransactionResultDto {
    val amount = pmTransactionHistory.transactionInfo.amount
    val fee = pmTransactionHistory.transactionInfo.fee
    val grandTotal = pmTransactionHistory.transactionInfo.grandTotal
    val email = pmTransactionHistory.userInfo.notificationEmail
    // Build user info

    val userInfo =
        UserInfoDto()
            .notificationEmail(email)
            .userFiscalCode(pmTransactionHistory.userInfo.userFiscalCode)
            .authenticationType(
                UserStatus.fromCode(pmTransactionHistory.userInfo.authenticationType)
            )
    // build transaction info
    val transactionInfo =
        TransactionInfoDto()
            .creationDate(OffsetDateTime.parse(pmTransactionHistory.transactionInfo.creationDate))
            .status(PaymentStatus.fromCode(pmTransactionHistory.transactionInfo.status))
            .statusDetails(
                pmTransactionHistory.transactionInfo.statusDetails?.let { AccountingStatus.fromCode(it) }
            )
            .eventStatus(null)
            .amount(amount)
            .fee(fee)
            .grandTotal(grandTotal)
            .rrn(pmTransactionHistory.transactionInfo.rrn)
            .authorizationCode(pmTransactionHistory.transactionInfo.authorizationCode)
            .paymentMethodName(pmTransactionHistory.transactionInfo.paymentMethodName)
            .brand(null)
            .authorizationRequestId(null)
            .paymentGateway(null)
            .authorizationOperationId(null)
            .refundOperationId(null)
    // build payment info
    val paymentInfo =
        PaymentInfoDto()
            .origin(pmTransactionHistory.paymentInfo.origin)
            .idTransaction(pmTransactionHistory.paymentInfo.details[0].idTransaction)
            .details(
                pmTransactionHistory.paymentInfo.details.map {
                    PaymentDetailInfoDto()
                        .subject(it.subject)
                        .iuv(it.iuv)
                        .rptId(null)
                        .paymentToken(null)
                        .paFiscalCode(it.paFiscalCode)
                        .amount(it.amount)
                        .creditorInstitution(it.creditorInstitution)
                }
            )
    // build psp info
    val pspInfo =
        PspInfoDto()
            .pspId(pmTransactionHistory.pspInfo.pspId)
            .idChannel(pmTransactionHistory.pspInfo.idChannel)
            .businessName(pmTransactionHistory.pspInfo.businessName)
    return TransactionResultDto()
        .product(ProductDto.PM)
        .userInfo(userInfo)
        .transactionInfo(transactionInfo)
        .paymentInfo(paymentInfo)
        .pspInfo(pspInfo)
}

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
