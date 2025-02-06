#bin/sh

# Build the unicorn application
./mvnw clean package -f ../../../labs/unicorn-store/software/unicorn-store-spring/pom.xml
./mvnw clean package -f ../../../labs/unicorn-store/software/alternatives/unicorn-store-micronaut/pom.xml
./mvnw clean package -f ../../../labs/unicorn-store/software/alternatives/unicorn-store-quarkus/pom.xml

S3_BUCKET=$(aws ssm get-parameter --name "unicornstore-lambda-bucket-name" \
  --query "Parameter.Value" --output text) && echo $S3_BUCKET
aws s3 cp target/unicorn-store-lambda.zip s3://$S3_BUCKET
aws lambda update-function-configuration --function-name unicorn-store-spring --runtime java21 --no-cli-pager
aws lambda update-function-code --function-name unicorn-store-spring \
  --s3-bucket $S3_BUCKET --s3-key unicorn-store-lambda.zip --no-cli-pager