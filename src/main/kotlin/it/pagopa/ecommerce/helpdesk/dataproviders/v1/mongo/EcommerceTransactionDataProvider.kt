package it.pagopa.ecommerce.helpdesk.dataproviders.v1.mongo

import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.exceptions.ConfidentialDataException
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager.ConfidentialData
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsEventStoreRepository as TransactionsEventStoreHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsViewRepository as TransactionsViewHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.v1.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v1.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.v1.baseTransactionToTransactionInfoDtoV1
import it.pagopa.ecommerce.helpdesk.utils.v1.baseTransactionToTransactionInfoDtoV2
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestEmailDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestFiscalCodeDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestPaymentTokenDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestRptIdDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestTransactionIdDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Component
class EcommerceTransactionDataProvider(
    @Autowired private val transactionsViewRepository: TransactionsViewRepository,
    @Autowired private val transactionsViewHistoryRepository: TransactionsViewHistoryRepository,
    @Autowired private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any>,
    @Autowired
    private val transactionsEventStoreHistoryRepository:
        TransactionsEventStoreHistoryRepository<Any>,
) : TransactionDataProvider {

    override fun totalRecordCount(
        searchParams: SearchParamDecoder<HelpDeskSearchTransactionRequestDto>
    ): Mono<Int> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMap {
                Mono.error<Long>(InvalidSearchCriteriaException(it.type, ProductDto.ECOMMERCE))
            }
        return decodedSearchParam
            .flatMap {
                when (it) {
                    is SearchTransactionRequestPaymentTokenDto ->
                        Mono.zip(
                                transactionsViewRepository.countTransactionsWithPaymentToken(
                                    it.paymentToken
                                ),
                                transactionsViewHistoryRepository.countTransactionsWithPaymentToken(
                                    it.paymentToken
                                )
                            )
                            .map { it.t1 + it.t2 }
                    is SearchTransactionRequestRptIdDto ->
                        Mono.zip(
                                transactionsViewRepository.countTransactionsWithRptId(it.rptId),
                                transactionsViewHistoryRepository.countTransactionsWithRptId(
                                    it.rptId
                                )
                            )
                            .map { it.t1 + it.t2 }
                    is SearchTransactionRequestTransactionIdDto ->
                        transactionsViewRepository.existsById(it.transactionId).map { exist ->
                            if (exist) {
                                1
                            } else {
                                0
                            }
                        }
                    is SearchTransactionRequestEmailDto ->
                        Mono.zip(
                                transactionsViewRepository.countTransactionsWithEmail(it.userEmail),
                                transactionsViewHistoryRepository.countTransactionsWithEmail(
                                    it.userEmail
                                )
                            )
                            .map { it.t1 + it.t2 }
                    is SearchTransactionRequestFiscalCodeDto -> invalidSearchCriteriaError
                    else -> invalidSearchCriteriaError
                }
            }
            .map { it.toInt() }
    }

    override fun findResult(
        searchParams: SearchParamDecoder<HelpDeskSearchTransactionRequestDto>,
        skip: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMapMany {
                Flux.error<BaseTransactionView>(
                    InvalidSearchCriteriaException(it.type, ProductDto.ECOMMERCE)
                )
            }

        val transactions: Flux<BaseTransactionView> =
            decodedSearchParam.flatMapMany {
                when (it) {
                    is SearchTransactionRequestPaymentTokenDto ->
                        Flux.concat(
                            transactionsViewRepository
                                .findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                                    paymentToken = it.paymentToken,
                                    skip = skip,
                                    limit = limit
                                ),
                            transactionsViewHistoryRepository
                                .findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                                    paymentToken = it.paymentToken,
                                    skip = skip,
                                    limit = limit
                                )
                        )
                    is SearchTransactionRequestRptIdDto ->
                        Flux.concat(
                            transactionsViewRepository
                                .findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                                    rptId = it.rptId,
                                    skip = skip,
                                    limit = limit
                                ),
                            transactionsViewHistoryRepository
                                .findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                                    rptId = it.rptId,
                                    skip = skip,
                                    limit = limit
                                )
                        )
                    is SearchTransactionRequestTransactionIdDto ->
                        Flux.concat (
                            transactionsViewRepository.findById(it.transactionId).toFlux(),
                            transactionsViewHistoryRepository.findById(it.transactionId).toFlux(),
                        )
                    is SearchTransactionRequestEmailDto ->
                        Flux.concat(
                            transactionsViewRepository
                                .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                                    encryptedEmail = it.userEmail,
                                    skip = skip,
                                    limit = limit
                                ),
                            transactionsViewHistoryRepository
                                .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                                    encryptedEmail = it.userEmail,
                                    skip = skip,
                                    limit = limit
                                )
                        )
                    is SearchTransactionRequestFiscalCodeDto -> invalidSearchCriteriaError
                    else -> invalidSearchCriteriaError
                }
            }

        return transactions
            .flatMap { mapToTransactionResultDto(it, searchParams.confidentialMailUtils!!) }
            .collectList()
    }

    private fun mapToTransactionResultDto(
        transaction: BaseTransactionView,
        confidentialMailUtils: ConfidentialMailUtils
    ): Mono<TransactionResultDto> {
        val events =
            Mono.just(transaction).flatMapMany {
                Flux.merge(transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transaction.transactionId
                ),
                transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                    transaction.transactionId
                ))
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
                                .toClearData(
                                    baseTransaction.email as Confidential<ConfidentialData>
                                )
                                .map { Optional.of(it) }
                                .onErrorResume(ConfidentialDataException::class.java) {
                                    val errorCause = it.cause
                                    if (
                                        errorCause is WebClientResponseException &&
                                            errorCause.statusCode.value() == 404
                                    ) {
                                        Mono.just(Optional.empty())
                                    } else {
                                        Mono.error(it)
                                    }
                                }
                        },
                        ::Pair
                    )
                    .map { (baseTransaction, email) ->
                        baseTransactionToTransactionInfoDtoV1(
                            baseTransaction,
                            email.map { it as ConfidentialData }
                        )
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
                                .toClearData(
                                    baseTransaction.email as Confidential<ConfidentialData>
                                )
                                .map { Optional.of(it) }
                                .onErrorResume(ConfidentialDataException::class.java) {
                                    val errorCause = it.cause
                                    if (
                                        errorCause is WebClientResponseException &&
                                            errorCause.statusCode.value() == 404
                                    ) {
                                        Mono.just(Optional.empty())
                                    } else {
                                        Mono.error(it)
                                    }
                                }
                        },
                        ::Pair
                    )
                    .map { (baseTransaction, email) ->
                        baseTransactionToTransactionInfoDtoV2(
                            baseTransaction,
                            email.map { it as ConfidentialData }
                        )
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
