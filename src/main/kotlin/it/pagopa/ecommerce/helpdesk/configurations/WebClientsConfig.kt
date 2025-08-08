package it.pagopa.ecommerce.helpdesk.configurations

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.generated.pdv.v1.ApiClient
import it.pagopa.generated.pdv.v1.api.TokenApi
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Named
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ApplicationScoped
class WebClientsConfig {

    @Produces
    @ApplicationScoped
    @Named("pdv-mail-client")
    fun personalDataVaultApiClientEmail(
        @ConfigProperty(name = "confidentialDataManager.personalDataVaultEmail.apiKey")
        personalDataVaultApiKey: String,
        @ConfigProperty(name = "confidentialDataManager.personalDataVault.apiBasePath") apiBasePath: String
    ): TokenApi {
        val pdvApiClient = ApiClient()
        pdvApiClient.setApiKey(personalDataVaultApiKey)
        pdvApiClient.basePath = apiBasePath
        return TokenApi(pdvApiClient)
    }

    @Produces
    @ApplicationScoped
    @Named("pdv-fiscal-code-client")
    fun personalDataVaultApiClientFiscalCode(
        @ConfigProperty(name = "confidentialDataManager.personalDataVaultFiscalCode.apiKey")
        personalDataVaultApiKey: String,
        @ConfigProperty(name = "confidentialDataManager.personalDataVault.apiBasePath") apiBasePath: String
    ): TokenApi {
        val pdvApiClient = ApiClient()
        pdvApiClient.setApiKey(personalDataVaultApiKey)
        pdvApiClient.basePath = apiBasePath
        return TokenApi(pdvApiClient)
    }

    @Produces
    @ApplicationScoped
    @Named("confidential-data-manager-client-email")
    fun emailConfidentialDataManager(
        @Named("pdv-mail-client") personalDataVaultApi: TokenApi
    ): ConfidentialDataManager {
        return ConfidentialDataManager(personalDataVaultApi)
    }

    @Produces
    @ApplicationScoped
    @Named("confidential-data-manager-client-fiscal-code")
    fun fiscalCodeConfidentialDataManager(
        @Named("pdv-fiscal-code-client") personalDataVaultApi: TokenApi
    ): ConfidentialDataManager {
        return ConfidentialDataManager(personalDataVaultApi)
    }
}
