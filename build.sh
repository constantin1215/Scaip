cd auth-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/auth-ms:v1"
cd ..
cd gateway-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/gateway-ms:v1"
cd ..
cd dispatch-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/dispatch-ms:v1"
cd ..
cd users-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/users-ms:v1"
cd ..
cd query-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/query-ms:v1"
cd ..
cd groups-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/groups-ms:v1"
cd ..
cd messaging-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/messaging-ms:v1"
cd ..
cd call-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/calls-ms:v1"
cd ..
cd video-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/video-ms:v1"
cd ..
cd audio-microservice
./gradlew build clean
./gradlew build -Dquarkus.container-image.build=true -Dquarkus.container-image.image="constatin1215/audio-ms:v1"
cd ..
docker compose up
