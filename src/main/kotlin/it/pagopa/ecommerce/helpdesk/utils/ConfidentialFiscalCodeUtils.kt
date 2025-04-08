package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.FiscalCode
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * Class used for handling fiscal code encryption and decryption.
 *
 * @param fiscalCodeConfidentialDataManager class for executing pdv call
 * @param encryptedToClearMap Map containing the opaque fiscal code token as the key and the clear fiscal code as
 *   the value.
 */
class ConfidentialFiscalCodeUtils(
    private val fiscalCodeConfidentialDataManager: ConfidentialDataManager,
    private val encryptedToClearMap: MutableMap<String, FiscalCode> = mutableMapOf()
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * The method searches for clear fiscal code in the cache. If not found, it makes a call to pdv to decrypt the fiscal code
     * and then caches it.
     *
     * @param encrypted fiscal code encrypted
     * @return Mono<FiscalCode> returns mono with clear fiscal code value object
     */
    fun toFiscalCode(encrypted: Confidential<FiscalCode>): Mono<FiscalCode> {
        return if (encryptedToClearMap.contains(encrypted.opaqueData)) {
            logger.info("fiscal code decrypt cache hit")
            mono { encryptedToClearMap[encrypted.opaqueData] }
        } else {
            fiscalCodeConfidentialDataManager
                .decrypt(encrypted, ::FiscalCode)
                .doOnError { e -> logger.error("Exception decrypting confidential data", e) }
                .doOnNext { decryptedFiscalCode ->
                    encryptedToClearMap[encrypted.opaqueData] = decryptedFiscalCode
                }
        }
    }

    /**
     * The method used for encrypting fiscal code using pdv
     *
     * @param clearText clear fiscal code
     * @return Mono<Confidential<FiscalCode>> returns mono with encrypted fiscal code
     */
    private fun toConfidential(clearText: FiscalCode): Mono<Confidential<FiscalCode>> {
        return mono {
            encryptedToClearMap.entries
                .stream()
                .filter { it.value == clearText }
                .map { it.key }
                .findFirst()
                .map { it }
        }
            .flatMap {
                if (it.isEmpty) {
                    fiscalCodeConfidentialDataManager
                        .encrypt(clearText)
                        .doOnNext { encryptedToClearMap[it.opaqueData] = clearText }
                        .doOnError { e ->
                            logger.error("Exception encrypting confidential data", e)
                        }
                } else {
                    Mono.just(Confidential(it.get()))
                }
            }
    }

    /**
     * The method used for encrypting fiscal code using pdv
     *
     * @param clearText clear fiscal code
     * @return Mono<Confidential<FiscalCode>> returns mono with encrypted fiscal code
     */
    fun toConfidential(fiscalCode: String): Mono<Confidential<FiscalCode>> {
        return toConfidential(FiscalCode(fiscalCode))
    }
}
