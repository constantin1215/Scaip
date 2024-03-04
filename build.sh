cd auth-microservice
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/auth-ms:v1"
cd ..

cd gateway-microservice
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/gateway-ms:v1"
cd ..

cd dispatch-microservice
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/dispatch-ms:v1"
cd ..

cd users-microservice
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/users-ms:v1"
cd ..
