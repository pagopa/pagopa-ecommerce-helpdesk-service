package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

import io.r2dbc.spi.ConnectionFactory


fun getResultSetFromPaginatedQuery(
    connectionFactory: ConnectionFactory,
    totalRecordCountQuery: String,
    resultQuery: String
) {

}