package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import it.pagopa.ecommerce.helpdesk.exceptions.NoResultFoundException
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.OffsetDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Hooks
import reactor.test.StepVerifier

class PMPaymentMethodsDataProviderTest {
    private val connectionFactory =
        H2ConnectionFactory(
            H2ConnectionConfiguration.builder()
                .inMemory("...")
                .option("INIT=runscript from './src/test/resources/h2scriptFile.sql'")
                .build()
        )

    private val pmPaymentMethodDataProvider = PMPaymentMethodsDataProvider(connectionFactory)

    @Test
    fun `Should find wallets for an user searching by email`() {
        val expectedResponse =
            SearchPaymentMethodResponseDto()
                .name("name")
                .fiscalCode("fiscal_code")
                .notificationEmail("test@test.it")
                .surname("surname")
                .username("username")
                .status("Utente registrato SPID")
                .addPaymentMethodsItem(
                    BancomatDetailInfoDto()
                        .type(DetailTypeDto.BANCOMAT.value)
                        .creationDate(OffsetDateTime.parse("2000-10-22T19:03:25.979+02:00"))
                        .bancomatNumber("cardPartialNumber")
                        .bancomatAbi("abi")
                )
                .addPaymentMethodsItem(
                    BankAccountDetailInfoDto()
                        .type(DetailTypeDto.BANK_ACCOUNT.value)
                        .creationDate(OffsetDateTime.parse("2000-10-22T19:03:25.979+02:00"))
                        .bankName("alias")
                        .bankState("state")
                )
                .addPaymentMethodsItem(
                    BpayDetailInfoDto()
                        .type(DetailTypeDto.BPAY.value)
                        .creationDate(OffsetDateTime.parse("2000-10-22T19:03:25.979+02:00"))
                        .bpayPhoneNumber("cellPhoneNumber")
                        .bpayName("bankName")
                        .idPsp("CCRTIT2TCAR")
                )
                .addPaymentMethodsItem(
                    CardDetailInfoDto()
                        .type(DetailTypeDto.CARD.value)
                        .creationDate(OffsetDateTime.parse("2000-10-22T19:03:25.979+02:00"))
                        .idPsp("CCRTIT2TCAR")
                        .cardBin("1234")
                        .cardNumber("*************1234")
                )
                .addPaymentMethodsItem(
                    GenericMethodDetailInfoDto()
                        .type(DetailTypeDto.GENERIC_METHOD.value)
                        .creationDate(OffsetDateTime.parse("2000-10-22T19:03:25.979+02:00"))
                        .description("Generic instrument")
                )
                .addPaymentMethodsItem(
                    PaypalDetailInfoDto()
                        .type(DetailTypeDto.PAYPAL.value)
                        .creationDate(OffsetDateTime.parse("2000-10-22T19:03:25.979+02:00"))
                        .ppayEmail("test@test.it")
                )
                .addPaymentMethodsItem(
                    SatispayDetailInfoDto()
                        .type(DetailTypeDto.SATISPAY.value)
                        .creationDate(OffsetDateTime.parse("2000-10-22T19:03:25.979+02:00"))
                        .idPsp("CCRTIT2TCAR")
                )

        val searchParam =
            SearchPaymentMethodRequestEmailDto().type("email").userEmail("test@test.it")
        StepVerifier.create(pmPaymentMethodDataProvider.findResult(searchParam))
            .assertNext { assertEquals(expectedResponse, it) }
            .verifyComplete()
    }

    @Test
    fun `Should not find wallets for an user searching by unknown email`() {
        Hooks.onOperatorDebug()
        val searchParam =
            SearchPaymentMethodRequestEmailDto().type("email").userEmail("test@notexist.it")
        StepVerifier.create(pmPaymentMethodDataProvider.findResult(searchParam))
            .expectError(NoResultFoundException::class.java)
            .verify()
    }
}
