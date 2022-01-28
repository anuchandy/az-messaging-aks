
The document describes setting up an AKS cluster, developer tools, deploying a Java Spring App (hosting EventHubs API) to AKS, and profiling the App running on ASK using JFR.s

## Install Docker

https://www.docker.com/products/docker-desktop

## Setup aks Cluster

### The env vars for Az CLI commands

Let's define a few env variables (scoped to the current terminal) that the Az CLI commands refers later.

Make sure to provide appropriate values for these env vars

```
set resource_group=anuchan-rg1-aks1
set location=eastus
set container_registry=anuchanaksacr
set aks_cluster=anuchan-akscluster
```

### Az CLI commands to create resources (resource group, container registry, aks cluster)
```
az group create --name=%resource_group% --location=%location%

az acr create --resource-group %resource_group% --location %location% --name %container_registry% --sku Basic

az config set defaults.acr=%container_registry%
az acr login

az aks create --resource-group=%resource_group% --name=%aks_cluster% --dns-name-prefix=%aks_cluster% --attach-acr %container_registry% --generate-ssh-keys
```


### Install kube CLI (kubectl, kubelogin)

```
az aks install-cli
```

> Required to add 'C:\Users\<user-name>\.azure-kubectl' and 'C:\Users\<user-name>\.azure-kubelogin' to PATH. Output of the command has the instructions.

### Connect kube CLI to the aks cluster

```
az aks get-credentials --resource-group=%resource_group% --name=%aks_cluster% --overwrite
```

> This command sets the aks cluster as current context in C:\Users\<user-name>\.kube\config


## Create aks namespace and assign secrets

```
set aks_namespace=anuchan-eh-app-ns
```

```
kubectl create namespace %aks_namespace%
```

### Create secrets.yml file locally

```yaml
apiVersion: v1
data:
  AZURE_EVENTHUBS_CONNECTION_STRING: <eventhubs-connection-string-base65-encoded>
  AZURE_STORAGE_CONN_STR: <storage-connection-string-base65-encoded>
kind: Secret
metadata:
  name: java-eh-app-secret
  namespace: <aks-namespace>
type: Opaque
```

Update <aks-namespace> to use the aks namespace we created above.

The metatdata.name value i.e. 'java-eh-app-secret' is an identifier for the secrets; this identifier will be referenced from aks `job.yml` definition (more on that later).

### Base64 encode the secrets

Base 64 encode the secrets i.e. EventHubs and storage connection strings.

> Below we used WSL shell in Windows but any linux shell will do

Input to echo should be in single quotes

```
echo '<eventhubs-connection-string>' | base64
```

Use the output to replace `<eventhubs-connection-string-base65-encoded>` in secrets.yml

```
echo '<storage-connection-string>' | base64
```

Use the output to replace `<storage-connection-string-base65-encoded>` in secrets.yml

### Assign the secrets to the aks namespace

```
kubectl apply -f <absolute-path-to>/secrets.yml
```

## Create the docker image with messaging Spring boot App

```
cd <path-to>\eventhubs
```

> Note: Currently, the code in Java Spring App is not using any EventHubs API. It serves as a template to fill in code with the EH scenario(s) you want to try in AKS containers.

```
mvn clean package spring-boot:repackage -DeventhubVersion='5.10.4'
```

```
docker build -t %container_registry%.azurecr.io/eventhubs-eventhubs-scenarios-5.10.4:latest .
```

## Push the docker image to ACR linked to AKS

```
az acr login -n %anuchanaksacr%.azurecr.io

docker push %container_registry%.azurecr.io/eventhubs-eventhubs-scenarios-5.10.4:latest
```

## Deploy containers (based on docker image) in AKS

### Make necessary update to job.yml

The `job.yml` contains the definition for the containers we want to deploy in the aks namespace using the docker image (with the java spring app) we pushed to acr.

Open the `job.yml` file

1. The value of `metadata.namespace` is 'anuchan-eh-app-ns'; replace it with the aks namespace created above.
2. The value of `spec.template.spec.nodeSelector.agentpool` is 'nodepool1'; replace it with the aks pool name appear in the portal.
3. Search for AZURE_EVENTHUBS_EVENT_HUB_NAME; its value is `eh_parition_32_0`, replace it with the name of the EventHubs resource in your EventHubs namespace.
4. Search for `image:`, its value is 'anuchanaksacr.azurecr.io/eventhubs-eventhubs-scenarios-5.10.4:latest', replace it with the name of the docker image we pushed to ACR earlier.

> You can also see how the secrets AZURE_EVENTHUBS_CONNECTION_STRING and AZURE_STORAGE_CONN_STR we created earlier (via secrets.yml) is referenced in this job.yml.

### Apply job.yml to deploy containers.

Two containers will be deployed as described in the `job.yml`, each running one instance of "Java Messaging Spring App" included in the docker image.s

The name of the first container is `receiver` and the second one is `sender` (as named in the job.yml).

```
kubectl create -f <absolute-path-to>\job.yml
```

#### Checking container deployment status

```
kubectl get pods -n %aks_namespace%
```

* output:
```
NAME                         READY   STATUS    RESTARTS   AGE
java-eh-app-zzjjq             2/2     Running   0          1h
```

The pod prefix `java-eh-app-` is derived from the value of `metadata.name` in job.yml.
It shows both the containers (2/2) are ready and running.

#### Redirecting the stdout of containers in the pod

If the Java program is writing to stdout (system.out.println), that can be redirected to your terminal using the below command.

The value of -c option is the name for the container (defined in job.yml).

```
kubectl logs -n %aks_namespace% java-eh-app-zzjjq -c receiver -f

kubectl logs -n %aks_namespace% java-eh-app-zzjjq -c sender -f
```

#### Copying slf4j log files

The Java Spring App has log4j logging enabled; the following command shows how to copy log files to your machine from the pod container `receiver`.

```
kubectl cp  -n %aks_namespace% -c receiver java-eh-app-zzjjq:/workspace/logs/debug.log debug.log
```

### Deleting the pod and containers

to delete the deployment run the following command

```
kubectl delete -f <absolute-path-to>\job.yml
```

### Enabling JFR on container and connecting from dev box

The JAVA_TOOL_OPTIONS variable in `job.yml` enables the JMX and
Flight Recorder for the `receiver` and `sender` containers.

For `receiver` container port 1088 is declared to accept JMX connection from a remote host (e.g., from our dev box), similarly 1099 for `sender` container.

To connect to the flight recorder running on the containers using Java/Azul Mission Control in your box, we first need to forward the ports (e.g., 1088 and 1099) to port in your dev box. Use the following command for port forwarding -

```
kubectl port-forward -n %aks_namespace% java-eh-app-zzjjq 1088:1088

kubectl port-forward -n %aks_namespace% java-eh-app-zzjjq 1099:1099
```

Now you can connect to these local port to download JFR recordings in the containers

* Init connection

<img width="610" alt="Create_Connection_Button" src="https://user-images.githubusercontent.com/1471612/151476804-7638971b-5415-4342-8a1d-d0cc55d1521b.png">

* Create connection to local port

<img width="701" alt="JMX_Connection_To_1088" src="https://user-images.githubusercontent.com/1471612/151476586-02d71494-f5bc-4c69-b540-ef0f63afe811.png">

* Dump the JFR recording

<img width="486" alt="GetRecordings" src="https://user-images.githubusercontent.com/1471612/151476479-64111905-f648-4620-8412-5771be7d16c3.png">
