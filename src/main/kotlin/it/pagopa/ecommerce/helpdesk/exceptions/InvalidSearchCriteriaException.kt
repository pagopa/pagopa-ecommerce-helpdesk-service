package it.pagopa.ecommerce.helpdesk.exceptions

import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import org.springframework.http.HttpStatus

class InvalidSearchCriteriaException(searchCriteriaType: String, productDto: ProductDto) :
    ApiError("Invalid search criteria with type: $searchCriteriaType for product: $productDto") {

    override fun toRestException() =
        RestApiException(
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            description = this.message!!,
            title = "Invalid search criteria"
        )
}
