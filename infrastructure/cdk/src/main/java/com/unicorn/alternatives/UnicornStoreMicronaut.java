package com.unicorn.alternatives;

import java.util.HashMap;
import java.util.List;

import com.unicorn.core.InfrastructureCore;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

public class UnicornStoreMicronaut extends Stack {

    private final InfrastructureCore infrastructureCore;

    public UnicornStoreMicronaut(final Construct scope, final String id, final StackProps props, final InfrastructureCore infrastructureCore) {
        super(scope, id, props);
        this.infrastructureCore = infrastructureCore;

        //Micronaut app
        var unicornStoreMicronaut = createUnicornLambdaFunction();
        infrastructureCore.getEventBridge().grantPutEventsTo(unicornStoreMicronaut);

        var restApi = setupRestApi(unicornStoreMicronaut);

        new CfnOutput(this, "ApiEndpointMicronaut", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());
    }

    private RestApi setupRestApi(Version unicornStoreLambdaContainer) {
        return LambdaRestApi.Builder.create(this, "UnicornStoreMicronautApi")
                .restApiName("UnicornStoreMicronautApi")
                .handler(unicornStoreLambdaContainer)
                .build();
    }

    private Version createUnicornLambdaFunction() {
        var lambda = Function.Builder.create(this, "UnicornStoreMicronautFunction")
                .runtime(Runtime.JAVA_21)
                .functionName("unicorn-store-micronaut")
                .memorySize(2048)
                .timeout(Duration.seconds(29))
                .code(Code.fromAsset("../../software/alternatives/unicorn-store-micronaut/target/store-micronaut-2.0.0.jar"))
                .handler("io.micronaut.function.aws.proxy.payload1.ApiGatewayProxyRequestEventFunction")
                .vpc(infrastructureCore.getVpc())
                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
                .securityGroups(List.of(infrastructureCore.getApplicationSecurityGroup()))
                .environment(new HashMap<>() {{
                    put("DATASOURCES_DEFAULT_PASSWORD", infrastructureCore.getDatabaseSecretString());
                    put("DATASOURCES_DEFAULT_URL", infrastructureCore.getDatabaseConnectionString());
                    put("DATASOURCES_DEFAULT_maxPoolSize", "1");
                }})
                .build();

        return lambda.getCurrentVersion();
    }
}
