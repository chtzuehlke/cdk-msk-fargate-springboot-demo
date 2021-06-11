package ch.furthermore.kafka.demo;


import software.amazon.awscdk.core.CfnOutput;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ecr.Repository;

public class ECRStack extends Stack { 
	public ECRStack(final Construct scope, final String id, final String ecrRepositoryName) {
        this(scope, id, null, ecrRepositoryName);
    }

	public ECRStack(final Construct scope, final String id, final StackProps props, final String ecrRepositoryName) {
        super(scope, id, props);

        Repository dockerRepo = Repository.Builder.create(this, "DockerRepository")
        	.repositoryName(ecrRepositoryName)
        	.build();
        
        CfnOutput.Builder.create(this, "DockerRepositoryURI").value(dockerRepo.getRepositoryUri()).build();
        CfnOutput.Builder.create(this, "DockerRepositoryARN").value(dockerRepo.getRepositoryArn()).build();
        CfnOutput.Builder.create(this, "DockerRepositoryName").value(dockerRepo.getRepositoryName()).build();
    }
}
