FROM eclipse-temurin:11-jre-alpine

ADD build/libs/pages-processor-*.jar /pages-processor.jar

ENTRYPOINT ["java", "-jar", "/pages-processor.jar"]

VOLUME /pages/

VOLUME /pages/pages

WORKDIR /pages


