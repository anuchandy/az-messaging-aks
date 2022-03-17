## Emulating network failures

This branch has a version of the project which enables us to troubleshoot the ServiceBus and EventHubs recovery routes under network failure emulation using docker container. The `main` branch is all about deploying to AKS+debugging, network failure emulation was not a topic.

1. Unlike the `main` branch, the project in this branch is supposed to run on a local docker container.
2. The DockerFile in this branch is different from the main in the following ways -
    - It enables iproute2, iptables, and tc so that, from the local machine, we can drive the network failures in the docker container running the SB|EH send-receive scenarios.
    - Since we're running as a docker instance the DockerFile defines "entrypoint" for JVM.

## How to run the project and emulate network - Using SB as an example

1. Make sure Docker is installed and running in the local machine.
2. Install [pumba](https://github.com/alexei-led/pumba/releases) in the local machine; it's the tool we use to drive network failures in the docker container from the local machine.
3. From the terminal switch to the 'azservicebus' directory.
4. Check the "entrypoint" in DockerFile to ensure it has the correct scenario you want to emulate network failure.

> ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher", "--scenario=ReceiveAsync"]

5. Create a file "docker-env-vars.txt" in this directory and defines the env vars the scenario uses.

```
AZURE_SERVICEBUS_CONNECTION_STRING=Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<key>
AZURE_SERVICEBUS_QUEUE_NAME=<queue-name>
AZURE_LOG_LEVEL=VERBOSE
```

6. Build the project

> mvn clean package spring-boot:repackage -DservicebusVersion='7.7.0-beta.1'

<details><summary>Note</summary>

Note: -DservicebusVersion points to the version of ServiceBus SDK to use. If we want to test a version that is not released yet, then MVN installs it on the local machine and specifies its version.

> sdk_root$ mvn clean install --batch-mode -Dmaven.wagon.http.pool=false --settings ./eng/settings.xml -DskipTests -Dinclude-template  -Dgpg.skip -Dmaven.javadoc.skip=true -Dcheckstyle.skip=false -Dspotbugs.skip=false -Drevapi.skip=true -Denforcer.skip=false -pl com.azure:azure-messaging-servicebus -am

> [INFO] Microsoft Azure client library for Service Bus 7.7.0-beta.1 SUCCESS [01:35 min]

</details>

7. Build the docker image

> Docker build -f Dockerfile -t anutc/sb-recovery-network .

8. Run the docker container using this image and referring the file containing the env vars (step 5).

> docker run --rm --name anutcontainer --env-file docker-env-vars.txt anutc/sb-recovery-network

The --name is optional; without that, Docker will generate a unique name for the container. We specified --name so that we don't have to copy-paste the generated name when driving network emulation on the container.

9. From a different terminal in the local machine, run pumba, an example command looks like this:

> pumba netem --duration 10m delay --time 120000 anutcontainer

The last argument refers to the docker container we named in step 8.
The command adds 2 minutes (120000 ms) delay for all outgoing packets for 10 minutes in the target container instance.

Another example:

> pumba netem --duration 10m corrupt --percent 60 anutcontainer

The command corrupts 60% of the TCP packages for 10 minutes.

For a full list of commands, refer offical pumba [readme](https://github.com/alexei-led/pumba)
