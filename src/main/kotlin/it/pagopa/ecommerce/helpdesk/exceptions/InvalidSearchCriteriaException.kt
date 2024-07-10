package it.pagopa.ecommerce.helpdesk.exceptions

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
        productDto: String
    ) : this("Invalid search criteria with type: $searchCriteriaType for product: $productDto")
}
