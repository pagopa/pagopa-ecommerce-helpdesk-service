--SET DEFINE OFF;
alter session set "_ORACLE_SCRIPT"=true; 
CREATE USER AGID_USER IDENTIFIED BY BW7P8U;

ALTER USER AGID_USER IDENTIFIED BY BW7P8U;
GRANT CONNECT, RESOURCE TO AGID_USER;
GRANT UNLIMITED TABLESPACE TO AGID_USER;

CREATE TABLESPACE AGID_DATA
  DATAFILE 'AGID_DATA.dat' 
  SIZE 500M
  ONLINE;

GRANT CREATE ANY TABLE to AGID_USER;
GRANT CREATE ANY SEQUENCE to AGID_USER;
GRANT CREATE ANY INDEX to AGID_USER;

SELECT 'GRANT ALL ON '||table_name||' TO AGID_USER' FROM ALL_TABLES WHERE OWNER = 'PAYMENT_MANAGER';

GRANT ALL PRIVILEGES to AGID_USER;