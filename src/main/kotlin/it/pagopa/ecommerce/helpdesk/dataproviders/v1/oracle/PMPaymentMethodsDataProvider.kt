package it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.PaymentMethodDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.utils.v1.resultToPaymentMethodDtoList
import it.pagopa.generated.ecommerce.helpdesk.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * PaymentMethodsDataProvider implementation that search payment methods into PM DB
 *
 * @see PMPaymentMethodsDataProvider
 */
@Component
class PMPaymentMethodsDataProvider(@Autowired private val connectionFactory: ConnectionFactory) :
    PaymentMethodDataProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun findResult(
        searchParams: PmSearchPaymentMethodRequestDto
    ): Mono<SearchPaymentMethodResponseDto> {
        val searchCriteriaType = searchParams.type
        val invalidSearchCriteriaError =
            Mono.error<SearchPaymentMethodResponseDto>(
                InvalidSearchCriteriaException(searchCriteriaType, ProductDto.PM.name)
            )
        return when (searchParams) {
            is SearchPaymentMethodRequestEmailDto ->
                getResultSetFromQuery(
                    resultQuery = searchWalletByUserEmail,
                    searchParam = searchParams.userEmail,
                    searchType = searchCriteriaType
                )
            is SearchPaymentMethodRequestFiscalCodeDto ->
                getResultSetFromQuery(
                    resultQuery = searchWalletByUserFiscalCode,
                    searchParam = searchParams.userFiscalCode,
                    searchType = searchCriteriaType
                )
            else -> invalidSearchCriteriaError
        }
    }

    private fun getResultSetFromQuery(
        resultQuery: String,
        searchParam: String,
        searchType: String
    ): Mono<SearchPaymentMethodResponseDto> =
        Mono.usingWhen(
            connectionFactory.create(),
            { connection ->
                logger.info("Retrieving payment methods from PM database.")

                Flux.from(connection.createStatement(resultQuery).bind(0, searchParam).execute())
                    .collectList()
                    .flatMap { results -> resultToPaymentMethodDtoList(results, searchType) }
            },
            { it.close() }
        )
}
