package it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo

import it.pagopa.ecommerce.commons.documents.BaseTransactionEvent
import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.exceptions.ConfidentialDataException
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager.ConfidentialData
import it.pagopa.ecommerce.helpdesk.dataproviders.CountInfo
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsEventStoreHistoryRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history.TransactionsViewHistoryRepository
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
    ): Mono<CountInfo> {
        val decodedSearchParam = searchParams.decode()
        val invalidSearchCriteriaError =
            decodedSearchParam.flatMap {
                Mono.error<CountInfo>(InvalidSearchCriteriaException(it.type, ProductDto.ECOMMERCE))
            }
        return decodedSearchParam.flatMap {
            when (it) {
                is SearchTransactionRequestPaymentTokenDto ->
                    Mono.zip(
                        transactionsViewRepository.countTransactionsWithPaymentToken(
                            it.paymentToken
                        ),
                        transactionsViewHistoryRepository.countTransactionsWithPaymentToken(
                            it.paymentToken
                        ),
                    ) { transactionsViewCount, transactionsViewHistoryCount ->
                        CountInfo(transactionsViewCount, transactionsViewHistoryCount)
                    }
                is SearchTransactionRequestRptIdDto ->
                    Mono.zip(
                        transactionsViewRepository.countTransactionsWithRptId(it.rptId),
                        transactionsViewHistoryRepository.countTransactionsWithRptId(it.rptId)
                    ) { transactionsViewCount, transactionsViewHistoryCount ->
                        CountInfo(transactionsViewCount, transactionsViewHistoryCount)
                    }
                is SearchTransactionRequestTransactionIdDto ->
                    Mono.zip(
                        transactionsViewRepository.existsById(it.transactionId).map { exist ->
                            if (exist) {
                                1
                            } else {
                                0
                            }
                        },
                        transactionsViewHistoryRepository.existsById(it.transactionId).map { exist
                            ->
                            if (exist) {
                                1
                            } else {
                                0
                            }
                        }
                    ) { transactionsViewCount, transactionsViewHistoryCount ->
                        CountInfo(
                            transactionsViewCount.toLong(),
                            transactionsViewHistoryCount.toLong()
                        )
                    }
                is SearchTransactionRequestEmailDto ->
                    Mono.zip(
                        transactionsViewRepository.countTransactionsWithEmail(it.userEmail),
                        transactionsViewHistoryRepository.countTransactionsWithEmail(it.userEmail)
                    ) { transactionsViewCount, transactionsViewHistoryCount ->
                        CountInfo(transactionsViewCount, transactionsViewHistoryCount)
                    }
                is SearchTransactionRequestFiscalCodeDto ->
                    Mono.zip(
                        transactionsViewRepository.countTransactionsWithFiscalCode(
                            it.userFiscalCode
                        ),
                        transactionsViewHistoryRepository.countTransactionsWithFiscalCode(
                            it.userFiscalCode
                        )
                    ) { transactionsViewCount, transactionsViewHistoryCount ->
                        CountInfo(transactionsViewCount, transactionsViewHistoryCount)
                    }
                is SearchTransactionRequestAuthorizationRequestIdDto ->
                    Mono.zip(
                        transactionsViewRepository.countTransactionsWithAuthorizationRequestId(
                            it.authorizationRequestId
                        ),
                        transactionsViewHistoryRepository
                            .countTransactionsWithAuthorizationRequestId(it.authorizationRequestId)
                    ) { transactionsViewCount, transactionsViewHistoryCount ->
                        CountInfo(transactionsViewCount, transactionsViewHistoryCount)
                    }
                is SearchTransactionRequestRRNDto ->
                    Mono.zip(
                        transactionsViewRepository.countTransactionsWithRRN(it.rrn),
                        transactionsViewHistoryRepository.countTransactionsWithRRN(it.rrn)
                    ) { transactionsViewCount, transactionsViewHistoryCount ->
                        CountInfo(transactionsViewCount, transactionsViewHistoryCount)
                    }
                is SearchTransactionRequestEndToEndIdDto ->
                    Mono.zip(
                        transactionsViewRepository.countTransactionsWithEndToEndId(it.endToEndId),
                        transactionsViewHistoryRepository.countTransactionsWithEndToEndId(
                            it.endToEndId
                        )
                    ) { transactionsViewCount, transactionsViewHistoryCount ->
                        CountInfo(transactionsViewCount, transactionsViewHistoryCount)
                    }
                else -> invalidSearchCriteriaError
            }
        }
    }

    override fun findResult(
        searchParams: SearchParamDecoderV2<HelpDeskSearchTransactionRequestDto>,
        skip: Int,
        limit: Int,
        countInfo: CountInfo
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
                        readEventsFromDbs(
                            onlineDbQuery = { skip, limit ->
                                transactionsViewRepository
                                    .findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                                        paymentToken = it.paymentToken,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            historyDbQuery = { skip, limit ->
                                transactionsViewHistoryRepository
                                    .findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                                        paymentToken = it.paymentToken,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            skip = skip,
                            limit = limit,
                            countInfo = countInfo
                        )
                    is SearchTransactionRequestRptIdDto ->
                        readEventsFromDbs(
                            onlineDbQuery = { skip, limit ->
                                transactionsViewRepository
                                    .findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                                        rptId = it.rptId,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            historyDbQuery = { skip, limit ->
                                transactionsViewHistoryRepository
                                    .findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                                        rptId = it.rptId,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            skip = skip,
                            limit = limit,
                            countInfo = countInfo
                        )
                    is SearchTransactionRequestTransactionIdDto ->
                        readEventsFromDbs(
                            onlineDbQuery = { _, _ ->
                                transactionsViewRepository.findById(it.transactionId).toFlux()
                            },
                            historyDbQuery = { _, _ ->
                                transactionsViewHistoryRepository
                                    .findById(it.transactionId)
                                    .toFlux()
                            },
                            skip = skip,
                            limit = limit,
                            countInfo = countInfo
                        )
                    is SearchTransactionRequestEmailDto ->
                        readEventsFromDbs(
                            onlineDbQuery = { skip, limit ->
                                transactionsViewRepository
                                    .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                                        encryptedEmail = it.userEmail,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            historyDbQuery = { skip, limit ->
                                transactionsViewHistoryRepository
                                    .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                                        encryptedEmail = it.userEmail,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            skip = skip,
                            limit = limit,
                            countInfo = countInfo
                        )
                    is SearchTransactionRequestFiscalCodeDto ->
                        readEventsFromDbs(
                            onlineDbQuery = { skip, limit ->
                                transactionsViewRepository
                                    .findTransactionsWithFiscalCodePaginatedOrderByCreationDateDesc(
                                        encryptedFiscalCode = it.userFiscalCode,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            historyDbQuery = { skip, limit ->
                                transactionsViewHistoryRepository
                                    .findTransactionsWithFiscalCodePaginatedOrderByCreationDateDesc(
                                        encryptedFiscalCode = it.userFiscalCode,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            skip = skip,
                            limit = limit,
                            countInfo = countInfo
                        )
                    is SearchTransactionRequestRRNDto ->
                        readEventsFromDbs(
                            onlineDbQuery = { skip, limit ->
                                transactionsViewRepository
                                    .findTransactionsWithRRNPaginatedOrderByCreationDateDesc(
                                        rrn = it.rrn,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            historyDbQuery = { skip, limit ->
                                transactionsViewHistoryRepository
                                    .findTransactionsWithRRNPaginatedOrderByCreationDateDesc(
                                        rrn = it.rrn,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            skip = skip,
                            limit = limit,
                            countInfo = countInfo
                        )
                    is SearchTransactionRequestAuthorizationRequestIdDto ->
                        readEventsFromDbs(
                            onlineDbQuery = { skip, limit ->
                                transactionsViewRepository
                                    .findTransactionsWithAuthorizationRequestIdPaginatedOrderByCreationDateDesc(
                                        authorizationRequestId = it.authorizationRequestId,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            historyDbQuery = { skip, limit ->
                                transactionsViewHistoryRepository
                                    .findTransactionsWithAuthorizationRequestIdPaginatedOrderByCreationDateDesc(
                                        authorizationRequestId = it.authorizationRequestId,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            skip = skip,
                            limit = limit,
                            countInfo = countInfo
                        )
                    is SearchTransactionRequestEndToEndIdDto ->
                        readEventsFromDbs(
                            onlineDbQuery = { skip, limit ->
                                transactionsViewRepository
                                    .findTransactionsWithEndToEndIdPaginatedOrderByCreationDateDesc(
                                        endToEndId = it.endToEndId,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            historyDbQuery = { skip, limit ->
                                transactionsViewHistoryRepository
                                    .findTransactionsWithEndToEndIdPaginatedOrderByCreationDateDesc(
                                        endToEndId = it.endToEndId,
                                        skip = skip,
                                        limit = limit
                                    )
                            },
                            skip = skip,
                            limit = limit,
                            countInfo = countInfo
                        )
                    else -> invalidSearchCriteriaError
                }
            }
        return transactions
            .flatMap { mapToTransactionResultDto(it, searchParams.confidentialMailUtils!!) }
            .collectList()
    }

    private fun readEventsFromDbs(
        onlineDbQuery: (skip: Int, limit: Int) -> Flux<BaseTransactionView>,
        historyDbQuery: (skip: Int, limit: Int) -> Flux<BaseTransactionView>,
        skip: Int,
        limit: Int,
        countInfo: CountInfo
    ): Flux<BaseTransactionView> {
        val onlineCount = countInfo.onlineDbCount
        val historicalCount = countInfo.historyDbCount
        /*
         * we serve online db records first and then historyDb ones concatenated
         * this order reflect the fact that records are ordered by descending timestamp
         * and history db surely contains records older than online DB
         */
        val recordOffset = skip + limit
        return if (recordOffset <= onlineCount) {
            // in this case we have to serve db only records
            onlineDbQuery(skip, limit)
        } else {
            if (skip < onlineCount) {
                // in this case requested offset overlap between online and history db, we have to
                // retrieve records from both datasource
                val onlineDbLimit = onlineCount - skip
                val historyDbLimit = limit - onlineDbLimit
                Flux.concat(
                    onlineDbQuery(skip, onlineDbLimit.toInt()),
                    if (historicalCount > 0) {
                        historyDbQuery(0, historyDbLimit.toInt())
                    } else {
                        Flux.empty()
                    }
                )
            } else {
                // otherwise we have left historical db records only
                val historyDbSkip = skip - onlineCount
                historyDbQuery(historyDbSkip.toInt(), limit)
            }
        }
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
