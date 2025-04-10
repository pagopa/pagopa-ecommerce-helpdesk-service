package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.ecommerce.commons.v1.TransactionTestUtils
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ConfidentialMailUtilsTests {

    private val emailCachedMap: MutableMap<String, Email> = mutableMapOf()

    private val confidentialDataManager: ConfidentialDataManager = mock()

    private val confidentialMailUtils: ConfidentialMailUtils =
        ConfidentialMailUtils(confidentialDataManager, emailCachedMap)

    @Test
    fun shouldDecryptMailSuccessfully() {
        val email = Email(TransactionTestUtils.EMAIL_STRING)
        val emailToken = UUID.randomUUID()
        val computedConfidential = Confidential<Email>(emailToken.toString())

        /* preconditions */
        given(confidentialDataManager.decrypt(eq(computedConfidential), any()))
            .willReturn(Mono.just(email))

        /* test */
        StepVerifier.create(confidentialMailUtils.toClearData(computedConfidential))
            .expectNext(email)
            .verifyComplete()

        assertEquals(email, emailCachedMap[computedConfidential.opaqueData])
        verify(confidentialDataManager, Mockito.times(1)).decrypt(eq(computedConfidential), any())
    }

    @Test
    fun shouldDecryptMailSuccessfullyWithCachedValue() {
        val email = Email(TransactionTestUtils.EMAIL_STRING)
        val emailToken = UUID.randomUUID()
        val computedConfidential = Confidential<Email>(emailToken.toString())
        emailCachedMap[computedConfidential.opaqueData] = email

        /* test */
        StepVerifier.create(confidentialMailUtils.toClearData(computedConfidential))
            .expectNext(email)
            .verifyComplete()

        verify(confidentialDataManager, Mockito.times(0)).decrypt(eq(computedConfidential), any())
        assertEquals(email, emailCachedMap[computedConfidential.opaqueData])
    }

    @Test
    fun shouldEncryptMailSuccessfully() {
        val email = Email(TransactionTestUtils.EMAIL_STRING)
        val emailToken = UUID.randomUUID()
        val computedConfidential = Confidential<Email>(emailToken.toString())

        /* preconditions */
        given(confidentialDataManager.encrypt(eq(email)))
            .willReturn(Mono.just(computedConfidential))

        /* test */
        StepVerifier.create(confidentialMailUtils.toConfidential(email.value))
            .expectNext(computedConfidential)
            .verifyComplete()

        verify(confidentialDataManager, Mockito.times(1)).encrypt(eq(email))
    }
}
