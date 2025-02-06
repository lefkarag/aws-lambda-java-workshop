#bin/sh

# Build the unicorn application
./mvnw clean package -f ../../../labs/unicorn-store/software/unicorn-store-spring/pom.xml
./mvnw clean package -f ../../../labs/unicorn-store/software/alternatives/unicorn-store-micronaut/pom.xml
./mvnw clean package -f ../../../labs/unicorn-store/software/alternatives/unicorn-store-quarkus/pom.xml
