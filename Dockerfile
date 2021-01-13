FROM registry.opensource.zalan.do/library/openjdk-8:latest

MAINTAINER Zalando SE

# add AWS RDS CA bundle
RUN mkdir /tmp/rds-ca && \
    curl https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem > /tmp/rds-ca/aws-rds-ca-bundle.pem
# split the bundle into individual certs (prefixed with xx)
# see http://blog.swwomm.com/2015/02/importing-new-rds-ca-certificate-into.html
RUN cd /tmp/rds-ca && csplit -sz aws-rds-ca-bundle.pem '/-BEGIN CERTIFICATE-/' '{*}'
RUN for CERT in /tmp/rds-ca/xx*; do mv $CERT /usr/local/share/ca-certificates/aws-rds-ca-$(basename $CERT).crt; done
RUN update-ca-certificates

COPY resources/api/kio-api.yaml /zalando-apis/
COPY target/kio.jar /
COPY java-dynamic-memory-opts /bin/java-dynamic-memory-opts

EXPOSE 8080
ENV HTTP_PORT=8080

CMD java $JAVA_OPTS $(java-dynamic-memory-opts 70) -jar /kio.jar
