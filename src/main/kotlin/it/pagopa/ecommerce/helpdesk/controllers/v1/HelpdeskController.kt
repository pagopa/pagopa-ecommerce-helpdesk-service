package it.pagopa.ecommerce.helpdesk.controllers.v1

import it.pagopa.ecommerce.helpdesk.services.v1.HelpdeskService
import it.pagopa.ecommerce.helpdesk.services.v1.PmService
import it.pagopa.generated.ecommerce.helpdesk.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.PmSearchPaymentMethodRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchPaymentMethodResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*
import org.slf4j.LoggerFactory

@Path("/helpdesk")
@Named("HelpdeskV1Controller")
class HelpdeskController(
    @Inject val helpdeskService: HelpdeskService,
    @Inject val pmService: PmService
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @POST
    @Path("/searchTransaction")
    @Consumes("application/json")
    @Produces("application/json")
    fun helpDeskSearchTransaction(
        @QueryParam("pageNumber") @DefaultValue("0") @Min(0) pageNumber: Int,
        @QueryParam("pageSize") @DefaultValue("10") @Min(1) @Max(20) pageSize: Int,
        @Valid helpDeskSearchTransactionRequestDto: HelpDeskSearchTransactionRequestDto
    ): Uni<SearchTransactionResponseDto> {
        return Uni.createFrom().publisher(
            helpdeskService.searchTransaction(
                pageNumber = pageNumber,
                pageSize = pageSize,
                searchTransactionRequestDto = helpDeskSearchTransactionRequestDto
            )
        )
    }

    @POST
    @Path("/searchPaymentMethod")
    @Consumes("application/json")
    @Produces("application/json")
    fun helpDeskSearchPaymentMethod(
        @Valid pmSearchPaymentMethodRequestDto: PmSearchPaymentMethodRequestDto
    ): Uni<SearchPaymentMethodResponseDto> {
        logger.info("[HelpDesk controller] pmSearchPaymentMethod")
        return Uni.createFrom().publisher(
            pmService.searchPaymentMethod(pmSearchPaymentMethodRequestDto = pmSearchPaymentMethodRequestDto)
        )
    }
}
