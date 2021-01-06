FROM registry.opensource.zalan.do/library/openjdk-8:8-20201005

MAINTAINER Zalando SE

COPY resources/api/kio-api.yaml /zalando-apis/

EXPOSE 8080
ENV HTTP_PORT=8080


COPY target/kio.jar /
COPY java-dynamic-memory-opts /bin

CMD java $JAVA_OPTS $(java-dynamic-memory-opts 70) -jar /kio.jar
