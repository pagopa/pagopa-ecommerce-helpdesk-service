package it.pagopa.ecommerce.helpdesk.controllers.v1

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.helpdesk.services.v1.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.model.*
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.*
import org.slf4j.LoggerFactory

@Path("/ecommerce")
@Named("EcommerceV1Controller")
class EcommerceController(@Inject val ecommerceService: EcommerceService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @POST
    @Path("/searchTransaction")
    @Consumes("application/json")
    @Produces("application/json")
    fun ecommerceSearchTransaction(
        @QueryParam("pageNumber") @DefaultValue("0") @Min(0) pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue("10") @Min(1) @Max(20) pageSize: Int,
        @Valid @NotNull ecommerceSearchTransactionRequestDto: EcommerceSearchTransactionRequestDto
    ): Uni<SearchTransactionResponseDto> {
        logger.info("Handling ecommerceSearchTransaction")
        // TODO: refactor after service migration - remove Mono->Uni conversion, return service call directly
        return Uni.createFrom().completionStage {
            ecommerceService.searchTransaction(
                pageNumber = pageNumber,
                pageSize = pageSize,
                ecommerceSearchTransactionRequestDto = ecommerceSearchTransactionRequestDto
            ).toFuture() // remove when service returns Uni<T> instead of Mono<T>
        }
    }

    @POST
    @Path("/searchDeadLetterEvents")
    @Consumes("application/json")
    @Produces("application/json")
    fun ecommerceSearchDeadLetterEvents(
        @QueryParam("pageNumber") @DefaultValue("0") @Min(0) pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue("10") @Min(1) @Max(1000) pageSize: Int,
        @Valid
        @NotNull
        ecommerceSearchDeadLetterEventsRequestDto: EcommerceSearchDeadLetterEventsRequestDto
    ): Uni<SearchDeadLetterEventResponseDto> {
        logger.info("[HelpDesk controller] ecommerceSearchDeadLetterEvents")
        // TODO: refactor after service migration - remove Mono->Uni conversion, return service call directly
        return Uni.createFrom().completionStage {
            ecommerceService.searchDeadLetterEvents(
                pageNumber = pageNumber,
                pageSize = pageSize,
                searchRequest = ecommerceSearchDeadLetterEventsRequestDto
            ).toFuture() // remove when service returns Uni<T> instead of Mono<T>
        }
    }

    @POST
    @Path("/searchNpgOperations")
    @Consumes("application/json")
    @Produces("application/json")
    fun ecommerceSearchNpgOperationsPost(
        @Valid searchNpgOperationsRequestDto: SearchNpgOperationsRequestDto
    ): Uni<SearchNpgOperationsResponseDto> {
        // TODO: refactor after service migration - remove Mono->Uni conversion, return service call directly
        return Uni.createFrom().completionStage {
            ecommerceService.searchNpgOperations(
                transactionId = searchNpgOperationsRequestDto.idTransaction
            ).toFuture() // remove when service returns Uni<T> instead of Mono<T>
        }
    }
}
