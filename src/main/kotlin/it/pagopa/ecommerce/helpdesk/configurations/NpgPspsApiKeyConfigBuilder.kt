package it.pagopa.ecommerce.helpdesk.configurations

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.commons.client.NpgClient.PaymentMethod
import it.pagopa.ecommerce.commons.utils.NpgApiKeyConfiguration
import it.pagopa.ecommerce.commons.utils.NpgPspApiKeysConfig
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ApplicationScoped
class NpgPspsApiKeyConfigBuilder {

    private val objectMapper = ObjectMapper()

    /**
     * Return a map where valued with each psp id - api keys entries
     *
     * @param apiKeys
     * - the secret api keys configuration json
     *
     * @return the parsed map
     */
    @Named("npgCardsApiKeys")
    @Produces
    @ApplicationScoped
    fun npgCardsApiKeys(
        @ConfigProperty(name = "npg.authorization.cards.keys") apiKeys: String,
        @ConfigProperty(name = "npg.authorization.cards.pspList") pspToHandle: Set<String>
    ): NpgPspApiKeysConfig =
        parsePspApiKeyConfiguration(
            apiKeys = apiKeys,
            pspToHandle = pspToHandle,
            paymentMethod = PaymentMethod.CARDS
        )

    /**
     * Return a map where valued with each psp id - api keys entries
     *
     * @param apiKeys
     * - the secret api keys configuration json
     *
     * @return the parsed map
     */
    @Named("npgPaypalApiKeys")
    @Produces
    @ApplicationScoped
    fun npgPaypalApiKeys(
        @ConfigProperty(name = "npg.authorization.paypal.keys") apiKeys: String,
        @ConfigProperty(name = "npg.authorization.paypal.pspList") pspToHandle: Set<String>
    ): NpgPspApiKeysConfig =
        parsePspApiKeyConfiguration(
            apiKeys = apiKeys,
            pspToHandle = pspToHandle,
            paymentMethod = PaymentMethod.PAYPAL
        )

    /**
     * Return a map where valued with each psp id - api keys entries
     *
     * @param apiKeys
     * - the secret api keys configuration json
     *
     * @return the parsed map
     */
    @Named("npgBancomatPayApiKeys")
    @Produces
    @ApplicationScoped
    fun npgBancomatPayApiKeys(
        @ConfigProperty(name = "npg.authorization.bancomatpay.keys") apiKeys: String,
        @ConfigProperty(name = "npg.authorization.bancomatpay.pspList") pspToHandle: Set<String>
    ): NpgPspApiKeysConfig =
        parsePspApiKeyConfiguration(
            apiKeys = apiKeys,
            pspToHandle = pspToHandle,
            paymentMethod = PaymentMethod.BANCOMATPAY
        )

    /**
     * Return a map where valued with each psp id - api keys entries
     *
     * @param apiKeys
     * - the secret api keys configuration json
     *
     * @return the parsed map
     */
    @Named("npgMyBankApiKeys")
    @Produces
    @ApplicationScoped
    fun npgMyBankApiKeys(
        @ConfigProperty(name = "npg.authorization.mybank.keys") apiKeys: String,
        @ConfigProperty(name = "npg.authorization.mybank.pspList") pspToHandle: Set<String>
    ): NpgPspApiKeysConfig =
        parsePspApiKeyConfiguration(
            apiKeys = apiKeys,
            pspToHandle = pspToHandle,
            paymentMethod = PaymentMethod.MYBANK
        )

    /**
     * Return a map where valued with each psp id - api keys entries
     *
     * @param apiKeys
     * - the secret api keys configuration json
     *
     * @return the parsed map
     */
    @Named("npgApplePayApiKeys")
    @Produces
    @ApplicationScoped
    fun npgApplePayApiKeys(
        @ConfigProperty(name = "npg.authorization.applepay.keys") apiKeys: String,
        @ConfigProperty(name = "npg.authorization.applepay.pspList") pspToHandle: Set<String>
    ): NpgPspApiKeysConfig =
        parsePspApiKeyConfiguration(
            apiKeys = apiKeys,
            pspToHandle = pspToHandle,
            paymentMethod = PaymentMethod.APPLEPAY
        )

    /**
     * Return a map where valued with each psp id - api keys entries
     *
     * @param apiKeys
     * - the secret api keys configuration json
     *
     * @return the parsed map
     */
    @Named("npgSatispayApiKeys")
    @Produces
    @ApplicationScoped
    fun npgSatispayApiKeys(
        @ConfigProperty(name = "npg.authorization.satispay.keys") apiKeys: String,
        @ConfigProperty(name = "npg.authorization.satispay.pspList") pspToHandle: Set<String>
    ): NpgPspApiKeysConfig =
        parsePspApiKeyConfiguration(
            apiKeys = apiKeys,
            pspToHandle = pspToHandle,
            paymentMethod = PaymentMethod.SATISPAY
        )

    /**
     * Return a map where valued with each psp id - api keys entries
     *
     * @param apiKeys
     * - the secret api keys configuration json
     *
     * @return the parsed map
     */
    @Named("npgGooglePayApiKeys")
    @Produces
    @ApplicationScoped
    fun npgGooglePayApiKeys(
        @ConfigProperty(name = "npg.authorization.googlepay.keys") apiKeys: String,
        @ConfigProperty(name = "npg.authorization.googlepay.pspList") pspToHandle: Set<String>
    ): NpgPspApiKeysConfig =
        parsePspApiKeyConfiguration(
            apiKeys = apiKeys,
            pspToHandle = pspToHandle,
            paymentMethod = PaymentMethod.GOOGLEPAY
        )

    /*
     * @formatter:off
     *
     * Warning kotlin:S107 - Functions should not have too many parameters
     * Suppressed because there is a bean per NPG payment method as per secret management choice.
     * Moreover, this is a factory method that is never called programmatically
     *
     * @formatter:on
     */
    @SuppressWarnings("kotlin:S107")
    @Produces
    @ApplicationScoped
    fun npgApiKeyHandler(
        npgCardsApiKeys: NpgPspApiKeysConfig,
        npgPaypalApiKeys: NpgPspApiKeysConfig,
        npgBancomatPayApiKeys: NpgPspApiKeysConfig,
        npgMyBankApiKeys: NpgPspApiKeysConfig,
        npgApplePayApiKeys: NpgPspApiKeysConfig,
        npgSatispayApiKeys: NpgPspApiKeysConfig,
        npgGooglePayApiKeys: NpgPspApiKeysConfig,
        @ConfigProperty(name = "npg.client.apiKey") defaultApiKey: String
    ) =
        NpgApiKeyConfiguration.Builder()
            .setDefaultApiKey(defaultApiKey)
            .withMethodPspMapping(PaymentMethod.CARDS, npgCardsApiKeys)
            .withMethodPspMapping(PaymentMethod.PAYPAL, npgPaypalApiKeys)
            .withMethodPspMapping(PaymentMethod.MYBANK, npgMyBankApiKeys)
            .withMethodPspMapping(PaymentMethod.BANCOMATPAY, npgBancomatPayApiKeys)
            .withMethodPspMapping(PaymentMethod.SATISPAY, npgSatispayApiKeys)
            .withMethodPspMapping(PaymentMethod.APPLEPAY, npgApplePayApiKeys)
            .withMethodPspMapping(PaymentMethod.GOOGLEPAY, npgGooglePayApiKeys)
            .build()

    private fun parsePspApiKeyConfiguration(
        apiKeys: String,
        pspToHandle: Set<String>,
        paymentMethod: PaymentMethod
    ) =
        NpgPspApiKeysConfig.parseApiKeyConfiguration(
                apiKeys,
                pspToHandle,
                paymentMethod,
                objectMapper
            )
            .fold({ throw it }, { it })
}
