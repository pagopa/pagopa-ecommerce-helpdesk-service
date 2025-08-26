package it.pagopa.ecommerce.helpdesk.controllers.v2

import io.smallrye.mutiny.Uni
import it.pagopa.ecommerce.helpdesk.services.v2.HelpdeskService
import it.pagopa.ecommerce.helpdesk.utils.PmProviderType
import it.pagopa.generated.ecommerce.helpdesk.v2.model.HelpDeskSearchTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.v2.model.SearchTransactionResponseDto
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.slf4j.LoggerFactory

@Path("/v2/helpdesk")
@Named("HelpdeskV2Controller")
class HelpdeskController(
    @Inject val helpdeskService: HelpdeskService,
    @ConfigProperty(name = "search.pm.in.ecommerce.history.enabled:false")
    private val searchPmInEcommerceHistory: Boolean
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
        logger.info(
            "[HelpDesk V2 controller] SearchTransaction using ${if (searchPmInEcommerceHistory) "v2 (ecommerce history db)" else "v1 (pm legacy db)"} search"
        )
        return Uni.createFrom()
            .publisher(
                helpdeskService.searchTransaction(
                    pageNumber = pageNumber,
                    pageSize = pageSize,
                    searchTransactionRequestDto = helpDeskSearchTransactionRequestDto,
                    pmProviderType =
                        if (searchPmInEcommerceHistory) PmProviderType.ECOMMERCE_HISTORY
                        else PmProviderType.PM_LEGACY
                )
            )
    }
}
