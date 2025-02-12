package com.unicorn.core;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.ssm.ParameterTier;
import software.amazon.awscdk.services.ssm.StringParameter;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.SecretsManagerSecretOptions;
import software.constructs.Construct;

import java.util.List;

public class InfrastructureCore extends Construct {

    private final DatabaseSecret databaseSecret;
    private final DatabaseInstance database;
    private final EventBus eventBridge;
    private final IVpc vpc;
    private final ISecurityGroup applicationSecurityGroup;
    private final StringParameter paramDBConnectionString;
    private final Secret secretPassword;

    public InfrastructureCore(final Construct scope, final String id, final IVpc vpc) {
        super(scope, id);

        this.vpc = vpc;
        databaseSecret = createDatabaseSecret();
        database = createDatabase(vpc, databaseSecret);
        eventBridge = createEventBus();
        applicationSecurityGroup = new SecurityGroup(this, "ApplicationSecurityGroup",
                SecurityGroupProps
                        .builder()
                        .securityGroupName("unicornstore-application-sg")
                        .vpc(vpc)
                        .allowAllOutbound(true)
                        .build());

        paramDBConnectionString = createParamDBConnectionString();
        secretPassword = createSecretPassword();
    }

    private EventBus createEventBus() {
        return EventBus.Builder.create(this, "UnicornEventBus")
                .eventBusName("unicorns")
                .build();
    }

    private SecurityGroup createDatabaseSecurityGroup(IVpc vpc) {
        var databaseSecurityGroup = SecurityGroup.Builder.create(this, "DatabaseSG")
                .securityGroupName("unicornstore-db-sg")
                .allowAllOutbound(false)
                .vpc(vpc)
                .build();

        databaseSecurityGroup.addIngressRule(
                Peer.ipv4("10.0.0.0/16"),
                Port.tcp(5432),
                "Allow Database Traffic from local network");

        return databaseSecurityGroup;
    }

    private DatabaseInstance createDatabase(IVpc vpc, DatabaseSecret databaseSecret) {

        var databaseSecurityGroup = createDatabaseSecurityGroup(vpc);
        var engine = DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_16).build());

        return DatabaseInstance.Builder.create(this, "UnicornInstance")
                .engine(engine)
                .vpc(vpc)
                .allowMajorVersionUpgrade(true)
                .backupRetention(Duration.days(0))
                .databaseName("unicorns")
                .instanceIdentifier("UnicornInstance")
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MEDIUM))
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_WITH_EGRESS)
                        .build())
                .securityGroups(List.of(databaseSecurityGroup))
                .credentials(Credentials.fromSecret(databaseSecret))
                .build();
    }

    private DatabaseSecret createDatabaseSecret() {
        return DatabaseSecret.Builder
                .create(this, "postgres")
                .secretName("unicornstore-db-secret")
                .username("postgres").build();
    }

    private Secret createSecretPassword() {
        // Separate password value for services which cannot get specific field from Secret json
        return Secret.Builder.create(this, "dbSecretPassword")
                .secretName("unicornstore-db-password-secret")
                .secretStringValue(SecretValue.secretsManager(databaseSecret.getSecretName(),
                        SecretsManagerSecretOptions.builder().jsonField("password").build()))
                .build();
    }

    public Secret getSecretPassword() {
        return secretPassword;
    }

    private StringParameter createParamDBConnectionString() {
        return StringParameter.Builder.create(this, "SsmParameterDBConnectionString")
                .allowedPattern(".*")
                .description("Database Connection String")
                .parameterName("unicornstore-db-connection-string")
                .stringValue(getDatabaseConnectionString())
                .tier(ParameterTier.STANDARD)
                .build();
    }

    public String getDatabaseConnectionString(){
        return "jdbc:postgresql://" + database.getDbInstanceEndpointAddress() + ":5432/unicorns";
    }

    public StringParameter getParamDBConnectionString() {
        return paramDBConnectionString;
    }

    public EventBus getEventBridge() {
        return eventBridge;
    }

    public IVpc getVpc() {
        return vpc;
    }

    public ISecurityGroup getApplicationSecurityGroup() {
        return applicationSecurityGroup;
    }

    public String getDatabaseSecretString() {
        return databaseSecret.secretValueFromJson("password").toString();
    }

    public DatabaseSecret getDatabaseSecret() {
        return databaseSecret;
    }

    public DatabaseInstance getDatabase() {
        return database;
    }
}
