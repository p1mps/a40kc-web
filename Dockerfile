FROM openjdk:8-alpine

COPY target/uberjar/a40kc-web.jar /a40kc-web/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/a40kc-web/app.jar"]
