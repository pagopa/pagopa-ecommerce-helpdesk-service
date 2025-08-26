package it.pagopa.ecommerce.helpdesk.controllers.v2

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.helpdesk.services.v2.EcommerceService
import it.pagopa.generated.ecommerce.helpdesk.v2.model.EcommerceSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchMetricsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.TransactionMetricsResponseDto
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*
import org.slf4j.LoggerFactory

@Path("/v2/ecommerce")
@Named("EcommerceV2Controller")
class EcommerceController(@Inject val ecommerceService: EcommerceService) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @POST
    @Path("/searchTransaction")
    @Consumes("application/json")
    @Produces("application/json")
    fun ecommerceSearchTransaction(
        @QueryParam("pageNumber") @DefaultValue("0") @Min(0) pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue("10") @Min(1) @Max(20) pageSize: Int,
        @Valid ecommerceSearchTransactionRequestDto: EcommerceSearchTransactionRequestDto
    ): Uni<SearchTransactionResponseDto> {
        logger.info("[HelpDesk controller] ecommerceSearchTransaction")
        return Uni.createFrom()
            .publisher(
                ecommerceService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    ecommerceSearchTransactionRequestDto = ecommerceSearchTransactionRequestDto
                )
            )
    }

    @POST
    @Path("/searchMetrics")
    @Consumes("application/json")
    @Produces("application/json")
    fun ecommerceSearchMetrics(
        @Valid searchMetricsRequestDto: SearchMetricsRequestDto
    ): Uni<TransactionMetricsResponseDto> {
        logger.info("[HelpDesk controller] ecommerceSearchMetrics")
        return Uni.createFrom()
            .publisher(
                ecommerceService.searchMetrics(searchMetricsRequestDto = searchMetricsRequestDto)
            )
    }
}
