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
k3d cluster delete perf-sandbox
k3d cluster create perf-sandbox
k3d image import sandbox/jenkins:latest -c perf-sandbox

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
