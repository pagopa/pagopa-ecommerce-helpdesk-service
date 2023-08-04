FROM pagopadcommonacr.azurecr.io/dboracle-ee-12.2.0.1-slim

USER root

RUN cd /home/oracle/ \
	&& curl -LO https://github.com/liquibase/liquibase/releases/download/v4.8.0/liquibase-4.8.0.tar.gz \
	&& mkdir -p liquibase \
	&& tar xf liquibase-4.8.0.tar.gz -C liquibase \
	&& chown -R oracle:0 /home/oracle/liquibase/ \
	&& chmod -R a+rwx /home/oracle/liquibase/ \
	&& ln -sf /home/oracle/liquibase /usr/local/bin \
	&& yum -y install dos2unix \
	&& yum -y install java-1.8.0-openjdk \
	&& yum -y install which

USER oracle

ADD lib/com.oracle.ojdbc8-12.2.0.1.jar /home/oracle/liquibase/
ADD rollback_image.sh /home/oracle/
ADD changelogs/ /home/oracle/changelogs

USER root

RUN cd /home/oracle/ \
	&& chown -R oracle:0 rollback_image.sh \
	&& chmod -R a+rwx rollback_image.sh \
	&& dos2unix rollback_image.sh

USER oracle

CMD /home/oracle/rollback_image.sh