package com.unicorn.core;

import com.unicorn.constructs.VSCodeIde;
import com.unicorn.constructs.WorkshopVpc;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.constructs.Construct;

import java.util.Arrays;

public class InfrastructureStack extends Stack {

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

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
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
        var ide = new VSCodeIde(this, "UnicornStoreIde", ideProps);
        var ideRole = ideProps.getRole();
        var ideInternalSecurityGroup = ide.getIdeInternalSecurityGroup();

        // Create Core infrastructure
        var infrastructureCore = new InfrastructureCore(this, "InfrastructureCore", vpc);

        // Execute Database setup
        var databaseSetup = new DatabaseSetup(this, "UnicornDatabaseConstruct", infrastructureCore);
        databaseSetup.getNode().addDependency(infrastructureCore.getDatabase());

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
