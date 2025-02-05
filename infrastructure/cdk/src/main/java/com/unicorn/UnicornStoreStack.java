package com.unicorn;

import com.unicorn.UnicornStoreStack;
import com.unicorn.alternatives.UnicornAuditService;
import com.unicorn.alternatives.UnicornStoreMicronaut;
import com.unicorn.alternatives.UnicornStoreQuarkus;
import com.unicorn.alternatives.UnicornStoreSpringGraalVM;
import com.unicorn.constructs.VSCodeIde;
import com.unicorn.constructs.WorkshopVpc;
import com.unicorn.core.DatabaseSetup;
import com.unicorn.core.InfrastructureCore;
import com.unicorn.core.UnicornStoreSpring;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.constructs.Construct;

import java.util.Arrays;

public class UnicornStoreStack extends Stack {

    private final static String BOOTSTRAP_SCRIPT = """
        date

        echo '=== Clone Git repository ==='
        sudo -H -u ec2-user bash -c "git clone https://github.com/aws-samples/aws-lambda-java-workshop ~/aws-lambda-java-workshop/"
        # sudo -H -u ec2-user bash -c "cd ~/aws-lambda-java-workshop && git checkout refactoring"

        echo '=== Setup IDE ==='
        sudo -H -i -u ec2-user bash -c "~/aws-lambda-java-workshop/infrastructure/scripts/setup/ide.sh"

        echo '=== Additional Setup ==='
        sudo -H -i -u ec2-user bash -c "~/java-on-aws/infrastructure/scripts/setup/app.sh"
        sudo -H -i -u ec2-user bash -c "~/java-on-aws/infrastructure/scripts/setup/eks.sh"
        """;

    public UnicornStoreStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, StackProps.builder()
                .synthesizer(new DefaultStackSynthesizer(DefaultStackSynthesizerProps.builder()
                        .generateBootstrapVersionRule(false)  // This disables the bootstrap version parameter
                        .build()))
                .build());

        // Create VPC
        var vpc = new WorkshopVpc(this, "UnicornStoreVpc", "unicornstore-vpc").getVpc();

        // Create Workshop IDE
        var ideProps = new VSCodeIde.VSCodeIdeProps();
        ideProps.setBootstrapScript(BOOTSTRAP_SCRIPT);
        ideProps.setVpc(vpc);
        ideProps.setInstanceName("unicornstore-ide");
        ideProps.setEnableAppSecurityGroup(true);
        ideProps.setInstanceType(InstanceType.of(InstanceClass.T3, InstanceSize.MEDIUM));
        ideProps.setExtensions(Arrays.asList(
                "amazonwebservices.aws-toolkit-vscode",
                "amazonwebservices.amazon-q-vscode",
                "vscjava.vscode-java-pack"
        ));
        new VSCodeIde(this, "UnicornStoreIde", ideProps);

        // Create Core infrastructure
        var infrastructureCore = new InfrastructureCore(this, "InfrastructureCore", vpc);

        // Execute Database setup
        var databaseSetup = new DatabaseSetup(this, "UnicornDatabaseConstruct", infrastructureCore);
        databaseSetup.getNode().addDependency(infrastructureCore.getDatabase());

        // Create Lambda functions
        new UnicornStoreSpring(this, "UnicornStoreSpringApp", infrastructureCore);
//        new UnicornStoreMicronaut(this, "UnicornStoreMicronautApp", StackProps.builder().build(), infrastructureCore);
//        new UnicornStoreSpringGraalVM(this, "UnicornStoreSpringGraalVMApp", StackProps.builder().build(), infrastructureCore);
//        new UnicornStoreQuarkus(this, "UnicornStoreQuarkusApp", StackProps.builder().build(), infrastructureCore);
//        new UnicornAuditService(this, "UnicornAuditServiceApp", StackProps.builder().build(), infrastructureCore);

        // Create Workshop CodeBuild
//        var codeBuildProps = new CodeBuildResourceProps();
//        codeBuildProps.setProjectName("unicornstore-codebuild");
//        codeBuildProps.setBuildspec(buildspec);
//        codeBuildProps.setVpc(vpc);
//        codeBuildProps.setAdditionalIamPolicies(Arrays.asList(
//                ManagedPolicy.fromAwsManagedPolicyName("AdministratorAccess")));
//        new CodeBuildResource(this, "UnicornStoreCodeBuild", codeBuildProps);
    }
}
