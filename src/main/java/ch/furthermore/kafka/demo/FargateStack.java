package ch.furthermore.kafka.demo;


import java.util.Arrays;
import java.util.Map;

import software.amazon.awscdk.core.CfnParameter;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.Compatibility;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.ecs.TaskDefinition;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.msk.ClientBrokerEncryption;
import software.amazon.awscdk.services.msk.EbsStorageInfo;
import software.amazon.awscdk.services.msk.EncryptionInTransitConfig;
import software.amazon.awscdk.services.msk.KafkaVersion;

public class FargateStack extends Stack {
	public FargateStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

	public FargateStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        CfnParameter repositoryArn = CfnParameter.Builder.create(this, "repositoryArn")
            .type("String")
            .description("ECR repository ARN")
            .build();
        
        CfnParameter repositoryName = CfnParameter.Builder.create(this, "repositoryName")
            .type("String")
            .description("ECR repository Name")
            .build();
        
        CfnParameter dockerImageVersion = CfnParameter.Builder.create(this, "dockerImageVersion")
            .type("String")
            .description("Docker image tag")
            .build();
        
        Vpc vpc = Vpc.Builder.create(this, "FargateVPC") 
            .maxAzs(3)
            .subnetConfiguration(Arrays.asList(SubnetConfiguration.builder()
        		.cidrMask(24)
        		.subnetType(SubnetType.PUBLIC)
        		.name("public")
        		.build()))
            .build();

        SecurityGroup sg = SecurityGroup.Builder.create(this, "KafkaAndFargateSecurityGroup")
			.vpc(vpc)
			.allowAllOutbound(true)
			.build();
		sg.addIngressRule(sg, Port.allTcp()); // FIXME least privilege: only related ports
        
        software.amazon.awscdk.services.msk.Cluster kafkaCluster = software.amazon.awscdk.services.msk.Cluster.Builder.create(this,  "Kafka") // takes ~20 minutes
        	.instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.SMALL))
        	.kafkaVersion(KafkaVersion.V2_8_0)
        	.vpc(vpc)
        	.securityGroups(Arrays.asList(sg))
        	.clusterName("kafkademo")
        	.numberOfBrokerNodes(1) // per AZ
        	.encryptionInTransit(EncryptionInTransitConfig.builder()
        			.clientBroker(ClientBrokerEncryption.TLS_PLAINTEXT)
        			.enableInCluster(true)
        			.build())
        	.ebsStorageInfo(EbsStorageInfo.builder().volumeSize(8).build()) // costly default: (2*) 1000 TB 
        	.removalPolicy(RemovalPolicy.DESTROY) 
        	.build();
        
		Cluster cluster = Cluster.Builder.create(this, "FargateCluster")
			.vpc(vpc)
			.build();
		
		TaskDefinition td = TaskDefinition.Builder.create(this, "FargateTask") 
			.memoryMiB("512")
			.cpu("256")
			.compatibility(Compatibility.FARGATE)
			.build();
		td.addContainer("FargateContainer",  ContainerDefinitionOptions.builder()
			.image(ContainerImage.fromEcrRepository(Repository.fromRepositoryName(this, "Repository", repositoryName.getValueAsString()), dockerImageVersion.getValueAsString()))
			.portMappings(Arrays.asList(PortMapping.builder()
				.containerPort(8080)
				.build()))
			.environment(Map.of("SPRING_KAFKA_BOOTSTRAP-SERVERS", kafkaCluster.getBootstrapBrokers())) 
			.logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
				.streamPrefix("kafkademo")
				.build()))
			.build());
		td.addToExecutionRolePolicy(PolicyStatement.Builder.create()
	        .effect(Effect.ALLOW)
	        .actions(Arrays.asList("ecr:GetAuthorizationToken"))
	        .resources(Arrays.asList("*"))
	        .build());
		td.addToExecutionRolePolicy(PolicyStatement.Builder.create()
	        .effect(Effect.ALLOW)
	        .actions(Arrays.asList("ecr:BatchCheckLayerAvailability", "ecr:GetDownloadUrlForLayer", "ecr:BatchGetImage"))
	        .resources(Arrays.asList(repositoryArn.getValueAsString()))
	        .build());
		
		FargateService.Builder.create(this, "FargateService")
	    	.cluster(cluster)
	    	.taskDefinition(td)
	    	.vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
	        .desiredCount(2)
	        .assignPublicIp(true)
	        .securityGroups(Arrays.asList(sg))
	        .build();
    }
}
