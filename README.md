# Hello world: CDK & MSK & SpringBoot @ ECS Fargate

Dependencies:

* Linux-like OS (macOS)
* Ruby installed
* JDK installed
* Docker installed and running
* CDK installed
* AWS CLI installed and configured (AWS_PROFILE & AWS_DEFAULT_REGION set)

Howto:

    rake cdkdeployecr
    rake dockerpush
    rake cdkdeployfargate
