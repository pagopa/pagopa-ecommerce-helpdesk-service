echo 'Waiting for DB to be operational...'
sleep 100s

/home/oracle/liquibase/liquibase --changeLogFile=/home/oracle/changelogs/schema-creation-changelog.xml --url="jdbc:oracle:thin:@localhost:1521:ORCLCDB" --username="sys as sysdba" --password=Oradoc_db1 --log-level=INFO --driver=oracle.jdbc.driver.OracleDriver --classpath=/home/oracle/liquibase/com.oracle.ojdbc8-12.2.0.1.jar update

while [ $? -ne 0 ]
do
	sleep 60s
	/home/oracle/liquibase/liquibase --changeLogFile=/home/oracle/changelogs/schema-creation-changelog.xml --url="jdbc:oracle:thin:@localhost:1521:ORCLCDB" --username="sys as sysdba" --password=Oradoc_db1 --log-level=INFO --driver=oracle.jdbc.driver.OracleDriver --classpath=/home/oracle/liquibase/com.oracle.ojdbc8-12.2.0.1.jar update
done

/home/oracle/liquibase/liquibase --changeLogFile=/home/oracle/changelogs/payment-manager/master-changelog.xml --url="jdbc:oracle:thin:@localhost:1521:ORCLCDB" --username=AGID_USER --password=BW7P8U --contexts="tag,baseline,insert,incremental,insert-dev" --log-level=INFO --driver=oracle.jdbc.driver.OracleDriver --classpath=/home/oracle/liquibase/com.oracle.ojdbc8-12.2.0.1.jar update