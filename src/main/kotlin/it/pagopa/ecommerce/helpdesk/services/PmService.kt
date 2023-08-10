package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.dataproviders.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PmService(@Autowired val pmTransactionDataProvider: PMTransactionDataProvider) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        pmSearchTransactionRequestDto: PmSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        logger.info(
            "[helpDesk pm service] searchTransaction method, search type: {}",
            pmSearchTransactionRequestDto.type
        )
        return pmTransactionDataProvider.totalRecordCount(pmSearchTransactionRequestDto).flatMap {
            totalCount ->
            if (totalCount > 0) {
                pmTransactionDataProvider
                    .findResult(
                        searchParams = pmSearchTransactionRequestDto,
                        pageSize = pageSize,
                        pageNumber = pageNumber
                    )
                    .map { results ->
                        buildTransactionSearchResponse(
                            currentPage = pageNumber,
                            totalCount = totalCount,
                            results = results
                        )
                    }
            } else {
                Mono.error(NoResultFoundException(pmSearchTransactionRequestDto.type))
            }
        }
    }
}
