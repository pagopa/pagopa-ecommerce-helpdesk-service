package it.pagopa.ecommerce.helpdesk.services

import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class PmService {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun searchTransaction(
        pageNumber: Int?,
        pageSize: Int?,
        pmSearchTransactionRequestDto: Mono<PmSearchTransactionRequestDto>?
    ): Mono<SearchTransactionResponseDto> {
        logger.info("[helpDesk pm service] searchTransaction method")
        return Mono.just(SearchTransactionResponseDto())
    }
}
