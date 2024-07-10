package it.pagopa.ecommerce.helpdesk.services.v2

import it.pagopa.ecommerce.helpdesk.dataproviders.v2.oracle.PMPaymentMethodsDataProvider
import it.pagopa.ecommerce.helpdesk.dataproviders.v2.oracle.PMTransactionDataProvider
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.utils.SearchParamDecoderV2
import it.pagopa.ecommerce.helpdesk.utils.v2.buildTransactionSearchResponse
import it.pagopa.generated.ecommerce.helpdesk.v2.model.PmSearchPaymentMethodRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.PmSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchPaymentMethodResponseDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service("PmServiceV2")
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
                SearchParamDecoderV2(
                    searchParameter = pmSearchTransactionRequestDto,
                    confidentialMailUtils = null
                )
            )
            .flatMap { totalCount ->
                if (totalCount > 0) {
                    pmTransactionDataProvider
                        .findResult(
                            searchParams =
                                SearchParamDecoderV2(
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
