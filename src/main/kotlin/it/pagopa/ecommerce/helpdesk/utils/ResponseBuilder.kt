package it.pagopa.ecommerce.helpdesk.utils

import io.r2dbc.spi.Result
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
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
                    .status(row[5, String::class.java])
            )
            .transactionInfo(
                TransactionInfoDto()
                    .creationDate(
                        OffsetDateTime.ofInstant(
                            row[6, Timestamp::class.java]?.let { Instant.ofEpochMilli(it.time) },
                            ZoneId.of("UTC")
                        )
                    )
                    .status(row[8, String::class.java]) // TODO handle status here
                    .amount(row[9, Integer::class.java]?.toInt())
                    .fee(row[10, Integer::class.java]?.toInt())
                    .grandTotal(row[11, Integer::class.java]?.toInt())
            )
            .paymentInfo(
                PaymentInfoDto()
                    .amount(row[14, Integer::class.java]?.toInt())
                    .subject(row[15, String::class.java])
                    .origin(row[16, String::class.java])
            )
            .paymentDetailInfo(
                PaymentDetailInfoDto()
                    .iuv(row[17, String::class.java])
                    .paymentContextCode(row[18, String::class.java])
                    .creditorInstitution(row[19, String::class.java])
                    .amount(row[20, Integer::class.java]?.toInt())
                    .paFiscalCode(row[21, String::class.java])
            )
            .pspInfo(
                PspInfoDto()
                    .pspId(row[22, String::class.java])
                    .businessName(row[23, String::class.java])
                    .idChannel(row[24, String::class.java])
            )
            .product(ProductDto.PM)
    }
