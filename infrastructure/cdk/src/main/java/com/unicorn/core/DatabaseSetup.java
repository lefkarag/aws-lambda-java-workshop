package com.unicorn.core;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.CustomResource;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseSetup extends Construct {

    private CustomResource databaseSetupResource;

    public DatabaseSetup(final Construct scope, final String id,
                         final InfrastructureCore infrastructureStore) {
        super(scope, id);

        if (databaseSetupResource == null) {
            Function databaseSetupFunction = Function.Builder.create(this, "DatabaseSetupFunction")
                    .code(Code.fromInline(loadFile("/database-setup.py")))
                    .handler("index.lambda_handler")
                    .runtime(Runtime.PYTHON_3_13)
                    .functionName("unicornstore-db-setup-lambda")
                    .timeout(Duration.minutes(3))
                    .vpc(infrastructureStore.getVpc())
                    .securityGroups(List.of(infrastructureStore.getApplicationSecurityGroup()))
                    .build();

            infrastructureStore.getDatabaseSecret().grantRead(databaseSetupFunction);
            infrastructureStore.getDatabase().grantDataApiAccess(databaseSetupFunction);

            databaseSetupResource = CustomResource.Builder.create(this, "DatabaseSetupResource")
                    .serviceToken(databaseSetupFunction.getFunctionArn())
                    .properties(Map.of(
                            "SecretName", infrastructureStore.getDatabaseSecret().getSecretName(),
                            "SqlStatements", loadFile("/schema.sql")
                    ))
                    .build();
            databaseSetupResource.getNode().addDependency(infrastructureStore.getDatabase());
        }
    }

    private String loadFile(String filePath) {
        try {
            return Files.readString(Path.of(getClass().getResource(filePath).getPath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file " + filePath, e);
        }
    }
}
