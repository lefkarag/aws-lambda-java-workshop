package com.unicorn;

import com.unicorn.constructs.VSCodeIde;
import com.unicorn.constructs.WorkshopVpc;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.constructs.Construct;

import java.util.Arrays;

public class UnicornStoreInfraStack extends Stack {

    private final static String BOOTSTRAP_SCRIPT = """
            date
            
            echo '=== Clone Git repository ==='
            sudo -H -u ec2-user bash -c "git clone https://github.com/lefkarag/aws-lambda-java-workshop.git ~/environment/aws-lambda-java-workshop/"
            sudo -H -u ec2-user bash -c "cd ~/environment/aws-lambda-java-workshop && git checkout cdk-refactoring-split"
            
            echo '=== Setup IDE ==='
            sudo -H -i -u ec2-user bash -c "~/environment/aws-lambda-java-workshop/infrastructure/scripts/setup/setup-ide.sh"
            
            echo '=== Additional Setup ==='
            sudo -H -i -u ec2-user bash -c "~/environment/aws-lambda-java-workshop/labs/unicorn-store/infrastructure/scripts/setup/build.sh"
            """;

    public UnicornStoreInfraStack(final Construct scope, final String id, final StackProps props) {
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
    }

}
