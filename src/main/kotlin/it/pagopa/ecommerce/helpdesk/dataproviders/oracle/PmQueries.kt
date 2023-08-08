package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

fun buildTransactionByUserEmailPaginatedQuery(userEmail: String) =
    """
        SELECT 	pu.FISCAL_CODE AS PU_FISCAL_CODE, pu.NOTIFICATION_EMAIL AS PU_NOTIFICATION_EMAIL, pu.SURNAME AS PU_SURNAME, pu.NAME AS PU_NAME, pu.USERNAME AS PU_USERNAME, pu.STATUS AS PU_STATUS,
		pt.CREATION_DATE AS PT_CREATION_DATE, pt.STATUS AS PT_STATUS ,pt.ACCOUNTING_STATUS AS PT_ACCOUNTING_STATUS , pt.AMOUNT AS PT_AMOUNT, pt.FEE AS PT_FEE, pt.GRAND_TOTAL AS PT_GRAND_TOTAL,pt.FK_PAYMENT AS PT_FK_PAYMENT,
		pp.ID AS PP_ID ,pp.AMOUNT AS PP_AMOUNT, pp.SUBJECT AS PP_SUBJECT, pp.ORIGIN AS PP_ORIGIN,
		ppd.IUV AS PPD_IUV , ppd.CCP AS PPD_CCP, ppd.ENTE_BENEFICIARIO AS PPD_ENTE_BENEFICIARIO, ppd.IMPORTO  AS PPD_IMPORTO, ppd.ID_DOMINIO AS PPD_ID_DOMINIO,
		pp2.ID_PSP AS PP2_ID_PSP, pp2.BUSINESS_NAME AS PP2_BUSINESS_NAME, pp2.ID_CHANNEL AS PP2_ID_CHANNEL
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_TRANSACTION pt ON pu.ID_USER =pt.FK_USER 
        left JOIN AGID_USER.PP_PAYMENT pp ON pt.FK_PAYMENT = pp.ID 
        left JOIN AGID_USER.PP_PAYMENT_DETAIL ppd ON pp.ID =ppd.PAYMENT_ID 
        left JOIN AGID_USER.PP_PSP pp2 ON pt.FK_PSP = pp2.ID 
        WHERE pu.NOTIFICATION_EMAIL  ='$userEmail'
        AND PT.AMOUNT > 1
        ORDER BY PT.CREATION_DATE DESC
        OFFSET %s ROWS FETCH NEXT %s ROWS ONLY
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