package com.unicorn.alternatives;

import com.unicorn.core.InfrastructureCore;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;


public class UnicornStoreQuarkus extends Construct {

    private final InfrastructureCore infrastructureCore;

    public UnicornStoreQuarkus(final Construct scope, final String id, final InfrastructureCore infrastructureCore) {
        super(scope, id);
        this.infrastructureCore = infrastructureCore;

        //Quarkus app
        var unicornStoreQuarkus = createUnicornLambdaFunction();
        infrastructureCore.getEventBridge().grantPutEventsTo(unicornStoreQuarkus);

        var restApi = setupRestApi(unicornStoreQuarkus);

        new CfnOutput(this, "ApiEndpointQuarkus", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());
    }

    private RestApi setupRestApi(Version unicornStoreLambdaContainer) {
        return LambdaRestApi.Builder.create(this, "UnicornStoreQuarkusApi")
                .restApiName("UnicornStoreQuarkusApi")
                .handler(unicornStoreLambdaContainer)
                .build();
    }

    private Version createUnicornLambdaFunction() {
        var lambda = Function.Builder.create(this, "UnicornStoreQuarkusFunction")
                .runtime(Runtime.JAVA_21)
                .functionName("unicorn-store-quarkus")
                .memorySize(2048)
                .timeout(Duration.seconds(29))
                .code(Code.fromAsset("../../labs/unicorn-store/software/alternatives/unicorn-store-quarkus/target/function.zip"))
                .handler("io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest")
                .vpc(infrastructureCore.getVpc())
                .securityGroups(List.of(infrastructureCore.getApplicationSecurityGroup()))
                .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
                .environment(new HashMap<>() {{
                    put("QUARKUS_DATASOURCE_PASSWORD", infrastructureCore.getDatabaseSecretString());
                    put("QUARKUS_DATASOURCE_JDBC_URL", infrastructureCore.getDatabaseConnectionString());
                    put("QUARKUS_DATASOURCE_JDBC_INITIAL_SIZE", "1");
                    put("QUARKUS_DATASOURCE_JDBC_MIN_SIZE", "0");
                    put("QUARKUS_DATASOURCE_JDBC_MAX_SIZE", "1");
                }})
                .build();
        return lambda.getCurrentVersion();
    }
}
