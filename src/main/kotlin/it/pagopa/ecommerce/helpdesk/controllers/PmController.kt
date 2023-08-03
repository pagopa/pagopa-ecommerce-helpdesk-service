package it.pagopa.ecommerce.helpdesk.controllers

import it.pagopa.ecommerce.helpdesk.services.PmService
import it.pagopa.generated.ecommerce.helpdesk.api.PmApi
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class PmController : PmApi {

    @Autowired private lateinit var pmService: PmService

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun pmSearchTransaction(
        pageNumber: Int?,
        pageSize: Int?,
        pmSearchTransactionRequestDto: Mono<PmSearchTransactionRequestDto>?,
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<SearchTransactionResponseDto>> {
        logger.info("[HelpDesk controller] pmSearchTransaction")
        return pmService
            .searchTransaction(pageSize, pageNumber, pmSearchTransactionRequestDto)
            .map { ResponseEntity.ok(it) }
    }
}
