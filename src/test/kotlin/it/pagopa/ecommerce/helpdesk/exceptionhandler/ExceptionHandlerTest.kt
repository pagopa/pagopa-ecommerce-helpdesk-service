package it.pagopa.ecommerce.helpdesk.exceptionhandler

import it.pagopa.ecommerce.commons.exceptions.ConfidentialDataException
import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.exceptions.InvalidSearchCriteriaException
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.exceptions.RestApiException
import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import jakarta.xml.bind.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException

class ExceptionHandlerTest {

    private val exceptionHandler = ExceptionHandler()

    @Test
    fun `Should handle RestApiException`() {
        val response =
            exceptionHandler.handleException(
                RestApiException(
                    httpStatus = HttpStatus.UNAUTHORIZED,
                    title = "title",
                    description = "description"
                )
            )
        assertEquals(
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.UNAUTHORIZED,
                title = "title",
                description = "description"
            ),
            response.body
        )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Should handle NoResultFoundException`() {
        val searchCriteria = "searchCriteria"
        val exception = NoResultFoundException(searchCriteria)
        val response = exceptionHandler.handleException(exception)
        assertEquals(
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.NOT_FOUND,
                title = "No result found",
                description = "No result can be found searching for criteria $searchCriteria"
            ),
            response.body
        )
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `Should handle ValidationExceptions`() {
        val exception = ValidationException("Invalid request")
        val response = exceptionHandler.handleRequestValidationException(exception)
        assertEquals(
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.BAD_REQUEST,
                title = "Bad request",
                description = "Invalid request"
            ),
            response.body
        )
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Should handle generic exception`() {
        val exception = NullPointerException("Nullpointer exception")
        val response = exceptionHandler.handleGenericException(exception)
        assertEquals(
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                title = "Error processing the request",
                description = "Generic error occurred"
            ),
            response.body
        )
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `Should handle InvalidSearchCriteriaException`() {
        val searchCriteria = "searchCriteria"
        val exception = InvalidSearchCriteriaException(searchCriteria, ProductDto.PM)
        val response = exceptionHandler.handleException(exception)
        assertEquals(
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                title = "Invalid search criteria",
                description =
                    "Invalid search criteria with type: $searchCriteria for product: ${ProductDto.PM}"
            ),
            response.body
        )
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `Should handle ConfidentialDataException`() {
        val exception =
            ConfidentialDataException(
                WebClientResponseException(HttpStatus.NOT_FOUND.value(), "", null, null, null)
            )
        val response = exceptionHandler.handleConfidentialDataException(exception)
        assertEquals(
            HelpdeskTestUtils.buildProblemJson(
                httpStatus = HttpStatus.BAD_GATEWAY,
                title = "Error processing the request",
                description = "Error while processing pdv request"
            ),
            response.body
        )
        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
    }
}
