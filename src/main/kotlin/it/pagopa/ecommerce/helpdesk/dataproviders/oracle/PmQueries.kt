package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

fun buildTransactionByUserEmailPaginatedQuery(userEmail: String, offset: Int, limit: Int) =
    """
        SELECT 	pu.FISCAL_CODE , pu.NOTIFICATION_EMAIL , pu.SURNAME , pu.NAME , pu.USERNAME , pu.STATUS ,
        		pt.CREATION_DATE , pt.STATUS ,pt.ACCOUNTING_STATUS , pt.AMOUNT , pt.FEE , pt.GRAND_TOTAL ,pt.FK_PAYMENT ,
        		pp.ID  ,pp.AMOUNT , pp.SUBJECT , pp.ORIGIN ,
        		ppd.IUV , ppd.CCP, ppd.ENTE_BENEFICIARIO , ppd.IMPORTO  , ppd.ID_DOMINIO ,
        		pp2.ID_PSP , pp2.BUSINESS_NAME , pp2.ID_CHANNEL 
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_TRANSACTION pt ON pu.ID_USER =pt.FK_USER 
        left JOIN AGID_USER.PP_PAYMENT pp ON pt.FK_PAYMENT = pp.ID 
        left JOIN AGID_USER.PP_PAYMENT_DETAIL ppd ON pp.ID =ppd.PAYMENT_ID 
        left JOIN AGID_USER.PP_PSP pp2 ON pt.FK_PSP = pp2.ID 
        WHERE pu.NOTIFICATION_EMAIL  ='$userEmail'
        AND PT.AMOUNT > 1
        ORDER BY PT.CREATION_DATE DESC
        OFFSET $offset ROWS FETCH NEXT $limit ROWS ONLY
    """
        .trimIndent()

fun buildTransactionByUserEmailCountQuery(userEmail: String) =
    """
        SELECT COUNT(*)
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_TRANSACTION pt ON pu.ID_USER =pt.FK_USER 
        left JOIN AGID_USER.PP_PAYMENT pp ON pt.FK_PAYMENT = pp.ID 
        left JOIN AGID_USER.PP_PAYMENT_DETAIL ppd ON pp.ID =ppd.PAYMENT_ID 
        left JOIN AGID_USER.PP_PSP pp2 ON pt.FK_PSP = pp2.ID 
        WHERE pu.NOTIFICATION_EMAIL  ='$userEmail'
        AND PT.AMOUNT > 1
    """
        .trimIndent()
