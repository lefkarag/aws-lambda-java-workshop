package com.unicorn.alternatives;

import com.unicorn.core.InfrastructureCore;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class UnicornStoreSpringGraalVM extends Construct {

    private final InfrastructureCore infrastructureCore;

    public UnicornStoreSpringGraalVM(final Construct scope, final String id, final InfrastructureCore infrastructureCore) {
        super(scope, id);
        this.infrastructureCore = infrastructureCore;

        var unicornStoreSpringGraalVM = createUnicornLambdaFunction();
        infrastructureCore.getEventBridge().grantPutEventsTo(unicornStoreSpringGraalVM);

        var restApi = setupRestApi(unicornStoreSpringGraalVM);

        new CfnOutput(this, "ApiEndpointSpringGraalVM", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());

        //Create output values for later reference
        new CfnOutput(this, "unicorn-store-spring-graalvm-function-arn", CfnOutputProps.builder()
                .value(unicornStoreSpringGraalVM.getFunctionArn())
                .build());
    }

    private RestApi setupRestApi(Function unicornStoreSpringGraalVM) {
        return LambdaRestApi.Builder.create(this, "UnicornStoreSpringGraalVMApi")
                .restApiName("UnicornStoreSpringGraalVMApi")
                .handler(unicornStoreSpringGraalVM)
                .build();
    }

    private Function createUnicornLambdaFunction() {
        return Function.Builder.create(this, "UnicornStoreSpringGraalVMFunction")
                .runtime(Runtime.PROVIDED_AL2023)
                .functionName("unicorn-store-spring-graalvm")
                .memorySize(2048)
                .timeout(Duration.seconds(29))
                .code(Code.fromAsset("../../labs/unicorn-store/software/alternatives/unicorn-store-spring-graalvm/lambda-spring-graalvm.zip"))
                .handler("com.amazonaws.serverless.proxy.spring.SpringDelegatingLambdaContainerHandler")
                .vpc(infrastructureCore.getVpc())
                .securityGroups(List.of(infrastructureCore.getApplicationSecurityGroup()))
                .environment(Map.of(
                        "MAIN_CLASS", "com.unicorn.store.StoreApplication",
                        "SPRING_DATASOURCE_PASSWORD", infrastructureCore.getDatabaseSecretString(),
                        "SPRING_DATASOURCE_URL", infrastructureCore.getDatabaseConnectionString(),
                        "SPRING_DATASOURCE_HIKARI_maximumPoolSize", "1")
                )
                .build();
    }
}
