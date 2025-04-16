package it.pagopa.ecommerce.helpdesk.utils.v2

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.v2.TransactionActivatedData
import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationCompletedData
import it.pagopa.ecommerce.commons.documents.v2.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v2.TransactionUserReceiptData
import it.pagopa.ecommerce.commons.documents.v2.activation.NpgTransactionGatewayActivationData
import it.pagopa.ecommerce.commons.documents.v2.authorization.*
import it.pagopa.ecommerce.commons.documents.v2.refund.NpgGatewayRefundData
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.domain.v2.TransactionWithClosureError
import it.pagopa.ecommerce.commons.domain.v2.pojos.*
import it.pagopa.ecommerce.commons.generated.npg.v1.dto.OperationResultDto
import it.pagopa.ecommerce.commons.generated.server.model.AuthorizationResultDto
import it.pagopa.ecommerce.commons.utils.v2.TransactionUtils.getTransactionFee
import it.pagopa.ecommerce.helpdesk.documents.EcommerceStatusCount
import it.pagopa.ecommerce.helpdesk.utils.GatewayAuthorizationData
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import it.pagopa.generated.ecommerce.nodo.v2.model.UserDto
import java.time.ZonedDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

fun baseTransactionToTransactionInfoDtoV2(
    baseTransaction: BaseTransaction,
    email: Optional<Email>,
    events: List<BaseTransactionEvent<Any>>
): TransactionResultDto {
    val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
    val fee = getTransactionFees(baseTransaction).orElse(0)
    val totalAmount = amount.plus(fee)
    val transactionActivatedData = getTransactionActivatedData(baseTransaction)
    val transactionGatewayActivationData =
        transactionActivatedData?.transactionGatewayActivationData
    val transactionAuthorizationRequestData = getTransactionAuthRequestedData(baseTransaction)
    val transactionAuthorizationCompletedData = getTransactionAuthCompletedData(baseTransaction)
    val gatewayAuthorizationData =
        getGatewayAuthorizationData(
            transactionAuthorizationCompletedData?.transactionGatewayAuthorizationData
        )
    val authorizationOperationId = getAuthorizationOperationId(baseTransaction)
    val refundOperationId = getRefundOperationId(baseTransaction)

    // Build user info

    val userInfo =
        UserInfoDto()
            .notificationEmail(email.map { it.value }.orElse("N/A"))
            .authenticationType(
                transactionActivatedData.let {
                    if (it?.userId != null) {
                        UserDto.TypeEnum.REGISTERED.toString()
                    } else {
                        UserDto.TypeEnum.GUEST.toString()
                    }
                }
            )

    val eventInfoList =
        events.map { event ->
            EventInfoDto()
                .creationDate(ZonedDateTime.parse(event.creationDate).toOffsetDateTime())
                .eventCode(event.eventCode)
        }

    // build transaction info
    val transactionInfo =
        TransactionInfoDto()
            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
            .status(getTransactionDetailsStatus(baseTransaction))
            .statusDetails(
                getStatusDetail(
                    transactionAuthorizationCompletedData?.transactionGatewayAuthorizationData
                )
            )
            .events(eventInfoList)
            .eventStatus(TransactionStatusDto.valueOf(baseTransaction.status.toString()))
            .amount(amount)
            .fee(fee)
            .grandTotal(totalAmount)
            .rrn(transactionAuthorizationCompletedData?.rrn)
            .authorizationCode(transactionAuthorizationCompletedData?.authorizationCode)
            .authorizationOperationId(authorizationOperationId)
            .refundOperationId(refundOperationId)
            .paymentMethodName(transactionAuthorizationRequestData?.paymentMethodName)
            .brand(
                getBrand(
                    transactionAuthorizationRequestData
                        ?.transactionGatewayAuthorizationRequestedData
                )
            )
            .authorizationRequestId(transactionAuthorizationRequestData?.authorizationRequestId)
            .paymentGateway(transactionAuthorizationRequestData?.paymentGateway?.toString())
            .correlationId(
                if (transactionGatewayActivationData is NpgTransactionGatewayActivationData)
                    UUID.fromString(transactionGatewayActivationData.correlationId)
                else null
            )
            .gatewayAuthorizationStatus(gatewayAuthorizationData?.authorizationStatus)
            .gatewayErrorCode(gatewayAuthorizationData?.errorCode)
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
                        .creditorInstitution(it.companyName.value)
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

fun getStatusDetail(
    transactionGatewayAuthorizationData: TransactionGatewayAuthorizationData?
): String? =
    when (transactionGatewayAuthorizationData) {
        is PgsTransactionGatewayAuthorizationData -> transactionGatewayAuthorizationData.errorCode
        is NpgTransactionGatewayAuthorizationData -> null
        is RedirectTransactionGatewayAuthorizationData ->
            transactionGatewayAuthorizationData.errorCode
        else -> null
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

private fun getTransactionActivatedData(
    baseTransaction: BaseTransaction
): TransactionActivatedData? =
    if (baseTransaction is BaseTransactionWithPaymentToken) {
        baseTransaction.transactionActivatedData
    } else {
        null
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
    when (getAuthorizationOutcomeV2(baseTransaction)) {
        AuthorizationResultDto.OK -> "Confermato"
        AuthorizationResultDto.KO -> "Rifiutato"
        else -> "Cancellato"
    }

fun getAuthorizationOutcomeV2(baseTransaction: BaseTransaction): AuthorizationResultDto? =
    when (baseTransaction) {
        is BaseTransactionExpired ->
            getAuthorizationOutcomeV2(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithRefundRequested ->
            getAuthorizationOutcomeV2(baseTransaction.transactionAtPreviousState)
        is TransactionWithClosureError ->
            getAuthorizationOutcomeV2(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithCompletedAuthorization -> {
            val gatewayAuthData =
                baseTransaction.transactionAuthorizationCompletedData
                    .transactionGatewayAuthorizationData
            when (gatewayAuthData) {
                is PgsTransactionGatewayAuthorizationData -> gatewayAuthData.authorizationResultDto
                is NpgTransactionGatewayAuthorizationData ->
                    if (gatewayAuthData.operationResult == OperationResultDto.EXECUTED) {
                        AuthorizationResultDto.OK
                    } else {
                        AuthorizationResultDto.KO
                    }
                is RedirectTransactionGatewayAuthorizationData ->
                    if (
                        gatewayAuthData.outcome ==
                            RedirectTransactionGatewayAuthorizationData.Outcome.OK
                    ) {
                        AuthorizationResultDto.OK
                    } else {
                        AuthorizationResultDto.KO
                    }
                else -> null
            }
        }
        else -> null
    }

fun getBrand(authorizationRequestedData: TransactionGatewayAuthorizationRequestedData?) =
    when (authorizationRequestedData) {
        is NpgTransactionGatewayAuthorizationRequestedData -> authorizationRequestedData.brand
        is PgsTransactionGatewayAuthorizationRequestedData ->
            authorizationRequestedData.brand?.toString()
        is RedirectTransactionGatewayAuthorizationRequestedData -> "N/A"
        else -> null
    }

fun getGatewayAuthorizationData(
    transactionGatewayAuthorizationData: TransactionGatewayAuthorizationData?
): GatewayAuthorizationData? {
    return when (transactionGatewayAuthorizationData) {
        is NpgTransactionGatewayAuthorizationData ->
            GatewayAuthorizationData(
                transactionGatewayAuthorizationData.operationResult.value,
                transactionGatewayAuthorizationData.errorCode
            )
        is PgsTransactionGatewayAuthorizationData ->
            GatewayAuthorizationData(
                transactionGatewayAuthorizationData.authorizationResultDto.value,
                transactionGatewayAuthorizationData.errorCode
            )
        is RedirectTransactionGatewayAuthorizationData ->
            GatewayAuthorizationData(
                transactionGatewayAuthorizationData.outcome.name,
                transactionGatewayAuthorizationData.errorCode
            )
        else -> null
    }
}

private fun getAuthorizationOperationId(baseTransaction: BaseTransaction): String? =
    when (baseTransaction) {
        is BaseTransactionExpired ->
            getAuthorizationOperationId(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithClosureError ->
            getAuthorizationOperationId(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithCompletedAuthorization -> {
            val gatewayAuthData =
                baseTransaction.transactionAuthorizationCompletedData
                    .transactionGatewayAuthorizationData
            when (gatewayAuthData) {
                is NpgTransactionGatewayAuthorizationData -> gatewayAuthData.operationId
                else -> null
            }
        }
        is BaseTransactionWithRefundRequested -> {
            val authorizationGatewayData =
                baseTransaction.transactionAuthorizationGatewayData.getOrNull()

            when (authorizationGatewayData) {
                is NpgTransactionGatewayAuthorizationData -> authorizationGatewayData.operationId
                else -> null
            }
        }
        else -> null
    }

private fun getRefundOperationId(baseTransaction: BaseTransaction): String? =
    when (baseTransaction) {
        is BaseTransactionRefunded -> {
            when (
                val gatewayOperationData =
                    baseTransaction.transactionRefundedData.gatewayOperationData
            ) {
                is NpgGatewayRefundData -> gatewayOperationData.operationId
                else -> null
            }
        }
        else -> null
    }

fun buildSearchMetricsResponse(metrics: List<EcommerceStatusCount>): TransactionMetricsResponseDto {
    val responseDto = TransactionMetricsResponseDto()
    val map: Map<String, Int> = metrics.associateBy({ it.status }, { it.count })

    return responseDto
        .ACTIVATED(map.getOrDefault("ACTIVATED", 0))
        .AUTHORIZATION_REQUESTED(map.getOrDefault("AUTHORIZATION_REQUESTED", 0))
        .AUTHORIZATION_COMPLETED(map.getOrDefault("AUTHORIZATION_COMPLETED", 0))
        .CLOSURE_REQUESTED(map.getOrDefault("CLOSURE_REQUESTED", 0))
        .CLOSED(map.getOrDefault("CLOSED", 0))
        .CLOSURE_ERROR(map.getOrDefault("CLOSURE_ERROR", 0))
        .NOTIFIED_OK(map.getOrDefault("NOTIFIED_OK", 0))
        .NOTIFIED_KO(map.getOrDefault("NOTIFIED_KO", 0))
        .NOTIFICATION_ERROR(map.getOrDefault("NOTIFICATION_ERROR", 0))
        .NOTIFICATION_REQUESTED(map.getOrDefault("NOTIFICATION_REQUESTED", 0))
        .EXPIRED(map.getOrDefault("EXPIRED", 0))
        .REFUNDED(map.getOrDefault("REFUNDED", 0))
        .CANCELED(map.getOrDefault("CANCELED", 0))
        .EXPIRED_NOT_AUTHORIZED(map.getOrDefault("EXPIRED_NOT_AUTHORIZED", 0))
        .UNAUTHORIZED(map.getOrDefault("UNAUTHORIZED", 0))
        .REFUND_ERROR(map.getOrDefault("REFUND_ERROR", 0))
        .REFUND_REQUESTED(map.getOrDefault("REFUND_REQUESTED", 0))
        .CANCELLATION_REQUESTED(map.getOrDefault("CANCELLATION_REQUESTED", 0))
        .CANCELLATION_EXPIRED(map.getOrDefault("CANCELLATION_EXPIRED", 0))
}
