package it.pagopa.ecommerce.helpdesk.services

import io.r2dbc.spi.ConnectionFactory
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.buildTransactionByUserEmailCountQuery
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.buildTransactionByUserEmailPaginatedQuery
import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.getResultSetFromPaginatedQuery
import it.pagopa.ecommerce.helpdesk.utils.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestEmailDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestFiscalCodeDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PmService(@Autowired val connectionFactory: ConnectionFactory) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        pmSearchTransactionRequestDto: PmSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        logger.info(
            "[helpDesk pm service] searchTransaction method, search type: ${pmSearchTransactionRequestDto.type}"
        )
        return when (pmSearchTransactionRequestDto) {
            is SearchTransactionRequestEmailDto ->
                getResultSetFromPaginatedQuery(
                    connectionFactory = connectionFactory,
                    totalRecordCountQuery =
                        buildTransactionByUserEmailCountQuery(
                            pmSearchTransactionRequestDto.userEmail
                        ),
                    resultQuery =
                        buildTransactionByUserEmailPaginatedQuery(
                            pmSearchTransactionRequestDto.userEmail
                        ),
                    pageSize = pageSize,
                    pageNumber = pageNumber
                )
            is SearchTransactionRequestFiscalCodeDto ->
                Mono.error(RuntimeException("Not implemented yet"))
            else -> Mono.error(RuntimeException(""))
        }.map { (totalCount, results) ->
            buildTransactionSearchResponse(pageNumber, totalCount, results)
        }
    }
}
