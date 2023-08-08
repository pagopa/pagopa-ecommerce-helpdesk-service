--SET DEFINE OFF;
alter session set "_ORACLE_SCRIPT"=true; 

CREATE USER EVENT_REGISTRY IDENTIFIED BY pwd_event_registry;

GRANT CONNECT, RESOURCE TO EVENT_REGISTRY;
GRANT UNLIMITED TABLESPACE TO EVENT_REGISTRY;

CREATE TABLESPACE EVENT_REGISTRY_IDX
  DATAFILE 'EVENT_REGISTRY_IDX.dat' 
  SIZE 500M
  ONLINE;

GRANT CREATE ANY TABLE to PPTEST;
GRANT CREATE ANY SEQUENCE to PPTEST;
GRANT CREATE ANY INDEX to PPTEST;

SELECT 'GRANT ALL ON '||table_name||' TO PPTEST' FROM ALL_TABLES WHERE OWNER = 'EVENT_REGISTRY';

GRANT ALL PRIVILEGES to PPTEST;