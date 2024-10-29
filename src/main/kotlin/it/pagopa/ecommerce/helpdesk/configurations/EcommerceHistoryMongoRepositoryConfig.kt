package it.pagopa.ecommerce.helpdesk.configurations

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@EnableReactiveMongoRepositories(
    basePackages = ["it.pagopa.ecommerce.helpdesk.dataproviders.repositories.history"],
    reactiveMongoTemplateRef = "ecommerceHistoryReactiveMongoTemplate"
)
class EcommerceHistoryMongoRepositoryConfig {}
