package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * Class used for handling encryption and decryption of confidential data.
 *
 * @param confidentialDataManager class for executing pdv calls
 * @param encryptedToClearMap Map containing the opaque token as the key and the clear data as the
 *   value.
 */
class ConfidentialDataUtils<T>(
    private val confidentialDataManager: ConfidentialDataManager,
    private val encryptedToClearMap: MutableMap<String, T> = mutableMapOf()
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * The method searches clear data in the cache. If not found, it makes a call to pdv to decrypt
     * the data and then caches it.
     *
     * @param encrypted data encrypted
     * @return Mono<T> return mono with clear data value object
     */
    fun toClearData(encrypted: Confidential<T>): Mono<T> {
        return if (encryptedToClearMap.contains(encrypted.opaqueData)) {
            logger.info("Data decrypt cache hit")
            mono { encryptedToClearMap[encrypted.opaqueData] }
        } else {
            confidentialDataManager
                .decrypt(encrypted)
                .doOnError { e -> logger.error("Exception decrypting confidential data", e) }
                .doOnNext { decryptedData ->
                    encryptedToClearMap[encrypted.opaqueData] = decryptedData
                }
        }
    }

    /**
     * The method used for encrypting data using pdv
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
     * The method used for encrypting data using pdv
     *
     * @param clearText clear data
     * @return Mono<Confidential<T>> return mono with encrypted data
     */
    fun toConfidential(data: T): Mono<Confidential<T>> {
        return toConfidential(data)
    }
}
