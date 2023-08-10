package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.commons.documents.v1.Transaction
import it.pagopa.ecommerce.commons.domain.v1.EmptyTransaction
import it.pagopa.ecommerce.commons.domain.v1.pojos.BaseTransaction
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.baseTransactionToTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Component
class EcommerceTransactionDataProvider(
    @Autowired private val transactionsViewRepository: TransactionsViewRepository,
    @Autowired private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any>,
) : TransactionDataProvider {

    override fun totalRecordCount(searchCriteria: HelpDeskSearchTransactionRequestDto): Mono<Long> =
        when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto -> transactionsViewRepository.countTransactionsWithPaymentToken(
                searchCriteria.paymentToken
            )

            is SearchTransactionRequestRptIdDto -> transactionsViewRepository.countTransactionsWithRptId(searchCriteria.rptId)
            is SearchTransactionRequestTransactionIdDto -> transactionsViewRepository.existsById(searchCriteria.transactionId)
                .map { exist ->
                    if (exist) {
                        1
                    } else {
                        0
                    }
                }

            //search by email not implemented yet, here must be changed with search for mail PDV token
            is SearchTransactionRequestEmailDto -> Mono.just(0)
            is SearchTransactionRequestFiscalCodeDto -> Mono.just(0)
            else ->
                Mono.error(
                    RuntimeException("Unhandled search criteria ${searchCriteria.javaClass}")
                )
        }

    override fun findResult(
        searchCriteria: HelpDeskSearchTransactionRequestDto,
        pageSize: Int,
        pageNumber: Int
    ): Mono<List<TransactionResultDto>> {
        val searchCriteriaType =
            TransactionDataProvider.SearchTypeMapping.getSearchType(searchCriteria.javaClass)
        val invalidSearchCriteriaError =
            Mono.error<List<TransactionResultDto>>(
                InvalidSearchCriteriaException(searchCriteriaType, ProductDto.ECOMMERCE)
            )
        return when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto ->
                transactionsViewRepository.findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                    searchCriteria.paymentToken,
                    Pageable.unpaged()
                )
                    .flatMap { mapToTransactionResultDto(it) }
                    .collectList()

            is SearchTransactionRequestRptIdDto ->
                transactionsViewRepository.findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                    searchCriteria.rptId,
                    Pageable.unpaged()
                )
                    .flatMap { mapToTransactionResultDto(it) }
                    .collectList()

            is SearchTransactionRequestTransactionIdDto ->
                transactionsViewRepository.findById(searchCriteria.transactionId)
                    .toFlux()
                    .flatMap { mapToTransactionResultDto(it) }
                    .collectList()

            is SearchTransactionRequestEmailDto -> invalidSearchCriteriaError
            is SearchTransactionRequestFiscalCodeDto -> invalidSearchCriteriaError
            else -> invalidSearchCriteriaError
        }
    }

    fun mapToTransactionResultDto(transaction: Transaction): Mono<TransactionResultDto> =
        Mono.just(transaction)
            .flatMapMany { transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(transaction.transactionId) }
            .reduce(EmptyTransaction(), it.pagopa.ecommerce.commons.domain.v1.Transaction::applyEvent)
            .cast(BaseTransaction::class.java)
            .map { baseTransaction -> baseTransactionToTransactionInfoDto(baseTransaction) }
}
