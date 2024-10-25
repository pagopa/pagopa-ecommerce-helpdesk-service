package it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo

import it.pagopa.ecommerce.helpdesk.dataproviders.PmTransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.documents.PmTransaction
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.pmTransactionToTransactionInfoDtoV2
import it.pagopa.generated.ecommerce.helpdesk.model.*
import it.pagopa.generated.ecommerce.helpdesk.v2.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionResultDto
import org.springframework.beans.factory.annotation.Autowired
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class PmDataProvider(@Autowired private val pmTransactionsViewRepository: PmTransactionsViewRepository): TransactionDataProvider {
    override fun totalRecordCount(searchParams: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto>): Mono<Int> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError = decodedSearchParam.flatMap {
            Mono.error<Int>(InvalidSearchCriteriaException(it.type, ProductDto.PM))
        }
        return decodedSearchParam.flatMap {
            when(it) {
                is SearchTransactionRequestPaymentTokenDto -> invalidSearchCriteriaError
                is SearchTransactionRequestRptIdDto -> invalidSearchCriteriaError
                is SearchTransactionRequestTransactionIdDto -> invalidSearchCriteriaError
                is SearchTransactionRequestEmailDto -> pmTransactionsViewRepository.countTransactionsWithEmail(it.userEmail)
                is SearchTransactionRequestFiscalCodeDto -> pmTransactionsViewRepository.countTransactionsWithUserFiscalCode(it.userFiscalCode)
                else -> invalidSearchCriteriaError
            }
        }.map { it.toInt() }
    }

    override fun findResult(
        searchParams: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto>,
        skip: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMapMany {
                Flux.error<PmTransaction>(
                    InvalidSearchCriteriaException(it.type, ProductDto.PM)
                )
            }
        val transactions: Flux<PmTransaction> =
            decodedSearchParam.flatMapMany {
                when(it) {
                    is SearchTransactionRequestPaymentTokenDto ->
                        invalidSearchCriteriaError
                    is SearchTransactionRequestRptIdDto ->
                        invalidSearchCriteriaError
                    is SearchTransactionRequestTransactionIdDto ->
                        invalidSearchCriteriaError
                    is SearchTransactionRequestEmailDto ->
                        pmTransactionsViewRepository
                            .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                                email = it.userEmail,
                                skip = skip,
                                limit = limit
                            )
                    is SearchTransactionRequestFiscalCodeDto -> pmTransactionsViewRepository
                        .findTransactionsWithUserFiscalCodePaginatedOrderByCreationDateDesc(
                            userFiscalCode = it.userFiscalCode,
                            skip = skip,
                            limit = limit
                        )
                    else -> invalidSearchCriteriaError
                }
            }
        return transactions
            .map { pmTransactionToTransactionInfoDtoV2(it) }
            .collectList()
    }
}