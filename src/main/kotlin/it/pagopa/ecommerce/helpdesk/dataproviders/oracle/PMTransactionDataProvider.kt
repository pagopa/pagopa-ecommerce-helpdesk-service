package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.TransactionDataProvider
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class PMTransactionDataProvider(@Autowired connectionFactory: ConnectionFactory) :
    TransactionDataProvider {

    override fun totalRecorcCount(searchCriteria: HelpDeskSearchTransactionRequestDto): Mono<Int> =
        when (searchCriteria) {
            is SearchTransactionRequestPaymentTokenDto -> Mono.just(0)
            is SearchTransactionRequestRptIdDto -> Mono.just(0)
            is SearchTransactionRequestTransactionIdDto -> Mono.just(0)
            is SearchTransactionRequestEmailDto -> Mono.just(0) // TODO implementare
            is SearchTransactionRequestFiscalCodeDto -> Mono.just(0) // TODO implementare
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
            is SearchTransactionRequestPaymentTokenDto -> Mono.just(listOf(TransactionResultDto()))
            is SearchTransactionRequestRptIdDto -> Mono.just(listOf(TransactionResultDto()))
            is SearchTransactionRequestTransactionIdDto -> Mono.just(listOf(TransactionResultDto()))
            is SearchTransactionRequestEmailDto ->
                Mono.just(listOf(TransactionResultDto())) // TODO implementare
            is SearchTransactionRequestFiscalCodeDto ->
                Mono.just(listOf(TransactionResultDto())) // TODO implementare
            else ->
                Mono.error(
                    RuntimeException("Unhandled search criteria ${searchCriteria.javaClass}")
                )
        }
}
