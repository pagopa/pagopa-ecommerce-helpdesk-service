package it.pagopa.ecommerce.helpdesk.dataproviders.oracle

val userEmailPaginatedQuery =
    """
        SELECT 	pu.FISCAL_CODE , pu.NOTIFICATION_EMAIL , pu.SURNAME , pu.NAME , pu.USERNAME , 
		CASE pu.STATUS 
		WHEN 0 THEN 'Utente non registrato'
		WHEN 1 THEN 'Utente registrato non SPID'
		WHEN 2 THEN 'Utente in attesa di verifica OTP'
		WHEN 3 THEN 'Password da impostare'
		WHEN 4 THEN 'Password da impostare - cambio password'
		WHEN 5 THEN 'Utente cancellato'
		WHEN 11 THEN 'Utente registrato SPID'
		WHEN 12 THEN 'Utente registrato su IO con CIE'
		ELSE TO_CHAR(pu.STATUS) END AS STATUS_UTENTE,
		pt.CREATION_DATE , 
		CASE pt.STATUS 
		WHEN 0 THEN 'Da autorizzare'
		WHEN 1 THEN 'In attesa'
		WHEN 2 THEN 'In attesa mod1'
		WHEN 3 THEN 'Confermato'
		WHEN 4 THEN 'Rifiutato'
		WHEN 6 THEN 'In attesa di XPAY'
		WHEN 7 THEN 'In errore'
		WHEN 8 THEN 'Confermato mod1'
		WHEN 9 THEN 'Confermato mod2'
		WHEN 10 THEN 'Rifiutato'
		WHEN 11 THEN 'Missing callback from PSP'
		WHEN 12 THEN 'Pagamento preso in carico'
		WHEN 13 THEN '3DS Scaduto'
		WHEN 14 THEN 'Authorized with nodo timeout'
		WHEN 15 THEN 'In attesa del metodo 3ds2'
		WHEN 16 THEN 'In attesa della challenge 3ds2'
		WHEN 17 THEN 'Ritornando dal metodo 3ds2'
		WHEN 18 THEN 'Ritornando dalla challenge 3ds2'
		WHEN 19 THEN 'Transazione XPAY / PPAL / BPAY da stornare'
		WHEN 20 THEN 'Transazione XPAY stornata da batch'
		WHEN 21 THEN 'Pagamento Autorizzato dal Payment Gateway'
		ELSE TO_CHAR(pt.STATUS) END AS STATUS_PAGAMENTO,
		CASE pt.ACCOUNTING_STATUS 
		WHEN 0 THEN 'Non gestito'
		WHEN 1 THEN 'Contabilizzato'
		WHEN 2 THEN 'Errore di contabilizzazione'
		WHEN 3 THEN 'Stornato'
		WHEN 4 THEN 'Errore Storno'
 		WHEN 5 THEN 'Ricevuta creata'
		ELSE TO_CHAR(pt.ACCOUNTING_STATUS ) END AS SATUS_PAGAMENTO_DETAIL, 
		PP.ORIGIN ,
		pt.AMOUNT , pt.FEE , pt.GRAND_TOTAL ,pt.rrn,  pt.AUTHORIZATION_CODE ,
		pt.SERVICE_NAME ,
		pp.SUBJECT ,ppd.IUV , ppd.CCP, ppd.ENTE_BENEFICIARIO , ppd.ID_DOMINIO ,
		pp2.ID_PSP , pp2.BUSINESS_NAME , pp2.ID_CHANNEL 
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_TRANSACTION pt ON pu.ID_USER =pt.FK_USER 
        left JOIN AGID_USER.PP_PAYMENT pp ON pt.FK_PAYMENT = pp.ID 
        left JOIN AGID_USER.PP_PAYMENT_DETAIL ppd ON pp.ID =ppd.PAYMENT_ID 
        left JOIN AGID_USER.PP_PSP pp2 ON pt.FK_PSP = pp2.ID 
        WHERE pu.NOTIFICATION_EMAIL = ?
        AND PT.AMOUNT > 1
        ORDER BY PT.CREATION_DATE DESC
        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
    """
        .trimIndent()

val userEmailCountQuery =
    """
        SELECT COUNT(*)
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_TRANSACTION pt ON pu.ID_USER =pt.FK_USER 
        left JOIN AGID_USER.PP_PAYMENT pp ON pt.FK_PAYMENT = pp.ID 
        left JOIN AGID_USER.PP_PAYMENT_DETAIL ppd ON pp.ID =ppd.PAYMENT_ID 
        left JOIN AGID_USER.PP_PSP pp2 ON pt.FK_PSP = pp2.ID 
        WHERE pu.NOTIFICATION_EMAIL = ?
        AND PT.AMOUNT > 1
    """
        .trimIndent()

val userFiscalCodePaginatedQuery =
    """
        SELECT 	pu.FISCAL_CODE , pu.NOTIFICATION_EMAIL , pu.SURNAME , pu.NAME , pu.USERNAME , 
		CASE pu.STATUS 
		WHEN 0 THEN 'Utente non registrato'
		WHEN 1 THEN 'Utente registrato non SPID'
		WHEN 2 THEN 'Utente in attesa di verifica OTP'
		WHEN 3 THEN 'Password da impostare'
		WHEN 4 THEN 'Password da impostare - cambio password'
		WHEN 5 THEN 'Utente cancellato'
		WHEN 11 THEN 'Utente registrato SPID'
		WHEN 12 THEN 'Utente registrato su IO con CIE'
		ELSE TO_CHAR(pu.STATUS) END AS STATUS_UTENTE,
		pt.CREATION_DATE ,
		CASE pt.STATUS 
		WHEN 0 THEN 'Da autorizzare'
		WHEN 1 THEN 'In attesa'
		WHEN 2 THEN 'In attesa mod1'
		WHEN 3 THEN 'Confermato'
		WHEN 4 THEN 'Rifiutato'
		WHEN 6 THEN 'In attesa di XPAY'
		WHEN 7 THEN 'In errore'
		WHEN 8 THEN 'Confermato mod1'
		WHEN 9 THEN 'Confermato mod2'
		WHEN 10 THEN 'Rifiutato'
		WHEN 11 THEN 'Missing callback from PSP'
		WHEN 12 THEN 'Pagamento preso in carico'
		WHEN 13 THEN '3DS Scaduto'
		WHEN 14 THEN 'Authorized with nodo timeout'
		WHEN 15 THEN 'In attesa del metodo 3ds2'
		WHEN 16 THEN 'In attesa della challenge 3ds2'
		WHEN 17 THEN 'Ritornando dal metodo 3ds2'
		WHEN 18 THEN 'Ritornando dalla challenge 3ds2'
		WHEN 19 THEN 'Transazione XPAY / PPAL / BPAY da stornare'
		WHEN 20 THEN 'Transazione XPAY stornata da batch'
		WHEN 21 THEN 'Pagamento Autorizzato dal Payment Gateway'
		ELSE TO_CHAR(pt.STATUS) END AS STATUS_PAGAMENTO,
		CASE pt.ACCOUNTING_STATUS 
		WHEN 0 THEN 'Non gestito'
		WHEN 1 THEN 'Contabilizzato'
		WHEN 2 THEN 'Errore di contabilizzazione'
		WHEN 3 THEN 'Stornato'
		WHEN 4 THEN 'Errore Storno'
 		WHEN 5 THEN 'Ricevuta creata'
		ELSE TO_CHAR(pt.ACCOUNTING_STATUS ) END AS SATUS_PAGAMENTO_DETAIL, 
		PP.ORIGIN ,
 		pt.AMOUNT , pt.FEE , pt.GRAND_TOTAL ,pt.rrn, pt.AUTHORIZATION_CODE , 
        pt.SERVICE_NAME ,
		pp.SUBJECT , ppd.IUV , ppd.CCP, ppd.ENTE_BENEFICIARIO , ppd.ID_DOMINIO ,
		pp2.ID_PSP , pp2.BUSINESS_NAME , pp2.ID_CHANNEL 
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_TRANSACTION pt ON pu.ID_USER =pt.FK_USER 
        left JOIN AGID_USER.PP_PAYMENT pp ON pt.FK_PAYMENT = pp.ID 
        left JOIN AGID_USER.PP_PAYMENT_DETAIL ppd ON pp.ID =ppd.PAYMENT_ID 
        left JOIN AGID_USER.PP_PSP pp2 ON pt.FK_PSP = pp2.ID 
        WHERE pu.FISCAL_CODE = ?
        AND PT.AMOUNT > 1
        AND pu.STATUS IN ('11', '12')
        ORDER BY PT.CREATION_DATE DESC 
        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
    """
        .trimIndent()

val userFiscalCodeCountQuery =
    """
        SELECT COUNT(*)
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_TRANSACTION pt ON pu.ID_USER =pt.FK_USER 
        left JOIN AGID_USER.PP_PAYMENT pp ON pt.FK_PAYMENT = pp.ID 
        left JOIN AGID_USER.PP_PAYMENT_DETAIL ppd ON pp.ID =ppd.PAYMENT_ID 
        left JOIN AGID_USER.PP_PSP pp2 ON pt.FK_PSP = pp2.ID 
        WHERE pu.FISCAL_CODE = ?
        AND PT.AMOUNT > 1
        AND pu.STATUS IN ('11', '12')
    """
        .trimIndent()
