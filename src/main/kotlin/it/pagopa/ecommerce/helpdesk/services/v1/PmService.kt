package it.pagopa.ecommerce.helpdesk.services.v1

import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMPaymentMethodsDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.SearchParamDecoder
import it.pagopa.ecommerce.helpdesk.utils.v1.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchPaymentMethodRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchPaymentMethodResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service("PmServiceV1")
class PmService(
    @Autowired val pmTransactionDataProvider: PMTransactionDataProvider,
    @Autowired val pmPaymentMethodsDataProvider: PMPaymentMethodsDataProvider
) {

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
        return pmTransactionDataProvider
            .totalRecordCount(
                SearchParamDecoder(
                    searchParameter = pmSearchTransactionRequestDto,
                    confidentialMailUtils = null
                )
            )
            .flatMap { totalCount ->
                if (totalCount > 0) {
                    pmTransactionDataProvider
                        .findResult(
                            searchParams =
                                SearchParamDecoder(
                                    searchParameter = pmSearchTransactionRequestDto,
                                    confidentialMailUtils = null
                                ),
                            skip = pageSize * pageNumber,
                            limit = pageSize
                        )
                        .map { results ->
                            buildTransactionSearchResponse(
                                currentPage = pageNumber,
                                totalCount = totalCount,
                                pageSize = pageSize,
                                results = results
                            )
                        }
                } else {
                    Mono.error(NoResultFoundException(pmSearchTransactionRequestDto.type))
                }
            }
    }

    fun searchPaymentMethod(
        pmSearchPaymentMethodRequestDto: PmSearchPaymentMethodRequestDto
    ): Mono<SearchPaymentMethodResponseDto> {
        logger.info(
            "[helpDesk pm service] searchPaymentMethods, search type: {}",
            pmSearchPaymentMethodRequestDto.type
        )
        return pmPaymentMethodsDataProvider.findResult(pmSearchPaymentMethodRequestDto)
    }
}
