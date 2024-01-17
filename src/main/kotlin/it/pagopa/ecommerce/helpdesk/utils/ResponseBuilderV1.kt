package it.pagopa.ecommerce.helpdesk.utils

import io.r2dbc.spi.Result
import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationCompletedData
import it.pagopa.ecommerce.commons.documents.v1.TransactionAuthorizationRequestData
import it.pagopa.ecommerce.commons.documents.v1.TransactionUserReceiptData
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.domain.v1.TransactionWithClosureError
import it.pagopa.ecommerce.commons.domain.v1.pojos.*
import it.pagopa.ecommerce.commons.generated.server.model.AuthorizationResultDto
import it.pagopa.ecommerce.commons.utils.v1.TransactionUtils.getTransactionFee
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.model.*
import it.pagopa.generated.ecommerce.nodo.v2.model.UserDto
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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

fun resultToPaymentMethodDtoList(
    results: List<Result>,
    searchType: String
): Mono<SearchPaymentMethodResponseDto> =
    Flux.fromIterable(results)
        .flatMap { result ->
            result.map { row ->
                val paymentMethod =
                    when {
                        row[10, BigDecimal::class.java] != null -> { // FK_CREDIT_CARD
                            CardDetailInfoDto()
                                .type(DetailTypeDto.CARD.value)
                                .creationDate(
                                    row[7, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                                )
                                .idPsp(row[8, String::class.java])
                                .cardBin(row[11, String::class.java])
                                .cardNumber(row[12, String::class.java])
                        }
                        row[13, BigDecimal::class.java] != null -> { // FK_BUYER_BANK
                            BankAccountDetailInfoDto()
                                .type(DetailTypeDto.BANK_ACCOUNT.value)
                                .creationDate(
                                    row[7, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                                )
                                .bankName(row[14, String::class.java])
                                .bankState(row[15, String::class.java])
                        }
                        row[16, BigDecimal::class.java] != null -> { // FK_BANCOMAT_CARD
                            BancomatDetailInfoDto()
                                .type(DetailTypeDto.BANCOMAT.value)
                                .creationDate(
                                    row[7, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                                )
                                .bancomatAbi(row[17, String::class.java])
                                .bancomatNumber(row[18, String::class.java])
                        }
                        row[19, BigDecimal::class.java] != null -> { // FK_SATISPAY
                            SatispayDetailInfoDto()
                                .type(DetailTypeDto.SATISPAY.value)
                                .creationDate(
                                    row[7, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                                )
                                .idPsp(row[8, String::class.java])
                        }
                        row[20, BigDecimal::class.java] != null -> { // FK_BPAY
                            BpayDetailInfoDto()
                                .type(DetailTypeDto.BPAY.value)
                                .creationDate(
                                    row[7, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                                )
                                .idPsp(row[8, String::class.java])
                                .bpayName(row[21, String::class.java])
                                .bpayPhoneNumber(row[22, String::class.java])
                        }
                        row[23, BigDecimal::class.java] != null -> { // FK_GENERIC_INSTRUMENT
                            GenericMethodDetailInfoDto()
                                .type(DetailTypeDto.GENERIC_METHOD.value)
                                .creationDate(
                                    row[7, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                                )
                                .description((row[24, String::class.java]))
                        }
                        row[25, BigDecimal::class.java] != null -> { // FK_PPAL
                            PaypalDetailInfoDto()
                                .type(DetailTypeDto.PAYPAL.value)
                                .creationDate(
                                    row[7, LocalDateTime::class.java]?.atOffset(ZoneOffset.of("+2"))
                                )
                                .ppayEmail((row[26, String::class.java]))
                        }
                        else -> {
                            ResponseBuilderV1Logger.logger.warn("Payment method not handled")
                            null
                        }
                    }
                val searchPaymentMethodResponse =
                    SearchPaymentMethodResponseDto()
                        .fiscalCode(row[0, String::class.java])
                        .notificationEmail(row[1, String::class.java])
                        .surname(row[2, String::class.java])
                        .name(row[3, String::class.java])
                        .username(row[4, String::class.java])
                        .status(row[5, String::class.java])
                if (paymentMethod != null) {
                    searchPaymentMethodResponse.addPaymentMethodsItem(paymentMethod)
                }
                searchPaymentMethodResponse
            }
        }
        .switchIfEmpty(Mono.error(NoResultFoundException(searchType)))
        .reduce(SearchPaymentMethodResponseDto()) { searchResponse, mappedRow ->
            searchResponse.fiscalCode(mappedRow.fiscalCode)
            searchResponse.notificationEmail(mappedRow.notificationEmail)
            searchResponse.name(mappedRow.name)
            searchResponse.surname(mappedRow.surname)
            searchResponse.username(mappedRow.username)
            searchResponse.status(mappedRow.status)
            searchResponse.addPaymentMethodsItem(mappedRow.paymentMethods[0])
        }
        .map { response ->
            response.paymentMethods?.sortBy { it.type }
            response
        }

fun baseTransactionToTransactionInfoDtoV1(
    baseTransaction: BaseTransaction,
    email: Optional<Email>
): TransactionResultDto {
    val amount = baseTransaction.paymentNotices.sumOf { it.transactionAmount.value }
    val fee = getTransactionFees(baseTransaction).orElse(0)
    val totalAmount = amount.plus(fee)
    val transactionAuthorizationRequestData = getTransactionAuthRequestedData(baseTransaction)
    val transactionAuthorizationCompletedData = getTransactionAuthCompletedData(baseTransaction)
    val transactionUserReceiptData = getTransactionUserReceiptData(baseTransaction)

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

fun buildDeadLetterEventsSearchResponse(
    currentPage: Int,
    totalCount: Int,
    pageSize: Int,
    results: List<DeadLetterEventDto>
): SearchDeadLetterEventResponseDto =
    SearchDeadLetterEventResponseDto()
        .page(
            PageInfoDto()
                .current(currentPage)
                .total(calculateTotalPages(totalCount = totalCount, pageSize = pageSize))
                .results(results.size)
        )
        .deadLetterEvents(results)

private fun calculateTotalPages(totalCount: Int, pageSize: Int) =
    if (totalCount % pageSize == 0) {
        totalCount / pageSize
    } else {
        totalCount / pageSize + 1
    }
