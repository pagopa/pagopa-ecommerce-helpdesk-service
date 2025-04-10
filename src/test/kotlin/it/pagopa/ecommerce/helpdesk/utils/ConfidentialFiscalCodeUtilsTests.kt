package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.FiscalCode
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ConfidentialFiscalCodeUtilsTests {

    private val EXAMPLE_FISCAL_CODE = "example"

    private val fiscalCodeCachedMap: MutableMap<String, FiscalCode> = mutableMapOf()

    private val confidentialDataManager: ConfidentialDataManager = mock()

    private val confidentialFiscalCodeUtils: ConfidentialFiscalCodeUtils =
        ConfidentialFiscalCodeUtils(confidentialDataManager, fiscalCodeCachedMap)

    @Test
    fun shouldDecryptFiscalCodeSuccessfully() {
        val fiscalCode = FiscalCode(EXAMPLE_FISCAL_CODE)
        val fiscalCodeToken = UUID.randomUUID()
        val computedConfidential = Confidential<FiscalCode>(fiscalCodeToken.toString())

        /* preconditions */
        given(confidentialDataManager.decrypt(eq(computedConfidential), any()))
            .willReturn(Mono.just(fiscalCode))

        /* test */
        StepVerifier.create(confidentialFiscalCodeUtils.toClearData(computedConfidential))
            .expectNext(fiscalCode)
            .verifyComplete()

        assertEquals(fiscalCode, fiscalCodeCachedMap[computedConfidential.opaqueData])
        verify(confidentialDataManager, Mockito.times(1)).decrypt(eq(computedConfidential), any())
    }

    @Test
    fun shouldDecryptFiscalCodeSuccessfullyWithCachedValue() {
        val fiscalCode = FiscalCode(EXAMPLE_FISCAL_CODE)
        val fiscalCodeToken = UUID.randomUUID()
        val computedConfidential = Confidential<FiscalCode>(fiscalCodeToken.toString())
        fiscalCodeCachedMap[computedConfidential.opaqueData] = fiscalCode

        /* test */
        StepVerifier.create(confidentialFiscalCodeUtils.toClearData(computedConfidential))
            .expectNext(fiscalCode)
            .verifyComplete()

        verify(confidentialDataManager, Mockito.times(0)).decrypt(eq(computedConfidential), any())
        assertEquals(fiscalCode, fiscalCodeCachedMap[computedConfidential.opaqueData])
    }

    @Test
    fun shouldEncryptFiscalCodeSuccessfully() {
        val fiscalCode = FiscalCode(EXAMPLE_FISCAL_CODE)
        val fiscalCodeToken = UUID.randomUUID()
        val computedConfidential = Confidential<FiscalCode>(fiscalCodeToken.toString())

        /* preconditions */
        given(confidentialDataManager.encrypt(eq(fiscalCode)))
            .willReturn(Mono.just(computedConfidential))

        /* test */
        StepVerifier.create(confidentialFiscalCodeUtils.toConfidential(fiscalCode.value))
            .expectNext(computedConfidential)
            .verifyComplete()

        verify(confidentialDataManager, Mockito.times(1)).encrypt(eq(fiscalCode))
    }
}
