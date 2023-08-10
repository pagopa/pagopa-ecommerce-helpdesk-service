package it.pagopa.ecommerce.helpdesk.utils

import io.r2dbc.spi.Result
import it.pagopa.ecommerce.commons.domain.v1.pojos.BaseTransaction
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.reactivestreams.Publisher

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

fun baseTransactionToTransactionInfoDto(baseTransaction: BaseTransaction): TransactionResultDto =
    TransactionResultDto()
