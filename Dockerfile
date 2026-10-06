FROM eclipse-temurin:21-jre

COPY --from=public.ecr.aws/awsguru/aws-lambda-adapter:1.1.0 \
     /lambda-adapter \
     /opt/extensions/lambda-adapter

WORKDIR /app

COPY backend/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]