INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'EMAIL_RETRY_MAX_ATTEMPTS', '2', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'EMAIL_RETRY_WAIT_MILLIS', '5000', SYSDATE);

INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'EMAIL_BATCH_ENABLED', 'true', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'EMAIL_BATCH_BLOCKSIZE', '100', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'EMAIL_BATCH_MAX_RETRY', '3', SYSDATE);

INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'EMAIL_BATCH_NUM_MINUTES_QUERY' ,'10', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'EMAIL_MAX_ONLINE_REQUEST_OFFSET_MINUTES' ,'5', SYSDATE);

INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE)
SELECT SEQ_CONFIG.NEXTVAL, 'EMAIL_BATCH_REPORT_EMAIL_TO' , VALUE, SYSDATE
FROM PP_CONFIG WHERE KEY = 'EMAIL_RECIPIENT';