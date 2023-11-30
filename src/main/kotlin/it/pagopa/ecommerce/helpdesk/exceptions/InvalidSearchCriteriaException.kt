package it.pagopa.ecommerce.helpdesk.exceptions

import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import org.springframework.http.HttpStatus

class InvalidSearchCriteriaException(reason: String) : ApiError(reason) {

    override fun toRestException() =
        RestApiException(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            description = this.message!!,
            title = "Invalid search criteria"
        )

    constructor(
        searchCriteriaType: String,
        productDto: ProductDto
    ) : this("Invalid search criteria with type: $searchCriteriaType for product: $productDto")
}
