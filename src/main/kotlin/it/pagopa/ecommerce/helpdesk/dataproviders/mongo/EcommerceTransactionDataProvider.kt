package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.exceptions.ConfidentialDataException
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.baseTransactionToTransactionInfoDtoV1
import it.pagopa.ecommerce.helpdesk.utils.baseTransactionToTransactionInfoDtoV2
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Component
class EcommerceTransactionDataProvider(
    @Autowired private val transactionsViewRepository: TransactionsViewRepository,
    @Autowired private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any>,
    @Autowired private val emailConfidentialDataManager: ConfidentialDataManager
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
        skip: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>> {
        /**
         * The confidentialMailUtils object is initialized to ensure that the static map stored in
         * memory, which is useful for managing the cache to avoid overloading PDV, does not
         * increase disproportionately .The cache context must terminate for each request
         */
        val confidentialMailUtils =
            ConfidentialMailUtils(emailConfidentialDataManager = emailConfidentialDataManager)
        val searchCriteriaType = searchParams.type
        val invalidSearchCriteriaError =
            Flux.error<BaseTransactionView>(
                InvalidSearchCriteriaException(searchCriteriaType, ProductDto.ECOMMERCE)
            )
        val transactions: Flux<BaseTransactionView> =
            when (searchParams) {
                is SearchTransactionRequestPaymentTokenDto ->
                    transactionsViewRepository
                        .findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                            paymentToken = searchParams.paymentToken,
                            skip = skip,
                            limit = limit
                        )
                is SearchTransactionRequestRptIdDto ->
                    transactionsViewRepository
                        .findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                            rptId = searchParams.rptId,
                            skip = skip,
                            limit = limit
                        )
                is SearchTransactionRequestTransactionIdDto ->
                    transactionsViewRepository.findById(searchParams.transactionId).toFlux()
                is SearchTransactionRequestEmailDto -> invalidSearchCriteriaError
                is SearchTransactionRequestFiscalCodeDto -> invalidSearchCriteriaError
                else -> invalidSearchCriteriaError
            }
        return transactions
            .flatMap { mapToTransactionResultDto(it, confidentialMailUtils) }
            .collectList()
    }

    private fun mapToTransactionResultDto(
        transaction: BaseTransactionView,
        confidentialMailUtils: ConfidentialMailUtils
    ): Mono<TransactionResultDto> {
        val events =
            Mono.just(transaction).flatMapMany {
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transaction.transactionId
                )
            }

        return when (transaction) {
            is it.pagopa.ecommerce.commons.documents.v1.Transaction ->
                events
                    .reduce(
                        it.pagopa.ecommerce.commons.domain.v1.EmptyTransaction(),
                        it.pagopa.ecommerce.commons.domain.v1.Transaction::applyEvent
                    )
                    .cast(it.pagopa.ecommerce.commons.domain.v1.pojos.BaseTransaction::class.java)
                    .zipWhen(
                        { baseTransaction ->
                            confidentialMailUtils
                                .toEmail(baseTransaction.email)
                                .map { Optional.of(it) }
                                .onErrorResume(ConfidentialDataException::class.java) {
                                    it.statusCode
                                        .filter { status -> status == HttpStatus.NOT_FOUND }
                                        .map { Mono.just(Optional.empty<Email>()) }
                                        .orElse(Mono.error(it))
                                }
                        },
                        ::Pair
                    )
                    .map { (baseTransaction, email) ->
                        baseTransactionToTransactionInfoDtoV1(baseTransaction, email)
                    }
            is it.pagopa.ecommerce.commons.documents.v2.Transaction ->
                events
                    .reduce(
                        it.pagopa.ecommerce.commons.domain.v2.EmptyTransaction(),
                        it.pagopa.ecommerce.commons.domain.v2.Transaction::applyEvent
                    )
                    .cast(it.pagopa.ecommerce.commons.domain.v2.pojos.BaseTransaction::class.java)
                    .zipWhen(
                        { baseTransaction ->
                            confidentialMailUtils
                                .toEmail(baseTransaction.email)
                                .map { Optional.of(it) }
                                .onErrorResume(ConfidentialDataException::class.java) {
                                    it.statusCode
                                        .filter { status -> status == HttpStatus.NOT_FOUND }
                                        .map { Mono.just(Optional.empty<Email>()) }
                                        .orElse(Mono.error(it))
                                }
                        },
                        ::Pair
                    )
                    .map { (baseTransaction, email) ->
                        baseTransactionToTransactionInfoDtoV2(baseTransaction, email)
                    }
            else ->
                Mono.error(
                    RuntimeException(
                        "inconsistent state for the transaction ${transaction.transactionId}!"
                    )
                )
        }
    }
}
