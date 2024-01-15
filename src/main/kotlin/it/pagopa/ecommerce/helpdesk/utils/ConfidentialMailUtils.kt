package it.pagopa.ecommerce.helpdesk.utils

import it.pagopa.ecommerce.commons.domain.Confidential
import it.pagopa.ecommerce.commons.domain.Email
import it.pagopa.ecommerce.commons.utils.ConfidentialDataManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ConfidentialMailUtils(
    @Autowired private val emailConfidentialDataManager: ConfidentialDataManager
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun toEmail(encrypted: Confidential<Email>): Mono<Email> {
        return emailConfidentialDataManager.decrypt(encrypted, ::Email).doOnError { e ->
            logger.error("Exception decrypting confidential data", e)
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
