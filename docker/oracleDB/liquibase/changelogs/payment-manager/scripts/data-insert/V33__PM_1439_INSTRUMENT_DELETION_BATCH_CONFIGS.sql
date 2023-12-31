INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'INSTRUMENT_DELETION_BATCH_FILE_PATH', '/logbackup/deletion-batch/fiscalCodes.txt', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'INSTRUMENT_DELETION_BATCH_COMMIT_INTERVAL_MS', '0', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'INSTRUMENT_DELETION_BATCH_START_DATE', '08-DEC-2020', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'INSTRUMENT_DELETION_BATCH_END_DATE', '30-JUN-2021', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'INSTRUMENT_DELETION_BATCH_SELECT_SIZE', '200', SYSDATE);
INSERT INTO PP_CONFIG (ID_CONFIG, KEY, VALUE, CREATION_DATE) VALUES (SEQ_CONFIG.NEXTVAL, 'INSTRUMENT_DELETION_BATCH_NUMBER_OF_RUNS', '5', SYSDATE);