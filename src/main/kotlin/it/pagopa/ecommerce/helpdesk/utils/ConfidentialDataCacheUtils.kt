package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

abstract class ConfidentialDataCacheUtils<T>(
    private val confidentialDataManager: ConfidentialDataManager,
    private val encryptedToClearMap: MutableMap<String, T> = mutableMapOf(),
    private val confidentialFromClearData: (String) -> T
) where T : ConfidentialDataManager.ConfidentialData {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * The method search clear data into cache if not found make a call to pdv to decrypt the data
     * then cache it
     *
     * @param encrypted encrypted data to be search
     * @return Mono<T> return mono with clear data value object
     */
    fun toClearData(encrypted: Confidential<T>): Mono<T> {
        return if (encryptedToClearMap.contains(encrypted.opaqueData)) {
            logger.info("confidential data cache hit")
            mono { encryptedToClearMap[encrypted.opaqueData] }
        } else {
            confidentialDataManager
                .decrypt(encrypted, confidentialFromClearData)
                .doOnError { e -> logger.error("Exception decrypting confidential data", e) }
                .doOnNext { decryptedData ->
                    encryptedToClearMap[encrypted.opaqueData] = decryptedData
                }
        }
    }

    /**
     * The method used for encrypt data using pdv
     *
     * @param clearText clear data
     * @return Mono<Confidential<T>> return mono with encrypted data
     */
    private fun toConfidential(clearText: T): Mono<Confidential<T>> {
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
                    confidentialDataManager
                        .encrypt(clearText)
                        .doOnNext { encrypted ->
                            encryptedToClearMap[encrypted.opaqueData] = clearText
                        }
                        .doOnError { e ->
                            logger.error("Exception encrypting confidential data", e)
                        }
                } else {
                    Mono.just(Confidential(it.get()))
                }
            }
    }

    /**
     * The method used for encrypt data using pdv
     *
     * @param clearData the clear data, not encrypted
     * @return Mono<Confidential<T>> return mono with encrypted PDV data
     */
    fun toConfidential(clearData: String): Mono<Confidential<T>> {
        return toConfidential(confidentialFromClearData(clearData))
    }
}
