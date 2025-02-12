package com.unicorn;

import java.util.Arrays;

import com.unicorn.constructs.WorkshopVpc;
import com.unicorn.constructs.VSCodeIde;
import com.unicorn.constructs.VSCodeIde.VSCodeIdeProps;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.constructs.Construct;

import software.amazon.awscdk.DefaultStackSynthesizer;
import software.amazon.awscdk.DefaultStackSynthesizerProps;

public class IdeStack extends Stack {

    private static final String BOOTSTRAP_SCRIPT = """
        date
        """;

    public IdeStack(final Construct scope, final String id) {
        // super(scope, id, props);
        super(scope, id, StackProps.builder()
                .synthesizer(new DefaultStackSynthesizer(DefaultStackSynthesizerProps.builder()
                        .generateBootstrapVersionRule(false)  // This disables the bootstrap version parameter
                        .build()))
                .build());

        // Create VPC
        var vpc = new WorkshopVpc(this, "IdeVpc", "ide-vpc").getVpc();

        // Create Workshop IDE
        var ideProps = new VSCodeIdeProps();
        ideProps.setBootstrapScript(BOOTSTRAP_SCRIPT);
        ideProps.setVpc(vpc);
        ideProps.setInstanceName("ide");
        ideProps.setEnableAppSecurityGroup(true);
        ideProps.setInstanceType(InstanceType.of(InstanceClass.M5, InstanceSize.XLARGE));
        ideProps.setExtensions(Arrays.asList(
                "amazonwebservices.aws-toolkit-vscode",
                "amazonwebservices.amazon-q-vscode",
                "vscjava.vscode-java-pack"
        ));
        new VSCodeIde(this, "VSCodeIde", ideProps);
    }
}
