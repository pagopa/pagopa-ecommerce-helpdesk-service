ALTER TABLE RE_WISP DROP CONSTRAINT RE_WISP_PK DROP INDEX;
CREATE INDEX RE_WISP_ID_IDX ON RE_WISP (ID);