package it.pagopa.ecommerce.helpdesk.utils

import io.r2dbc.spi.Result
import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationCompletedData
import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v1.TransactionUserReceiptData
import it.pagopa.ecommerce.commons.domain.v1.TransactionWithClosureError
import it.pagopa.ecommerce.commons.domain.v1.pojos.*
import it.pagopa.ecommerce.commons.generated.server.model.AuthorizationResultDto
import it.pagopa.ecommerce.commons.utils.v1.TransactionUtils.getTransactionFee
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.PMPaymentMethodsDataProvider.Companion.PAYPAL_TYPE
import it.pagopa.generated.ecommerce.helpdesk.model.*
import it.pagopa.generated.ecommerce.nodo.v2.model.UserDto
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import org.reactivestreams.Publisher

fun buildTransactionSearchResponse(
    currentPage: Int,
    totalCount: Int,
    pageSize: Int,
    results: List<TransactionResultDto>
): SearchTransactionResponseDto {
    val totalPages =
        if (totalCount % pageSize == 0) {
            totalCount / pageSize
        } else {
            totalCount / pageSize + 1
        }
    return SearchTransactionResponseDto()
        .page(PageInfoDto().current(currentPage).total(totalPages).results(results.size))
        .transactions(results)
}

fun resultToTransactionInfoDto(result: Result): Publisher<TransactionResultDto> =
    result.map { row ->
        TransactionResultDto()
            .userInfo(
                UserInfoDto()
                    .userFiscalCode(row[0, String::class.java])
                    .notificationEmail(row[1, String::class.java])
                    .surname(row[2, String::class.java])
                    .name(row[3, String::class.java])
                    .username(row[4, String::class.java])
                    .authenticationType(row[5, String::class.java])
            )
            .transactionInfo(
                TransactionInfoDto()
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
                PaymentInfoDto()
                    .origin(row[9, String::class.java])
                    .details(
                        listOf(
                            PaymentDetailInfoDto()
                                .subject(row[16, String::class.java])
                                .iuv(row[17, String::class.java])
                                .rptId(null)
                                .idTransaction(row[18, String::class.java])
                                .paymentToken(null)
                                .creditorInstitution(row[19, String::class.java])
                                .paFiscalCode(row[20, String::class.java])
                        )
                    )
            )
            .pspInfo(
                PspInfoDto()
                    .pspId(row[21, String::class.java])
                    .businessName(row[22, String::class.java])
                    .idChannel(row[23, String::class.java])
            )
            .product(ProductDto.PM)
    }

fun resultToPaymentMethodDtoList(result: Result): Publisher<SearchPaymentMethodResponseDto> =
    result.map { row ->
        SearchPaymentMethodResponseDto()
            .fiscalCode(row[0, String::class.java])
            .notificationEmail(row[1, String::class.java])
            .surname(row[2, String::class.java])
            .name(row[3, String::class.java])
            .username(row[4, String::class.java])
            .status(row[5, String::class.java])
            .paymentMethods(
                listOf(
                    if (row[6, String::class.java] != null) { // FK_CREDIT_CARD
                        CardDetailInfoDto()
                            .type(DetailTypeDto.CARD.value)
                            .creationDate(
                                row[14, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                            )
                            .idPsp(row[15, String::class.java])
                            .businessName(row[16, String::class.java])
                            .cardBin(row[17, String::class.java])
                            .cardNumber(row[18, String::class.java])
                    } else if (row[7, String::class.java] != null) { // FK_BUYER_BANK
                        BankAccountDetailInfoDto()
                            .type(DetailTypeDto.BANK_ACCOUNT.value)
                            .creationDate(
                                row[14, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                            )
                            .bankName(row[19, String::class.java])
                            .bankState(row[20, String::class.java])
                    } else if (row[8, String::class.java] != null) { // FK_BANCOMAT_CARD
                        BancomatDetailInfoDto()
                            .type(DetailTypeDto.BANCOMAT.value)
                            .creationDate(
                                row[14, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                            )
                            .bancomatAbi(row[21, String::class.java])
                            .bancomatNumber(row[22, String::class.java])
                    } else if (row[9, String::class.java] != null) { // FK_SATISPAY
                        SatispayDetailInfoDto()
                            .type(DetailTypeDto.SATISPAY.value)
                            .creationDate(
                                row[14, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                            )
                            .idPsp(row[15, String::class.java])
                            .businessName(row[16, String::class.java])
                            .uidSatispay(row[23, String::class.java])
                    } else if (row[10, String::class.java] != null) { // FK_BPAY
                        BpayDetailInfoDto()
                            .type(DetailTypeDto.BPAY.value)
                            .creationDate(
                                row[14, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                            )
                            .idPsp(row[15, String::class.java])
                            .businessName(row[16, String::class.java])
                            .bpayName(row[24, String::class.java])
                            .bpayPhoneNumber(row[25, String::class.java])
                    } else if (row[11, String::class.java] != null) { // FK_GENERIC_INSTRUMENT
                        GenericMethodDetailInfoDto()
                            .type(DetailTypeDto.GENERIC_METHOD.value)
                            .creationDate(
                                row[14, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                            )
                            .description((row[26, String::class.java]))
                    } else if ((PAYPAL_TYPE == (row[12, Long::class.java]))) { // PAYPAL
                        PaypalDetailInfoDto()
                            .type(DetailTypeDto.PAYPAL.value)
                            .creationDate(
                                row[14, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                            )
                            .ppayEmail((row[28, String::class.java]))
                    } else {
                        null
                    }
                )
            )
    }

fun baseTransactionToTransactionInfoDto(baseTransaction: BaseTransaction): TransactionResultDto {
    val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
    val fee = getTransactionFees(baseTransaction).orElse(0)
    val totalAmount = amount.plus(fee)
    val transactionAuthorizationRequestData = getTransactionAuthRequestedData(baseTransaction)
    val transactionAuthorizationCompletedData = getTransactionAuthCompletedData(baseTransaction)
    val transactionUserReceiptData = getTransactionUserReceiptData(baseTransaction)

    // Build user info

    val userInfo =
        UserInfoDto()
            // TODO to be valued here with PDV integration
            .notificationEmail("")
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
            .eventStatus(TransactionStatusDto.valueOf(baseTransaction.status.toString()))
            .amount(amount)
            .fee(fee)
            .grandTotal(totalAmount)
            .rrn(transactionAuthorizationCompletedData?.rrn)
            .authorizationCode(transactionAuthorizationCompletedData?.authorizationCode)
            .paymentMethodName(transactionAuthorizationRequestData?.paymentMethodName)
            .brand(transactionAuthorizationRequestData?.brand?.toString())
    // build payment info
    val paymentInfo =
        PaymentInfoDto()
            .origin(baseTransaction.clientId.toString())
            .details(
                baseTransaction.paymentNotices.map {
                    PaymentDetailInfoDto()
                        .subject(it.transactionDescription.value)
                        .rptId(it.rptId.value)
                        .idTransaction(baseTransaction.transactionId.value())
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
    when (getAuthorizationOutcome(baseTransaction)) {
        AuthorizationResultDto.OK -> "Confermato"
        AuthorizationResultDto.KO -> "Rifiutato"
        else -> "Cancellato"
    }

fun getAuthorizationOutcome(baseTransaction: BaseTransaction): AuthorizationResultDto? =
    when (baseTransaction) {
        is BaseTransactionExpired ->
            getAuthorizationOutcome(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithRefundRequested ->
            getAuthorizationOutcome(baseTransaction.transactionAtPreviousState)
        is TransactionWithClosureError ->
            getAuthorizationOutcome(baseTransaction.transactionAtPreviousState)
        is BaseTransactionWithCompletedAuthorization ->
            baseTransaction.transactionAuthorizationCompletedData.authorizationResultDto
        else -> null
    }
