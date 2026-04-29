FROM eclipse-temurin:17-jre-alpine
ADD target/TeacherMicroservice-1.0-SNAPSHOT-jar-with-dependencies.jar TeacherMicroservice.jar
ADD questions_database.txt questions_database.txt

ENTRYPOINT ["java", "-jar", "TeacherMicroservice.jar"]