package ch.furthermore.kafka.demo;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

public class CdkApp {
    public static void main(final String[] args) {
        App app = new App();

        new ECRStack(app, "KafkaDemoECR", StackProps.builder().build(), "kafkademo");
        
        new FargateStack(app, "KafkaDemoFargate", StackProps.builder().build());
        
        app.synth();
    }
}
