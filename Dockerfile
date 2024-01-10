FROM container-registry.zalando.net/library/eclipse-temurin-8-jdk:latest

MAINTAINER Zalando SE

# add AWS RDS CA bundle
RUN mkdir /tmp/rds-ca && \
    curl https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem > /tmp/rds-ca/aws-rds-ca-bundle.pem
# split the bundle into individual certs (prefixed with xx)
# see http://blog.swwomm.com/2015/02/importing-new-rds-ca-certificate-into.html
RUN cd /tmp/rds-ca && csplit -sz aws-rds-ca-bundle.pem '/-BEGIN CERTIFICATE-/' '{*}'
RUN for CERT in /tmp/rds-ca/xx*; do mv $CERT /usr/local/share/ca-certificates/aws-rds-ca-$(basename $CERT).crt; done

# Sample script for importing certificates on Linux to the Java Keystore
# https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.SSL-certificate-rotation.html#UsingWithRDS.SSL-certificate-rotation-sample-script
RUN awk 'split_after == 1 {n++;split_after=0} /-----END CERTIFICATE-----/ {split_after=1}{print > "rds-ca-" n ".pem"}' < /tmp/rds-ca/aws-rds-ca-bundle.pem && \
    for CERT in rds-ca-*; do alias=$(openssl x509 -noout -text -in $CERT | perl -ne 'next unless /Subject:/; s/.*(CN=|CN = )//; print'); keytool -import -file ${CERT} -alias "${alias}" -storepass changeit -keystore $JAVA_HOME/jre/lib/security/cacerts -noprompt; rm ${CERT}; done

RUN update-ca-certificates

COPY resources/api/kio-api.yaml /zalando-apis/
COPY target/kio.jar /
COPY java-dynamic-memory-opts /usr/local/bin/java-dynamic-memory-opts
RUN chmod +x /usr/local/bin/java-dynamic-memory-opts

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $JAVA_OPTS $(java-dynamic-memory-opts 70) -jar /kio.jar
