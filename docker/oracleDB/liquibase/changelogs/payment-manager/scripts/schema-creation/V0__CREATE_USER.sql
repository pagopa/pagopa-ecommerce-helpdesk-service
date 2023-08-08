--SET DEFINE OFF;
alter session set "_ORACLE_SCRIPT"=true; 
CREATE USER PPTEST IDENTIFIED BY BW7P8U;

ALTER USER PPTEST IDENTIFIED BY BW7P8U;
GRANT CONNECT, RESOURCE TO PPTEST;
GRANT UNLIMITED TABLESPACE TO PPTEST;

CREATE TABLESPACE AGID_DATA
  DATAFILE 'AGID_DATA.dat' 
  SIZE 500M
  ONLINE;