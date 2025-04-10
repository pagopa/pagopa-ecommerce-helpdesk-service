package it.pagopa.ecommerce.helpdesk.configurations

import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import it.pagopa.generated.pdv.v1.ApiClient
import it.pagopa.generated.pdv.v1.api.TokenApi
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebClientsConfig {

    @Bean
    @Qualifier("pdv-mail-client")
    fun personalDataVaultApiClientEmail(
        @Value("\${confidentialDataManager.personalDataVaultEmail.apiKey}")
        personalDataVaultApiKey: String,
        @Value("\${confidentialDataManager.personalDataVault.apiBasePath}") apiBasePath: String
    ): TokenApi {
        val pdvApiClient = ApiClient()
        pdvApiClient.setApiKey(personalDataVaultApiKey)
        pdvApiClient.basePath = apiBasePath
        return TokenApi(pdvApiClient)
    }

    @Bean
    @Qualifier("pdv-fiscal-code-client")
    fun personalDataVaultApiClientFiscalCode(
        @Value("\${confidentialDataManager.personalDataVaultFiscalCode.apiKey}")
        personalDataVaultApiKey: String,
        @Value("\${confidentialDataManager.personalDataVault.apiBasePath}") apiBasePath: String
    ): TokenApi {
        val pdvApiClient = ApiClient()
        pdvApiClient.setApiKey(personalDataVaultApiKey)
        pdvApiClient.basePath = apiBasePath
        return TokenApi(pdvApiClient)
    }

    @Bean
    @Qualifier("confidential-data-manager-client-email")
    fun emailConfidentialDataManager(
        @Qualifier("pdv-mail-client") personalDataVaultApi: TokenApi
    ): ConfidentialDataManager {
        return ConfidentialDataManager(personalDataVaultApi)
    }

    @Bean
    @Qualifier("confidential-data-manager-client-fiscal-code")
    fun fiscalCodeConfidentialDataManager(
        @Qualifier("pdv-fiscal-code-client") personalDataVaultApi: TokenApi
    ): ConfidentialDataManager {
        return ConfidentialDataManager(personalDataVaultApi)
    }
}
