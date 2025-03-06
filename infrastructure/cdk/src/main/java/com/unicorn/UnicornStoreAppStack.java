package com.unicorn;

import com.unicorn.common.CfnExports;
import com.unicorn.core.UnicornStoreSpring;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.events.IEventBus;
import software.constructs.Construct;
import software.amazon.awscdk.services.ec2.Vpc;

public class UnicornStoreAppStack extends Stack {

    public UnicornStoreAppStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("ACCOUNT_ID"))
                        .region(System.getenv("AWS_REGION"))
                        .build())
                .build());

        //Get previously created infrastructure
        var vpc = getVpc();
        var eventBridge = getEventBridge();
        var securityGroup = getSecurityGroup();
        var databaseSecret = getDatabaseSecret();
        var databaseConnection = getDatabaseConnection();

        var unicornStoreSpring = new UnicornStoreSpring(this, "UnicornStoreSpringApp",
                eventBridge, vpc, securityGroup, databaseSecret, databaseConnection);

//        var unicornStoreMicronaut = new UnicornStoreMicronaut(scope, "UnicornStoreMicronautApp", StackProps.builder()
//                .build());
//
//        var unicornStoreSpringGraalVM = new UnicornStoreSpringGraalVM(scope, "UnicornStoreSpringGraalVMApp", StackProps.builder()
//                .build());
//
//        var unicornStoreQux``arkus = new UnicornStoreQuarkus(scope, "UnicornStoreQuarkusApp", StackProps.builder().build());

//        var unicornAuditService = new UnicornAuditService(scope, "UnicornAuditServiceApp", StackProps.builder().build());
    }

    private IVpc getVpc() {
        var vpc = Vpc.fromLookup(this, "ImportedVpcId", VpcLookupOptions.builder()
                .vpcId(System.getenv("VPC_ID"))
                .build());

        return vpc;
    }

    private IEventBus getEventBridge() {
        var eventBridge = EventBus.fromEventBusArn(
                this,
                "ImportedEventBus",
                Fn.importValue(CfnExports.UNICORN_STORE_EVENT_BRIDGE_ARN)
        );

        return eventBridge;
    }

    private ISecurityGroup getSecurityGroup() {
        var securityGroup = SecurityGroup.fromSecurityGroupId(
                this,
                "ImportedSecurityGroup",
                Fn.importValue(CfnExports.UNICORN_STORE_SECURITY_GROUP_ID)
        );

        return securityGroup;
    }

    private String getDatabaseSecret() {
        SecretValue secretValue = SecretValue.secretsManager("unicornstore-db-password-secret",
                SecretsManagerSecretOptions.builder()
                        .jsonField("password")
                        .build());

        return secretValue.toString();
    }

    private String getDatabaseConnection() {
        return Fn.importValue(CfnExports.UNICORN_STORE_DATABASE_CONNECTION);
    }

}
