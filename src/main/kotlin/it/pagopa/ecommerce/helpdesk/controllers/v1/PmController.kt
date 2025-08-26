package it.pagopa.ecommerce.helpdesk.controllers.v1

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.helpdesk.services.v1.PmService
import it.pagopa.generated.ecommerce.helpdesk.model.*
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*
import org.slf4j.LoggerFactory

@Path("/pm")
@Named("PmV1Controller")
class PmController(@Inject val pmService: PmService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @POST
    @Path("/searchTransaction")
    @Consumes("application/json")
    @Produces("application/json")
    fun pmSearchTransaction(
        @QueryParam("pageNumber") @DefaultValue("0") @Min(0) pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue("10") @Min(1) @Max(20) pageSize: Int,
        @Valid pmSearchTransactionRequestDto: PmSearchTransactionRequestDto
    ): Uni<SearchTransactionResponseDto> {
        logger.info("[HelpDesk controller] pmSearchTransaction")
        return Uni.createFrom()
            .publisher(
                pmService.searchTransaction(
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    pmSearchTransactionRequestDto = pmSearchTransactionRequestDto
                )
            )
    }

    @POST
    @Path("/searchPaymentMethod")
    @Consumes("application/json")
    @Produces("application/json")
    fun pmSearchPaymentMethod(
        @Valid pmSearchPaymentMethodRequestDto: PmSearchPaymentMethodRequestDto
    ): Uni<SearchPaymentMethodResponseDto> {
        logger.info("[HelpDesk controller] pmSearchPaymentMethod")
        return Uni.createFrom()
            .publisher(
                pmService.searchPaymentMethod(
                    pmSearchPaymentMethodRequestDto = pmSearchPaymentMethodRequestDto
                )
            )
    }

    @POST
    @Path("/searchBulkTransaction")
    @Consumes("application/json")
    @Produces("application/json")
    fun pmSearchBulkTransaction(
        @Valid pmSearchBulkTransactionRequestDto: PmSearchBulkTransactionRequestDto
    ): Multi<TransactionBulkResultDto> {
        logger.info("[HelpDesk controller] pmSearchBulkTransaction")
        return Multi.createFrom()
            .publisher(pmService.searchBulkTransaction(pmSearchBulkTransactionRequestDto))
            .onItem()
            .transformToMultiAndMerge { list -> Multi.createFrom().iterable(list) }
    }
}
