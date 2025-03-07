#bin/sh

TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
AWS_REGION=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/dynamic/instance-identity/document | jq -r '.region')
ACCOUNT_ID=$(aws sts get-caller-identity --output text --query Account --region $AWS_REGION)
MAC_ADDRESS=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/network/interfaces/macs)
export VPC_ID=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/network/interfaces/macs/$MAC_ADDRESS/vpc-id)

# Build the unicorn application
cd ~/environment/aws-lambda-java-workshop/labs/unicorn-store
./mvnw clean package -f software/unicorn-store-spring/pom.xml
./mvnw clean package -f software/alternatives/unicorn-store-micronaut/pom.xml
./mvnw clean package -f software/alternatives/unicorn-store-quarkus/pom.xml

# Bootstrap and build the UnicornStoreApp stack
cd ~/environment/aws-lambda-java-workshop/infrastructure/cdk
cdk bootstrap
cdk deploy UnicornStoreAppStack --require-approval never --outputs-file target/output.json

# Detach AdministratorAccess which was needed for cdk bootstrap
aws iam detach-role-policy --role-name unicornstore-ide-user --policy-arn arn:aws:iam::aws:policy/AdministratorAccess
