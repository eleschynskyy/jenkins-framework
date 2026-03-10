# Jenkins on Kubernetes – deployment guide

This deploys Jenkins in a Kubernetes cluster with Configuration-as-Code (JCasC), Job DSL, and the Kubernetes plugin so pipelines run on dynamic agent pods.

## Prerequisites

- Cluster (e.g. k3d) with `kubectl` configured
- Docker image `sandbox/jenkins:latest` built and available to the cluster

## Apply order

ConfigMaps and RBAC must exist before the Deployment, and the Deployment expects a ServiceAccount named `jenkins`. Apply in this order (from repo root):

```bash
# 0. Cluster initial management
docker build --no-cache -t sandbox/jenkins:latest .
docker build --no-cache -t sandbox/jmeter:latest .
    docker run --rm -it -v "$(pwd)":/project sandbox/jmeter:latest ./run.sh
k3d cluster delete perf-sandbox
k3d cluster create perf-sandbox
k3d image import sandbox/jenkins:latest -c perf-sandbox
k3d image import sandbox/jmeter:latest -c perf-sandbox

# 1. Namespace and RBAC (ServiceAccount + Role + RoleBinding for the Kubernetes plugin)
kubectl apply -f jenkins/namespace.yaml
kubectl apply -f jenkins/jenkins-rbac.yaml

# 2. ConfigMaps (JCasC and Job DSL scripts)
kubectl create configmap jenkins-casc --from-file=casc.yaml=jenkins/casc_configs/casc.yaml -n sandbox --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap jenkins-jobs --from-file=jenkins/jobs-dsl/ -n sandbox --dry-run=client -o yaml | kubectl apply -f -

# 3. Deployment and Service
kubectl apply -f jenkins/jenkins-deployment.yaml
kubectl apply -f jenkins/jenkins-service.yaml
```

## Access

```bash
kubectl get pods -n sandbox
kubectl logs <pod> -n sandbox
kubectl port-forward -n sandbox svc/jenkins 8080:8080
```

Then open http://localhost:8080.

## Common failures

| Symptom | Cause | Fix |
|--------|--------|-----|
| Pod stays `CreateContainerConfigError` or `InvalidVariable` | Missing ConfigMap `jenkins-casc` or `jenkins-jobs` | Create both ConfigMaps before the Deployment (step 2 above). |
| Pod fails with “service account not found” | Missing ServiceAccount `jenkins` | Apply `jenkins/jenkins-rbac.yaml` before the Deployment. |
| Agents never start / “Connection refused” to Jenkins | No Service or wrong `jenkinsUrl` | Apply `jenkins/jenkins-service.yaml`. JCasC uses `http://jenkins.sandbox.svc.cluster.local:8080`. |
| Kubernetes plugin cannot create pods | SA has no RBAC | Ensure `jenkins-rbac.yaml` (Role + RoleBinding) is applied. |
| Job DSL not finding scripts | Wrong path in JCasC | In `casc.yaml`, `jobDsl.targets` must be `jobs-dsl/*.groovy` (matches volume mount path). |
| "Jenkins doesn't have label 'nft'" / Still waiting to schedule | Pipeline ran on controller; or no nft agent pods are created | **Jenkinsfile** must have `agent { label 'nft' }` at top level. **If no agent pods appear in the cluster**, see "No agent pods created" below. |

## No agent pods created (only Jenkins controller running)

If the pipeline has `agent { label 'nft' }` but no extra pods appear in the namespace and the job fails with "Jenkins doesn't have label 'nft'", the Kubernetes plugin is not creating agent pods. Do the following:

1. **Apply the latest CASC** (includes `skipTlsVerify` and timeouts for in-cluster API access):
   ```bash
   kubectl create configmap jenkins-casc --from-file=casc.yaml=jenkins/casc_configs/casc.yaml -n sandbox --dry-run=client -o yaml | kubectl apply -f -
   kubectl rollout restart deployment/jenkins -n sandbox
   ```

2. **Test the cloud** in Jenkins: **Manage Jenkins → Manage Nodes and Clouds → Configure Clouds → Kubernetes** → click **Test Connection**. It should report "Connected". If it fails, check Jenkins system log (**Manage Jenkins → System log**) for Kubernetes/API errors.

3. **Ensure images exist in the cluster** so the nft pod can start:
   ```bash
   k3d image import jenkins/inbound-agent:latest -c <cluster-name>
   k3d image import sandbox/jmeter:latest -c <cluster-name>
   ```
   Or use an image pull secret if the cluster pulls from a private registry.

4. **Check for pod creation failures**:
   ```bash
   kubectl get events -n sandbox --sort-by='.lastTimestamp'
   kubectl get pods -n sandbox
   ```
   If you see a CreateContainerConfigError, ImagePullBackOff, or an admission error (e.g. forbidden sysctls), fix the image or the pod template (e.g. remove the `yaml` sysctls block in `casc.yaml` temporarily to test).

5. **RBAC**: Ensure `jenkins/jenkins-rbac.yaml` is applied so the `jenkins` ServiceAccount can create pods in `sandbox`.

## Rebuild image and reload config

After changing plugins (`plugins.txt`), JCasC (`casc_configs/`), or Dockerfile:

```bash
docker build -t sandbox/jenkins:latest jenkins/
# If using k3d:
k3d image import sandbox/jenkins:latest -c <cluster-name>
```

After changing only JCasC or Job DSL files, you can update ConfigMaps and restart Jenkins without rebuilding:

```bash
kubectl create configmap jenkins-casc --from-file=casc.yaml=jenkins/casc_configs/casc.yaml -n sandbox --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap jenkins-jobs --from-file=jenkins/jobs/ -n sandbox --dry-run=client -o yaml | kubectl apply -f -
kubectl rollout restart deployment/jenkins -n sandbox
```

## "Jenkins doesn't have label 'nft'" – fix in the pipeline repo

Pipeline jobs **do not** support "Restrict where this project can be run" (no job-level label). The **Jenkinsfile in the repo** (e.g. test_template) must allocate an `nft` agent from the start.

**Scripted pipeline** – start the file with:
```groovy
node('nft') {
  // rest of your pipeline (checkout, stages, etc.)
}
```

**Declarative pipeline** – use:
```groovy
pipeline {
  agent { label 'nft' }
  stages {
    // ...
  }
}
```

If the Jenkinsfile does not start with `node('nft')` or `agent { label 'nft' }`, the run starts on the Jenkins controller and fails when it hits a later `node('nft')` block.

## Run JMeter in Kubernetes agent (no Docker daemon)

The `nft` pod template includes a second container named `jmeter` (`sandbox/jmeter:latest`).
Use Jenkins Kubernetes `container(...)` step instead of Docker Pipeline `withDockerContainer(...)`:

```groovy
node('nft') {
  container('jmeter') {
    sh 'jmeter -v'
    sh '<commands to be executed inside the jmeter container>'
  }
}
```

This avoids `docker: not found` and does not require Docker-in-Docker or host socket mounts.

### Migrating from withDockerContainer(image, args)

Old pattern:

```groovy
steps.withDockerContainer(image: dockerImage, args: dockerArgs) {
  steps.sh '<commands>'
}
```

New Kubernetes-native pattern:

```groovy
steps.withEnv(["JMETER_EXTRA_ARGS=${dockerArgs ?: ''}"]) {
  steps.container('jmeter') {
    steps.sh '<commands>'
  }
}
```

Notes:
- `-e VAR=...` style docker args -> use `withEnv(...)` / pod `envVars`
- `-v ...` mounts -> define under pod template `volumes` in JCasC
- CPU/memory docker flags -> define container requests/limits in JCasC
