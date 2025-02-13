#bin/sh

# Build the unicorn application
cd ~/environment/aws-lambda-java-workshop/labs/unicorn-store
./mvnw clean package -f software/unicorn-store-spring/pom.xml
./mvnw clean package -f software/alternatives/unicorn-store-micronaut/pom.xml
./mvnw clean package -f software/alternatives/unicorn-store-quarkus/pom.xml
