package it.pagopa.ecommerce.helpdesk.utils

import io.r2dbc.spi.Result
import it.pagopa.ecommerce.commons.domain.v1.pojos.*
import it.pagopa.generated.ecommerce.helpdesk.model.*
import it.pagopa.generated.ecommerce.nodo.v2.model.UserDto
import org.reactivestreams.Publisher
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

fun buildTransactionSearchResponse(
    currentPage: Int,
    totalCount: Int,
    results: List<TransactionResultDto>
): SearchTransactionResponseDto =
    SearchTransactionResponseDto()
        .page(PageInfoDto().current(currentPage).total(totalCount).results(results.size))
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
                    .authotizationCode(row[14, String::class.java])
                    .paymentMethodName(row[15, String::class.java])
                    .brand(null)
            )
            .paymentInfo(
                PaymentInfoDto()
                    .subject(row[16, String::class.java])
                    .origin(row[9, String::class.java])
            )
            .paymentDetailInfo(
                PaymentDetailInfoDto()
                    .iuv(row[17, String::class.java])
                    .rptIds(null)
                    .idTransaction(row[18, String::class.java])
                    .paymentToken(null)
                    .creditorInstitution(row[19, String::class.java])
                    .paFiscalCode(row[20, String::class.java])
            )
            .pspInfo(
                PspInfoDto()
                    .pspId(row[21, String::class.java])
                    .businessName(row[22, String::class.java])
                    .idChannel(row[23, String::class.java])
            )
            .product(ProductDto.PM)
    }

fun baseTransactionToTransactionInfoDto(baseTransaction: BaseTransaction): TransactionResultDto {
    val userInfo =
        UserInfoDto()
            .notificationEmail("") // TODO to be valued here with PDV integration
            // TODO this field is statically valued with GUEST eCommerce side into Nodo ClosePayment
            // requests.
            // Must be populated dinamically when logic will be updated eCommerce side
            // (event-dispatcher/transactions-service)
            .authenticationType(UserDto.TypeEnum.GUEST.toString())
    val transactionInfo =
        TransactionInfoDto()
            .creationDate(baseTransaction.creationDate.toOffsetDateTime())
            .status("")
    val paymentInfo =
        PaymentInfoDto()
            .origin(baseTransaction.clientId.toString())
            // TODO make also this object a list?
            .subject(baseTransaction.paymentNotices[0].transactionDescription.value)
    val paymentDetailInfoDto =
        baseTransaction.paymentNotices.map {
            PaymentDetailInfoDto()
                .rptIds(listOf(it.rptId.value()))
                .idTransaction(baseTransaction.transactionId.value())
                .paymentToken(it.paymentToken.value)
                // TODO here take the first from the transfer list?
                .paFiscalCode(it.transferList[0].paFiscalCode)
        }
    when (baseTransaction) {
        is BaseTransactionWithRefundRequested ->
            baseTransactionToTransactionInfoDto(baseTransaction.transactionAtPreviousState)

        is BaseTransactionExpired ->
            baseTransactionToTransactionInfoDto(baseTransaction.transactionAtPreviousState)

        is BaseTransactionWithClosureError ->
            baseTransactionToTransactionInfoDto(baseTransaction.transactionAtPreviousState)

        is BaseTransactionWithRequestedUserReceipt -> {
            paymentDetailInfoDto.forEach {
                it.creditorInstitution(
                    baseTransaction.transactionUserReceiptData.receivingOfficeName
                )
            }
        }

        is BaseTransactionWithCompletedAuthorization -> null
        is BaseTransactionWithRequestedAuthorization -> null
        is BaseTransactionWithCancellationRequested -> null
        is BaseTransactionWithPaymentToken -> null
    }

    return TransactionResultDto()
        .product(ProductDto.ECOMMERCE)
        .userInfo(userInfo)
        .transactionInfo(transactionInfo)
        .paymentInfo(paymentInfo)
        .paymentDetailInfo(paymentDetailInfoDto[0])
}
