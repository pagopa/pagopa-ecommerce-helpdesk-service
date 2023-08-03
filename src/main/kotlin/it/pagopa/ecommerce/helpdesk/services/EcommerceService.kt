package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class EcommerceService {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int?,
        pageSize: Int?,
        ecommerceSearchTransactionRequestDto: Mono<EcommerceSearchTransactionRequestDto>?
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk ecommerce service] searchTransaction method")
        return Mono.just(SearchTransactionResponseDto())
    }
}
