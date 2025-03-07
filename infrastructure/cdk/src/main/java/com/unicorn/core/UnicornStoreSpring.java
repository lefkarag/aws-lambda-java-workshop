package com.unicorn.core;

import com.unicorn.common.CfnExports;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnOutputProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.events.IEventBus;
import software.amazon.awscdk.services.lambda.Alias;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class UnicornStoreSpring extends Construct {

    private Construct scope;

    public UnicornStoreSpring(final Construct scope, final String id,
                              IEventBus eventBridge, IVpc vpc, ISecurityGroup securityGroup,
                              String dbSecretString, String dbConnectionString) {
        super(scope, id);

        this.scope = scope;

        //Create Spring Lambda function
        var unicornStoreSpringLambda = createUnicornLambdaFunction(vpc, securityGroup, dbSecretString, dbConnectionString);

        //Permission for Spring Boot Lambda Function
        eventBridge.grantPutEventsTo(unicornStoreSpringLambda);

        //Setup a Proxy-Rest API to access the Spring Lambda function
        var restApi = setupRestApi(unicornStoreSpringLambda);
    }

    private RestApi setupRestApi(Alias unicornStoreSpringLambdaAlias) {
        return LambdaRestApi.Builder.create(scope, "UnicornStoreSpringApi")
                .restApiName("UnicornStoreSpringApi")
                .handler(unicornStoreSpringLambdaAlias)
                .build();
    }

    private Alias createUnicornLambdaFunction(IVpc vpc, ISecurityGroup securityGroup,
                                              String dbSecretString, String dbConnectionString) {
        var lambda = Function.Builder.create(scope, "UnicornStoreSpringFunction")
                .runtime(Runtime.JAVA_21)
                .functionName("unicorn-store-spring")
                .memorySize(512)
                .timeout(Duration.seconds(29))
                .code(Code.fromAsset("../../labs/unicorn-store/software/unicorn-store-spring/target/store-spring-1.0.0.jar"))
                .handler("com.amazonaws.serverless.proxy.spring.SpringDelegatingLambdaContainerHandler")
                .vpc(vpc)
                .securityGroups(List.of(securityGroup))
                .environment(Map.of(
                        "MAIN_CLASS", "com.unicorn.store.StoreApplication",
                        "SPRING_DATASOURCE_PASSWORD", dbSecretString,
                        "SPRING_DATASOURCE_URL", dbConnectionString,
                        "SPRING_DATASOURCE_HIKARI_maximumPoolSize", "1",
                        "AWS_SERVERLESS_JAVA_CONTAINER_INIT_GRACE_TIME", "500"
                ))
                .build();

        // Create an alias for the latest version
        var alias = Alias.Builder.create(scope, "UnicornStoreSpringFunctionAlias")
                .aliasName("live")
                .version(lambda.getLatestVersion())
                .build();

        return alias;
    }

}
