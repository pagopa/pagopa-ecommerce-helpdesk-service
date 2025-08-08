package it.pagopa.ecommerce.helpdesk.controllers.v1

import it.pagopa.ecommerce.helpdesk.services.v1.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.api.EcommerceApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import jakarta.ws.rs.core.Response

@Path("/ecommerce")
@Named("EcommerceV1Controller")
class EcommerceController(@Inject val ecommerceService: EcommerceService) : EcommerceApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun ecommerceSearchTransaction(
        @QueryParam("pageNumber") @DefaultValue("0") @Min(0) pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue("10") @Min(1) @Max(20) pageSize: Int,
        @Valid @NotNull ecommerceSearchTransactionRequestDto: EcommerceSearchTransactionRequestDto
    ): SearchTransactionResponseDto? {
        logger.info("Handling ecommerceSearchTransaction")

        val result = ecommerceService.searchTransaction(
            pageNumber = pageNumber,
            pageSize = pageSize,
            ecommerceSearchTransactionRequestDto = ecommerceSearchTransactionRequestDto
        )

        return result;
    }

    override fun ecommerceSearchDeadLetterEvents(
        @QueryParam("pageNumber") @DefaultValue("0") @Min(0) pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue("10") @Min(1) @Max(1000) pageSize: Int,
        @Valid @NotNull ecommerceSearchDeadLetterEventsRequestDto: EcommerceSearchDeadLetterEventsRequestDto,

    ): SearchDeadLetterEventResponseDto {
        logger.info("[HelpDesk controller] ecommerceSearchDeadLetterEvents")

        val response = ecommerceService.searchDeadLetterEvents(
            pageNumber = pageNumber,
            pageSize = pageSize,
            searchRequest = ecommerceSearchDeadLetterEventsRequestDto
        )

        return response
    }

    override fun ecommerceSearchNpgOperationsPost(
        searchNpgOperationsRequestDto: SearchNpgOperationsRequestDto,

    ): SearchNpgOperationsResponseDto {
       return ecommerceService.searchNpgOperations(transactionId = searchNpgOperationsRequestDto.idTransaction)
    }
}
