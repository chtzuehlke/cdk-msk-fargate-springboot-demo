# Hello world: CDK & MSK & SpringBoot @ ECS Fargate

Disclaimer: not ready for production!

## Dependencies

* Linux-like OS (macOS)
* Ruby installed
* JDK installed
* Docker installed and running
* CDK installed
* AWS CLI installed and configured (AWS_PROFILE & AWS_DEFAULT_REGION set)

## Step 1

    rake cdkdeployecr
    rake dockerpush
    rake cdkdeployfargate

## Step 2

Watch the CloudWatch logs of both service tasks

## Step 3

    rake deletefargate
    rake deleteecr
