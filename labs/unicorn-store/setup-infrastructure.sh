#bin/sh
set -e

# Build the unicorn application
cd ~/environment/aws-lambda-java-workshop/labs/unicorn-store
./mvnw clean package -f software/unicorn-store-spring/pom.xml
./mvnw clean package -f software/alternatives/unicorn-store-micronaut/pom.xml
./mvnw clean package -f software/alternatives/unicorn-store-quarkus/pom.xml

# Bootstrap and build the UnicornStoreApp stack
cd ~/environment/aws-lambda-java-workshop/labs/unicorn-store/infrastructure/cdk
cdk bootstrap
cdk deploy UnicornStoreInfrastructure --require-approval never --outputs-file target/output.json

# Detach AdministratorAccess which was needed for cdk bootstrap
aws iam detach-role-policy --role-name unicornstore-ide-user --policy-arn arn:aws:iam::aws:policy/AdministratorAccess
