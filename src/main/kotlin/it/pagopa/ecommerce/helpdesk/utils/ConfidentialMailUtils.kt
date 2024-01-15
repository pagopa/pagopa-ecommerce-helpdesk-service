package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

class ConfidentialMailUtils(private val emailConfidentialDataManager: ConfidentialDataManager) {
    private val emailCachedMap = mutableMapOf<String, Email>()

    private val logger = LoggerFactory.getLogger(javaClass)

    fun toEmail(encrypted: Confidential<Email>): Mono<Email> {
        return if (emailCachedMap.contains(encrypted.opaqueData)) {
            mono { emailCachedMap[encrypted.opaqueData] }
        } else {
            emailConfidentialDataManager
                .decrypt(encrypted, ::Email)
                .doOnError { e -> logger.error("Exception decrypting confidential data", e) }
                .map { decryptedEmail ->
                    emailCachedMap[encrypted.opaqueData] = decryptedEmail
                    decryptedEmail
                }
        }
    }

    fun toConfidential(clearText: Email): Mono<Confidential<Email>> {
        return emailConfidentialDataManager.encrypt(clearText).doOnError { e ->
            logger.error("Exception encrypting confidential data", e)
        }
    }

    fun toConfidential(email: String): Mono<Confidential<Email>> {
        return toConfidential(Email(email))
    }
}
