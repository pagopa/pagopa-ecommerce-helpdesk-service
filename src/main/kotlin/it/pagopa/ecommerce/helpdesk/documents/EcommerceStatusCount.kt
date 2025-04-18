package it.pagopa.ecommerce.helpdesk.documents

import org.springframework.data.mongodb.core.mapping.Field

data class EcommerceStatusCount(@Field("_id") val status: String, val count: Int)
