package it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.exceptions.ConfidentialDataException
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager.ConfidentialData
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsEventStoreRepository as TransactionsEventStoreHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsViewRepository as TransactionsViewHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.v2.ConfidentialMailUtils
import it.pagopa.ecommerce.helpdesk.utils.v2.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.baseTransactionToTransactionInfoDtoV1
import it.pagopa.ecommerce.helpdesk.utils.v2.baseTransactionToTransactionInfoDtoV2
import it.pagopa.generated.ecommerce.helpdesk.v2.model.*
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Component("EcommerceTransactionDataProviderV2")
class EcommerceTransactionDataProvider(
    @Autowired private val transactionsViewRepository: TransactionsViewRepository,
    @Autowired private val transactionsViewHistoryRepository: TransactionsViewHistoryRepository,
    @Autowired private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any>,
    @Autowired
    private val transactionsEventStoreHistoryRepository:
        TransactionsEventStoreHistoryRepository<Any>
) : TransactionDataProvider {

    override fun totalRecordCount(
        searchParams: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto>
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
                                ),
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
                        Mono.zip(
                                transactionsViewRepository.existsById(it.transactionId).map { exist
                                    ->
                                    if (exist) {
                                        1
                                    } else {
                                        0
                                    }
                                },
                                transactionsViewHistoryRepository
                                    .existsById(it.transactionId)
                                    .map { exist ->
                                        if (exist) {
                                            1
                                        } else {
                                            0
                                        }
                                    }
                            )
                            .map { it.t1 + it.t2 }
                    is SearchTransactionRequestEmailDto ->
                        Mono.zip(
                                transactionsViewRepository.countTransactionsWithEmail(it.userEmail),
                                transactionsViewHistoryRepository.countTransactionsWithEmail(
                                    it.userEmail
                                )
                            )
                            .map { it.t1 + it.t2 }
                    is SearchTransactionRequestFiscalCodeDto ->
                        Mono.zip(
                                transactionsViewRepository.countTransactionsWithFiscalCode(
                                    it.userFiscalCode
                                ),
                                transactionsViewHistoryRepository.countTransactionsWithFiscalCode(
                                    it.userFiscalCode
                                )
                            )
                            .map { it.t1 + it.t2 }
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
                Flux.error<BaseTransactionView>(
                    InvalidSearchCriteriaException(it.type, ProductDto.ECOMMERCE)
                )
            }

        val transactions: Flux<BaseTransactionView> =
            decodedSearchParam.flatMapMany {
                when (it) {
                    is SearchTransactionRequestPaymentTokenDto ->
                        Flux.merge(
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
                        Flux.merge(
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
                        Flux.merge(
                            transactionsViewRepository.findById(it.transactionId).toFlux(),
                            transactionsViewHistoryRepository.findById(it.transactionId).toFlux()
                        )
                    is SearchTransactionRequestEmailDto ->
                        Flux.merge(
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
                    is SearchTransactionRequestFiscalCodeDto ->
                        Flux.merge(
                            transactionsViewRepository
                                .findTransactionsWithFiscalCodePaginatedOrderByCreationDateDesc(
                                    encryptedFiscalCode = it.userFiscalCode,
                                    skip = skip,
                                    limit = limit
                                ),
                            transactionsViewHistoryRepository
                                .findTransactionsWithFiscalCodePaginatedOrderByCreationDateDesc(
                                    encryptedFiscalCode = it.userFiscalCode,
                                    skip = skip,
                                    limit = limit
                                )
                        )
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
        val events: Flux<BaseTransactionEvent<Any>> =
            Flux.concat(
                transactionsEventStoreRepository.findByTransactionIdOrderByCreationDateAsc(
                    transaction.transactionId
                ),
                transactionsEventStoreHistoryRepository.findByTransactionIdOrderByCreationDateAsc(
                    transaction.transactionId
                )
            )

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
                    .flatMap { (baseTransaction, email) ->
                        events.collectList().map { Triple(baseTransaction, email, it) }
                    }
                    .map { (baseTransaction, email, events) ->
                        baseTransactionToTransactionInfoDtoV1(
                            baseTransaction,
                            email.map { it as ConfidentialData },
                            events
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
                    .flatMap { (baseTransaction, email) ->
                        events.collectList().map { Triple(baseTransaction, email, it) }
                    }
                    .map { (baseTransaction, email, events) ->
                        baseTransactionToTransactionInfoDtoV2(
                            baseTransaction,
                            email.map { it as ConfidentialData },
                            events
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
