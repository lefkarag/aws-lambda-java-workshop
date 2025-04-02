#bin/sh

TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
AWS_REGION=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/dynamic/instance-identity/document | jq -r '.region')
ACCOUNT_ID=$(aws sts get-caller-identity --output text --query Account --region $AWS_REGION)
MAC_ADDRESS=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/network/interfaces/macs)
export VPC_ID=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/network/interfaces/macs/$MAC_ADDRESS/vpc-id)

app=$1
build=$2

if [ $app == "spring" ]
then
  if [[ $build == "--build" ]]
  then
    ./mvnw clean package -f software/unicorn-store-spring/pom.xml
  fi
  cd infrastructure/cdk
  cdk deploy UnicornStoreSpringApp --outputs-file target/output.json --require-approval never
  exit 0
fi

if [ $app == "micronaut" ]
then
  if [[ $build == "--build" ]]
  then
    ./mvnw clean package -f software/alternatives/unicorn-store-micronaut/pom.xml
  fi
  cd infrastructure/cdk
  cdk deploy UnicornStoreMicronautApp --outputs-file target/output-micronaut.json --require-approval never
  exit 0
fi

if [ $app == "quarkus" ]
then
  if [[ $build == "--build" ]]
  then
    ./mvnw clean package -f software/alternatives/unicorn-store-quarkus/pom.xml
  fi
  cd infrastructure/cdk
  cdk deploy UnicornStoreQuarkusApp --outputs-file target/output-quarkus.json --require-approval never
  exit 0
fi

if [ $app == "spring-graalvm" ]
then
  if [[ $build == "--build" ]]
  then
    ./mvnw clean package -f software/alternatives/unicorn-store-basic/pom.xml
  fi
  cd infrastructure/cdk
  cdk deploy UnicornStoreSpringGraalVMApp --outputs-file target/output-spring-graalvm.json --require-approval never
  exit 0
fi

if [ $app == "audit-service" ]
then
  if [[ $build == "--build" ]]
  then
    ./mvnw clean package -f software/alternatives/unicorn-audit-service/pom.xml
  fi
  cd infrastructure/cdk
  cdk deploy UnicornAuditServiceApp --outputs-file target/output-audit-service.json --require-approval never
  exit 0
fi

