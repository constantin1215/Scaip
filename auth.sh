cd auth-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/auth-ms:v1"
cd ..
