package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.ecommerce.helpdesk.dataproviders.mongo.EcommerceTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class EcommerceService(
    @Autowired private val ecommerceTransactionDataProvider: EcommerceTransactionDataProvider
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int,
        pageSize: Int,
        ecommerceSearchTransactionRequestDto: EcommerceSearchTransactionRequestDto
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk ecommerce service] searchTransaction method")
        return ecommerceTransactionDataProvider
            .totalRecordCount(ecommerceSearchTransactionRequestDto)
            .flatMap { totalCount ->
                if (totalCount > 0) {
                    ecommerceTransactionDataProvider.findResult(
                        searchParams = ecommerceSearchTransactionRequestDto,
                        pageSize = pageSize,
                        pageNumber = pageNumber
                    ).map { results ->
                        buildTransactionSearchResponse(
                            currentPage = pageNumber,
                            totalCount = totalCount,
                            results = results
                        )
                    }
                } else {
                    Mono.error(NoResultFoundException(ecommerceSearchTransactionRequestDto.type))
                }
            }
    }
}
