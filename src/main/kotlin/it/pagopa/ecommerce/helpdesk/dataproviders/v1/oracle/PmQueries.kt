package it.pagopa.ecommerce.helpdesk.dataproviders.v1.oracle

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

// note: the conversion from Java's OffsetTime to timestamp for the parameters is handled by oracle
// through jdbc
val timestampRangePaginatedQuery =
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

val timestampRangeCountQuery =
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

val searchWalletByUserFiscalCode =
    """
    SELECT  pu.FISCAL_CODE , pu.NOTIFICATION_EMAIL , pu.SURNAME , pu.NAME , pu.USERNAME , 
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
		pw."TYPE" ,
		pw.CREATION_DATE , pp.ID_PSP , pp.BUSINESS_NAME ,
		pw.FK_CREDIT_CARD, pcc.CARD_BIN , pcc.CARD_NUMBER ,
		pw.FK_BUYER_BANK, pmbb.ALIAS as MYBANK, pmbb.STATE AS MYBANK_STATE ,
		pw.FK_BANCOMAT_CARD, pbc.ABI as BANCOMAT_ABI, pbc.CARD_PARTIAL_NUMBER AS BANCOMAT_NUMBER,
		ps.ID_SATISPAY AS FK_SATISPAY,
		pw.FK_BPAY, pb.BANK_NAME AS BPAY_NAME ,pb.CELLPHONE_NUMBER AS BPAY_NUMBER,
		pw.FK_GENERIC_INSTRUMENT, pgi.DESCRIPTION AS GENERIC_INSTRUMENT_DESCRIPTION,
		pp2.ID  AS FK_PPAL, pp2.EMAIL_PP AS PPAL_EMAIL 
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_WALLET pw ON pu.ID_USER =pw.ID_USER 
        left  JOIN AGID_USER.PP_CREDIT_CARD pcc  ON pw.FK_CREDIT_CARD  = pcc.ID_CREDIT_CARD 
        left JOIN AGID_USER.PP_MYBANK_BUYER_BANK pmbb  ON pw.FK_BUYER_BANK  = pmbb.ID_BUYER_BANK 
        left JOIN AGID_USER.PP_BANCOMAT_CARD pbc  ON pw.FK_BANCOMAT_CARD  = pbc.ID_BANCOMAT_CARD 
        left JOIN AGID_USER.PP_SATISPAY ps  ON pw.FK_SATISPAY = ps.ID_SATISPAY AND pw.FK_SATISPAY IS NOT null
        left JOIN AGID_USER.PP_BPAY pb  ON pw.FK_BPAY  = pb.ID_BPAY 
        left JOIN AGID_USER.PP_GENERIC_INSTRUMENT pgi  ON pw.FK_GENERIC_INSTRUMENT  = pgi.ID 
        left JOIN AGID_USER.PP_PSP pp  ON pw.FK_PSP  =  pp.ID 
        LEFT JOIN AGID_USER.PP_PAYPAL pp2 ON pw.ID_WALLET =pp2.FK_WALLET AND pp2.IS_CANCELED = '0'
        WHERE pu.FISCAL_CODE = ?
        AND pu.STATUS IN ('11', '12')
        AND pw.FL_ENABLED ='1' AND pw.FL_VISIBLE ='1'
        AND ((pw.FK_SATISPAY IS NULL AND pp.ID_PSP != 'SATYLUL1') OR (pw.FK_SATISPAY IS NULL AND pp.ID_PSP IS NULL) OR  (pw.FK_SATISPAY IS NOT NULL ))
        ORDER BY Pw.CREATION_DATE DESC 
    """
        .trimIndent()

val searchWalletByUserEmail =
    """
    SELECT  pu.FISCAL_CODE , pu.NOTIFICATION_EMAIL , pu.SURNAME , pu.NAME , pu.USERNAME , 
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
		pw."TYPE" ,
		pw.CREATION_DATE , pp.ID_PSP , pp.BUSINESS_NAME ,
		pw.FK_CREDIT_CARD, pcc.CARD_BIN , pcc.CARD_NUMBER ,
		pw.FK_BUYER_BANK, pmbb.ALIAS as MYBANK, pmbb.STATE AS MYBANK_STATE ,
		pw.FK_BANCOMAT_CARD, pbc.ABI as BANCOMAT_ABI, pbc.CARD_PARTIAL_NUMBER AS BANCOMAT_NUMBER,
		ps.ID_SATISPAY AS FK_SATISPAY,
		pw.FK_BPAY, pb.BANK_NAME AS BPAY_NAME ,pb.CELLPHONE_NUMBER AS BPAY_NUMBER,
		pw.FK_GENERIC_INSTRUMENT, pgi.DESCRIPTION AS GENERIC_INSTRUMENT_DESCRIPTION,
		pp2.ID  AS FK_PPAL, pp2.EMAIL_PP AS PPAL_EMAIL 
        FROM AGID_USER.PP_USER pu 
        left JOIN AGID_USER.PP_WALLET pw ON pu.ID_USER =pw.ID_USER 
        left  JOIN AGID_USER.PP_CREDIT_CARD pcc  ON pw.FK_CREDIT_CARD  = pcc.ID_CREDIT_CARD 
        left JOIN AGID_USER.PP_MYBANK_BUYER_BANK pmbb  ON pw.FK_BUYER_BANK  = pmbb.ID_BUYER_BANK 
        left JOIN AGID_USER.PP_BANCOMAT_CARD pbc  ON pw.FK_BANCOMAT_CARD  = pbc.ID_BANCOMAT_CARD 
        left JOIN AGID_USER.PP_SATISPAY ps  ON pw.FK_SATISPAY = ps.ID_SATISPAY AND pw.FK_SATISPAY IS NOT null
        left JOIN AGID_USER.PP_BPAY pb  ON pw.FK_BPAY  = pb.ID_BPAY 
        left JOIN AGID_USER.PP_GENERIC_INSTRUMENT pgi  ON pw.FK_GENERIC_INSTRUMENT  = pgi.ID 
        left JOIN AGID_USER.PP_PSP pp  ON pw.FK_PSP  =  pp.ID 
        LEFT JOIN AGID_USER.PP_PAYPAL pp2 ON pw.ID_WALLET =pp2.FK_WALLET AND pp2.IS_CANCELED = '0'
        WHERE pu.NOTIFICATION_EMAIL = ?
        AND pu.STATUS IN ('11', '12')
        AND pw.FL_ENABLED ='1' AND pw.FL_VISIBLE ='1'
        AND ((pw.FK_SATISPAY IS NULL AND pp.ID_PSP != 'SATYLUL1') OR (pw.FK_SATISPAY IS NULL AND pp.ID_PSP IS NULL) OR  (pw.FK_SATISPAY IS NOT NULL ))
        ORDER BY Pw.CREATION_DATE DESC 
    """
        .trimIndent()
