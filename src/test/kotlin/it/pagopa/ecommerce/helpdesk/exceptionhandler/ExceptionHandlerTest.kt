package it.pagopa.ecommerce.helpdesk.exceptionhandler

import it.pagopa.ecommerce.helpdesk.HelpdeskTestUtils
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.ecommerce.helpdesk.exceptions.RestApiException
import jakarta.xml.bind.ValidationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

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
    fun `Should handle ApiError`() {
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
                description = "Nullpointer exception"
            ),
            response.body
        )
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }
}