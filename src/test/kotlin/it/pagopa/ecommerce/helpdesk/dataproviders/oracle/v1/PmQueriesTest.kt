package it.pagopa.ecommerce.helpdesk.dataproviders.oracle.v1

import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.timestampRangeCountQuery
import it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle.timestampRangePaginatedQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PmQueriesTest {

    @Test
    fun `timestampRangePaginatedQuery should be properly constructed and trimmed`() {
        val expectedQuery =
            """
       SELECT  
           pu.FISCAL_CODE, 
           pu.NOTIFICATION_EMAIL, 
           pu.SURNAME, 
           pu.NAME, 
           pu.USERNAME, 
           pu.STATUS as PU_STATUS,
           pt.CREATION_DATE, 
           pt.STATUS as PT_STATUS,
           pt.ACCOUNTING_STATUS,
           PP.ORIGIN,
           pt.AMOUNT, 
           pt.FEE, 
           pt.GRAND_TOTAL,
           pt.rrn,  
           pt.AUTHORIZATION_CODE,
           pt.SERVICE_NAME,
           pp.SUBJECT,
           ppd.IUV, 
           ppd.CCP, 
           ppd.ENTE_BENEFICIARIO, 
           ppd.ID_DOMINIO,
           pp2.ID_PSP, 
           pp2.BUSINESS_NAME, 
           pp2.ID_CHANNEL
       FROM AGID_USER.PP_PAYMENT pp
       LEFT JOIN AGID_USER.PP_TRANSACTION pt 
           ON pt.FK_PAYMENT = pp.ID 
       LEFT JOIN AGID_USER.PP_USER pu 
           ON pu.ID_USER = pt.FK_USER 
       LEFT JOIN AGID_USER.PP_PAYMENT_DETAIL ppd 
           ON pp.ID = ppd.PAYMENT_ID 
       LEFT JOIN AGID_USER.PP_PSP pp2 
           ON pt.FK_PSP = pp2.ID 
       WHERE pp.CREATION_DATE BETWEEN ? AND ?
       OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
   """
                .trimIndent()

        assertEquals(expectedQuery, timestampRangePaginatedQuery)
    }

    @Test
    fun `timestampRangeCountQuery should be properly constructed and trimmed`() {
        val expectedQuery =
            """
        SELECT COUNT(*)
        FROM AGID_USER.PP_PAYMENT pp
        LEFT JOIN AGID_USER.PP_TRANSACTION pt 
            ON pt.FK_PAYMENT = pp.ID 
        LEFT JOIN AGID_USER.PP_USER pu 
            ON pu.ID_USER = pt.FK_USER 
        LEFT JOIN AGID_USER.PP_PAYMENT_DETAIL ppd 
            ON pp.ID = ppd.PAYMENT_ID 
        LEFT JOIN AGID_USER.PP_PSP pp2 
            ON pt.FK_PSP = pp2.ID 
        WHERE pp.CREATION_DATE BETWEEN ? AND ?
    """
                .trimIndent()

        assertEquals(expectedQuery, timestampRangeCountQuery)
    }
}
