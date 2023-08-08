package it.pagopa.ecommerce.helpdesk.dataproviders.mongo

import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class EcommerceTransactionDataProvider() : TransactionDataProvider {

    override fun totalRecorcCount(searchCriteria: HelpDeskSearchTransactionRequestDto): Mono<Int> =
        when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto -> Mono.just(0) // TODO implementare
            is SearchTransactionRequestRptIdDto -> Mono.just(0) // TODO implementare
            is SearchTransactionRequestTransactionIdDto -> Mono.just(0) // TODO implementare
            is SearchTransactionRequestEmailDto -> Mono.just(0)
            is SearchTransactionRequestFiscalCodeDto -> Mono.just(0)
            else ->
                Mono.error(
                    RuntimeException("Unhandled search criteria ${searchCriteria.javaClass}")
                )
        }

    override fun findResult(
        searchCriteria: HelpDeskSearchTransactionRequestDto,
        offset: Int,
        limit: Int
    ): Mono<List<TransactionResultDto>> =
        when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto ->
                Mono.just(listOf(TransactionResultDto())) // TODO implementare
            is SearchTransactionRequestRptIdDto ->
                Mono.just(listOf(TransactionResultDto())) // TODO implementare
            is SearchTransactionRequestTransactionIdDto ->
                Mono.just(listOf(TransactionResultDto())) // TODO implementare
            is SearchTransactionRequestEmailDto -> Mono.just(listOf(TransactionResultDto()))
            is SearchTransactionRequestFiscalCodeDto -> Mono.just(listOf(TransactionResultDto()))
            else ->
                Mono.error(
                    RuntimeException("Unhandled search criteria ${searchCriteria.javaClass}")
                )
        }
}
