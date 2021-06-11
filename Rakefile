require 'open3'
require 'digest/md5'

def command(cmd)
  puts "$ #{cmd}"

  Open3.popen2e(cmd) do |stdin, stdout_err, wait_thr|
    while line = stdout_err.gets
      puts line
    end

    exit_status = wait_thr.value
    abort "FAILED: #{cmd}" unless exit_status.success?
  end
end

desc "Build runnable jar"
task :build do
  command "chmod +x ./mvnw && ./mvnw install"
end

desc "Run locally"
task :run => :build do
  command "java -jar ./target/kafka-demo-0.0.1-SNAPSHOT.jar" 
end

desc "cdk ls"
task :cdkls => :build do
  command "cdk ls" 
end

desc "CDK deploy ECR stack"
task :cdkdeployecr => :build do
  command "cdk deploy --require-approval=never --path-metadata false --outputs-file ecr.json -e KafkaDemoECR" 
end

desc "Build docker image"
task :dockerbuild do
  command "chmod +x ./mvnw && ./mvnw install spring-boot:build-image -Dspring-boot.build-image.imageName=furthermore/kafkademo"
end

desc "Docker compose up"
task :composeup => :dockerbuild do
  command "docker-compose up -d"
end

desc "Docker tail logs 1"
task :dockerlogs1  do
  command "docker logs -f kafka-demo_demo1_1"
end

desc "Docker tail logs 2"
task :dockerlogs2  do
  command "docker logs -f kafka-demo_demo2_1"
end

desc "Docker compose down"
task :composedown do
  command "docker-compose down"
end

desc "Push docker image"
task :dockerpush => :dockerbuild do
  command "aws ecr get-login-password | docker login --username AWS --password-stdin $(cat ecr.json | jq -r '.KafkaDemoECR.DockerRepositoryURI')"
  command "docker tag furthermore/kafkademo $(cat ecr.json | jq -r '.KafkaDemoECR.DockerRepositoryURI'):latest"
  command "docker push $(cat ecr.json | jq -r '.KafkaDemoECR.DockerRepositoryURI'):latest"
end

desc "CDK deploy Fargate stack"
task :cdkdeployfargate => :build do
  command "cdk deploy --require-approval=never --path-metadata false --parameters dockerImageVersion=latest --parameters repositoryArn=$(cat ecr.json | jq -r '.KafkaDemoECR.DockerRepositoryARN') --parameters repositoryName=$(cat ecr.json | jq -r '.KafkaDemoECR.DockerRepositoryName') --outputs-file fargate.json -e KafkaDemoFargate" 
end

desc "CF delete Fargate stack"
task :deletefargate do
  command "aws cloudformation delete-stack --stack-name KafkaDemoFargate" 
end

desc "CF delete ECR stack"
task :deleteecr do
  command "aws cloudformation delete-stack --stack-name KafkaDemoECR" 
end
