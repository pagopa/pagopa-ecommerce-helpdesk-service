package it.pagopa.ecommerce.helpdesk.dataproviders.v2.mongo

import it.pagopa.ecommerce.commons.documents.BaseTransactionView
import it.pagopa.ecommerce.commons.exceptions.ConfidentialDataException
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsEventStoreRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.repositories.ecommerce.TransactionsViewRepository
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.TransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.ConfidentialMailUtils
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
    @Autowired private val transactionsEventStoreRepository: TransactionsEventStoreRepository<Any>
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
                        transactionsViewRepository.countTransactionsWithPaymentToken(
                            it.paymentToken
                        )
                    is SearchTransactionRequestRptIdDto ->
                        transactionsViewRepository.countTransactionsWithRptId(it.rptId)
                    is SearchTransactionRequestTransactionIdDto ->
                        transactionsViewRepository.existsById(it.transactionId).map { exist ->
                            if (exist) {
                                1
                            } else {
                                0
                            }
                        }
                    is SearchTransactionRequestEmailDto ->
                        transactionsViewRepository.countTransactionsWithEmail(it.userEmail)
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
                        transactionsViewRepository
                            .findTransactionsWithPaymentTokenPaginatedOrderByCreationDateDesc(
                                paymentToken = it.paymentToken,
                                skip = skip,
                                limit = limit
                            )
                    is SearchTransactionRequestRptIdDto ->
                        transactionsViewRepository
                            .findTransactionsWithRptIdPaginatedOrderByCreationDateDesc(
                                rptId = it.rptId,
                                skip = skip,
                                limit = limit
                            )
                    is SearchTransactionRequestTransactionIdDto ->
                        transactionsViewRepository.findById(it.transactionId).toFlux()
                    is SearchTransactionRequestEmailDto ->
                        transactionsViewRepository
                            .findTransactionsWithEmailPaginatedOrderByCreationDateDesc(
                                encryptedEmail = it.userEmail,
                                skip = skip,
                                limit = limit
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
                                .toClearData(baseTransaction.email)
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
                        baseTransactionToTransactionInfoDtoV1(baseTransaction, email, events)
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
                                .toClearData(baseTransaction.email)
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
                        baseTransactionToTransactionInfoDtoV2(baseTransaction, email, events)
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
