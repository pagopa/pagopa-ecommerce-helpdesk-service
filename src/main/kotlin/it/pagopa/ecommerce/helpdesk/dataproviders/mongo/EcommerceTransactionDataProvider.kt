package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.commons.documents.v1.Transaction
import it.pagopa.ecommerce.commons.domain.v1.EmptyTransaction
import it.pagopa.ecommerce.commons.domain.v1.pojos.BaseTransaction
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.baseTransactionToTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Component
class EcommerceTransactionDataProvider(
    @Autowired private val transactionsViewRepository: TransactionsViewRepository,
    @Autowired private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any>,
) : TransactionDataProvider {

    override fun totalRecordCount(searchParams: HelpDeskSearchTransactionRequestDto): Mono<Int> {
        val searchCriteriaType = searchParams.type
        val invalidSearchCriteriaError =
            Mono.error<Int>(
                InvalidSearchCriteriaException(searchCriteriaType, ProductDto.ECOMMERCE)
            )
        return when (searchParams) {
            is SearchTransactionRequestPaymentTokenDto ->
                transactionsViewRepository.countTransactionsWithPaymentToken(
                    searchParams.paymentToken
                )

            is SearchTransactionRequestRptIdDto ->
                transactionsViewRepository.countTransactionsWithRptId(searchParams.rptId)

            is SearchTransactionRequestTransactionIdDto ->
                transactionsViewRepository.existsById(searchParams.transactionId).map { exist ->
                    if (exist) {
                        1
                    } else {
                        0
                    }
                }

            // TODO search by email not implemented yet, here must be changed with search for mail
            // PDV token
            is SearchTransactionRequestEmailDto -> invalidSearchCriteriaError
            is SearchTransactionRequestFiscalCodeDto -> invalidSearchCriteriaError
            else -> invalidSearchCriteriaError
        }.map { it.toInt() }
    }

    override fun findResult(
        searchParams: HelpDeskSearchTransactionRequestDto,
        pageSize: Int,
        pageNumber: Int
    ): Mono<List<TransactionResultDto>> {
        val searchCriteriaType = searchParams.type
        val invalidSearchCriteriaError =
            Flux.error<Transaction>(
                InvalidSearchCriteriaException(searchCriteriaType, ProductDto.ECOMMERCE)
            )
        val pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("creationDate").descending())
        val transactions: Flux<Transaction> = when (searchParams) {
            is SearchTransactionRequestPaymentTokenDto ->
                transactionsViewRepository
                    .findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                        searchParams.paymentToken,
                        pageRequest
                    )

            is SearchTransactionRequestRptIdDto ->
                transactionsViewRepository
                    .findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                        searchParams.rptId,
                        pageRequest
                    )

            is SearchTransactionRequestTransactionIdDto ->
                transactionsViewRepository
                    .findById(searchParams.transactionId)
                    .toFlux()

            is SearchTransactionRequestEmailDto -> invalidSearchCriteriaError
            is SearchTransactionRequestFiscalCodeDto -> invalidSearchCriteriaError
            else -> invalidSearchCriteriaError
        }
        return transactions
            .flatMap { mapToTransactionResultDto(it) }
            .collectList()
    }

    private fun mapToTransactionResultDto(transaction: Transaction): Mono<TransactionResultDto> =
        Mono.just(transaction)
            .flatMapMany {
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transaction.transactionId
                )
            }
            .reduce(
                EmptyTransaction(),
                it.pagopa.ecommerce.commons.domain.v1.Transaction::applyEvent
            )
            .cast(BaseTransaction::class.java)
            .map { baseTransaction -> baseTransactionToTransactionInfoDto(baseTransaction) }
}
