DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_PANS_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_PANS_URL', 'http://mock-ppt-lmi-npa-sit.ocp-tst-npaspc.sia.eu/cobadge/api/pagopa/banking/v4.0/utils/payment-instruments/search', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'COBADGE_SEARCH_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'COBADGE_SEARCH_URL', 'http://mock-ppt-lmi-npa-sit.ocp-tst-npaspc.sia.eu/cobadge/api/pagopa/banking/v4.0/utils/payment-instruments/{searchRequestId}', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'SATISPAY_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'SATISPAY_URL', 'http://mock-ppt-lmi-npa-sit.ocp-tst-npaspc.sia.eu/satispay/v1/consumers', SYSDATE);

UPDATE PP_PAYPAL_PSP_DETAILS SET PSP_URL = 'https://api.dev.platform.pagopa.it/mock-psp/api/paypalpsp' WHERE ID_PSP = 'PAYTITM1';
UPDATE PP_PAYPAL_PSP_DETAILS SET PSP_URL = 'https://api.dev.platform.pagopa.it/mock-psp/api/paypalpsp' WHERE ID_PSP = 'PAYPAL_PSP_MOCK_AZURE';
UPDATE PP_PAYPAL_PSP_DETAILS SET PSP_URL = 'https://api.dev.platform.pagopa.it/mock-psp/api/paypalpsp' WHERE ID_PSP = 'IDPSPFNZ';
UPDATE PP_PAYPAL_PSP_DETAILS SET PSP_URL = 'https://api.dev.platform.pagopa.it/mock-psp/api/paypalpsp' WHERE ID_PSP = '40000000001';
UPDATE PP_PAYPAL_PSP_DETAILS SET PSP_URL = 'test' WHERE ID_PSP = '70000000001';
UPDATE PP_PAYPAL_PSP_DETAILS SET PSP_URL = 'https://st.paytipper.com/srvs/AI' WHERE ID_PSP = 'PAYTIPPER';
UPDATE PP_PAYPAL_PSP_DETAILS SET PSP_URL = 'https://api.dev.platform.pagopa.it/mock-psp/api/paypalpsp' WHERE ID_PSP = '50000000001';