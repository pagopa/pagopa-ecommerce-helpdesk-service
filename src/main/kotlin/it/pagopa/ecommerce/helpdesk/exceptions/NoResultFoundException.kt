package it.pagopa.ecommerce.helpdesk.exceptions

import org.springframework.http.HttpStatus

class NoResultFoundException(private val searchCriteriaType: String) :
    ApiError("No result found for criteria: $searchCriteriaType") {

    override fun toRestException() =
        RestApiException(
            httpStatus = HttpStatus.NOT_FOUND,
            description = "No result can be found searching for criteria $searchCriteriaType",
            title = "No result found"
        )
}
