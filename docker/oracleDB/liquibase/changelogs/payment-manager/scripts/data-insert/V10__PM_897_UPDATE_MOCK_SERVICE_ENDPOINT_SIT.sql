DELETE FROM PP_CONFIG WHERE KEY = 'BPD_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BPD_URL', 'https://api.dev.platform.pagopa.it/pmmockserviceapi/bpd/pm/payment-instrument', SYSDATE);
DELETE FROM PP_CONFIG WHERE KEY = 'BANCOMAT_PANS_URL';
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'BANCOMAT_PANS_URL', 'https://api.dev.platform.pagopa.it/pmmockserviceapi/bancomat/pagopa/pan', SYSDATE);