package it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo

import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.PmTransactionsRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.documents.PmTransactionHistory
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.pmTransactionToTransactionInfoDtoV2
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import it.pagopa.generated.ecommerce.helpdesk.v2.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionResultDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class PmTransactionHistoryDataProvider(
    @Autowired private val pmTransactionsRepository: PmTransactionsRepository
) : TransactionDataProvider {
    override fun totalRecordCount(
        searchParams: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto>
    ): Mono<Int> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMap {
                Mono.error<Int>(InvalidSearchCriteriaException(it.type, ProductDto.PM))
            }
        return decodedSearchParam
            .flatMap {
                when (it) {
                    is SearchTransactionRequestPaymentTokenDto -> invalidSearchCriteriaError
                    is SearchTransactionRequestRptIdDto -> invalidSearchCriteriaError
                    is SearchTransactionRequestTransactionIdDto -> invalidSearchCriteriaError
                    is SearchTransactionRequestEmailDto ->
                        pmTransactionsRepository.countTransactionsWithEmail(it.userEmail)
                    is SearchTransactionRequestFiscalCodeDto ->
                        pmTransactionsRepository.countTransactionsWithUserFiscalCode(
                            it.userFiscalCode
                        )
                    else -> invalidSearchCriteriaError
                }
            }
            .map { it.toInt() }
    }

    override fun findResult(
        searchParams: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto>,
        skip: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMapMany {
                Flux.error<PmTransactionHistory>(
                    InvalidSearchCriteriaException(it.type, ProductDto.PM)
                )
            }
        val transactions: Flux<PmTransactionHistory> =
            decodedSearchParam.flatMapMany {
                when (it) {
                    is SearchTransactionRequestPaymentTokenDto -> invalidSearchCriteriaError
                    is SearchTransactionRequestRptIdDto -> invalidSearchCriteriaError
                    is SearchTransactionRequestTransactionIdDto -> invalidSearchCriteriaError
                    is SearchTransactionRequestEmailDto ->
                        pmTransactionsRepository
                            .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                                email = it.userEmail,
                                skip = skip,
                                limit = limit
                            )
                    is SearchTransactionRequestFiscalCodeDto ->
                        pmTransactionsRepository
                            .findTransactionsWithUserFiscalCodePaginatedOrderByCreationDateDesc(
                                userFiscalCode = it.userFiscalCode,
                                skip = skip,
                                limit = limit
                            )
                    else -> invalidSearchCriteriaError
                }
            }
        return transactions.map { pmTransactionToTransactionInfoDtoV2(it) }.collectList()
    }
}
