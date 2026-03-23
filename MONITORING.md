# InfluxDB 2 and Grafana on Kubernetes

This adds InfluxDB 2 and Grafana to the `sandbox` namespace with persistent storage for InfluxDB, a pre-configured InfluxDB datasource in Grafana, and dashboard provisioning from JSON so you can re-use exported dashboards on every deploy.

## Prerequisites

- Cluster and namespace `sandbox` (e.g. `kubectl apply -f jenkins/namespace.yaml`)
- You will create InfluxDB buckets yourself after first run

## 1. InfluxDB 2

### Create the auth secret (required before first deploy)

InfluxDB init uses this secret for the initial admin user and token. Use the same token value in Grafana datasource provisioning later.

```bash
kubectl create secret generic influxdb-auth -n sandbox \
  --from-literal=admin-password=YOUR_ADMIN_PASSWORD \
  --from-literal=admin-token=YOUR_INFLUXDB_TOKEN
```

Example (change in production):

```bash
kubectl create secret generic influxdb-auth -n sandbox \
  --from-literal=admin-password=admin \
  --from-literal=admin-token=my-super-secret-auth-token
```

### Deploy InfluxDB (PVC + Deployment + Service)

```bash
kubectl apply -f influxdb/pvc.yaml
kubectl apply -f influxdb/deployment.yaml
kubectl apply -f influxdb/service.yaml
```

- Data is stored in PVC `influxdb-data` (e.g. 10Gi). Adjust `influxdb/pvc.yaml` if needed.
- First run performs one-time setup (org `sandbox`, bucket `default`, admin user). Create extra buckets in the InfluxDB UI or API later.

### Access InfluxDB

```bash
kubectl port-forward -n sandbox svc/influxdb 8086:8086
```

Open http://localhost:8086 and log in with the admin user and password from the secret.

---

## 2. Grafana

### Create provisioning ConfigMaps

Grafana needs three ConfigMaps: datasources, dashboard provider, and dashboard JSONs.

**a) Datasource (InfluxDB)**  
Replace `__INFLUXDB_TOKEN__` in `grafana/provisioning/datasources/datasources.yaml` with the same token you used in `influxdb-auth` (e.g. `my-super-secret-auth-token`). Then:

```bash
kubectl create configmap grafana-datasources -n sandbox \
  --from-file=datasources.yaml=grafana/provisioning/datasources/datasources.yaml \
  --dry-run=client -o yaml | kubectl apply -f -
```

**b) Dashboard provider**  
Tells Grafana to load JSON from `/etc/grafana/provisioning/dashboards/json`:

```bash
kubectl create configmap grafana-dashboard-provider -n sandbox \
  --from-file=default.yaml=grafana/provisioning/dashboards/default.yaml \
  --dry-run=client -o yaml | kubectl apply -f -
```

**c) Dashboards (JSON)**  
Place exported dashboard JSON files in `grafana/dashboards/`. Then create/update the ConfigMap:

```bash
# If grafana/dashboards/ is empty, create an empty ConfigMap first so the mount exists:
kubectl create configmap grafana-dashboards -n sandbox --from-file=grafana/dashboards/ --dry-run=client -o yaml | kubectl apply -f -
```

If you already have JSON files in `grafana/dashboards/` (e.g. `my-dashboard.json`), the same command adds them. Grafana will load all JSON files from that ConfigMap.

### Deploy Grafana

```bash
kubectl apply -f grafana/deployment.yaml
kubectl apply -f grafana/service.yaml
```

Default login: user `admin`, password `admin` (set in deployment; override with a secret for production).

### Access Grafana

```bash
kubectl port-forward -n sandbox svc/grafana 3000:3000
```

Open http://localhost:3000. The InfluxDB datasource should already be configured.

---

## 3. Exporting and re-using dashboards (JSON)

1. In Grafana UI: create or edit dashboards, then **Dashboard settings (gear) → JSON Model → Copy to clipboard** (or **Save as file**).
2. Save the JSON into the repo under `grafana/dashboards/`, e.g. `grafana/dashboards/jenkins-metrics.json`.
3. Recreate the dashboards ConfigMap and restart Grafana so it picks up the new file:

   ```bash
   kubectl create configmap grafana-dashboards -n sandbox --from-file=grafana/dashboards/ --dry-run=client -o yaml | kubectl apply -f -
   kubectl rollout restart deployment/grafana -n sandbox
   ```

4. On a fresh cluster, run the same ConfigMap create and deploy steps; Grafana will load all JSONs from `grafana/dashboards/` and the dashboards will appear again.

**Tip:** Remove or anonymize sensitive bits from exported JSON (e.g. UIDs) if you want stable, version-controlled dashboards. You can also set the datasource in the JSON to the provisioned datasource name (`InfluxDB`) so it works after re-deploy.

---

## Apply order (full stack)

From repo root:

```bash
# Namespace (if not already)
kubectl apply -f jenkins/namespace.yaml

# InfluxDB
kubectl create secret generic influxdb-auth -n sandbox --from-literal=admin-password=admin --from-literal=admin-token=my-token
kubectl apply -f influxdb/pvc.yaml
kubectl apply -f influxdb/deployment.yaml
kubectl apply -f influxdb/service.yaml

# Grafana provisioning (after editing datasources.yaml to set the token)
kubectl create configmap grafana-datasources -n sandbox --from-file=datasources.yaml=grafana/provisioning/datasources/datasources.yaml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboard-provider -n sandbox --from-file=default.yaml=grafana/provisioning/dashboards/default.yaml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboards -n sandbox --from-file=grafana/dashboards/ --dry-run=client -o yaml | kubectl apply -f -

# Grafana
kubectl apply -f grafana/deployment.yaml
kubectl apply -f grafana/service.yaml
kubectl describe pod -n sandbox -l app=grafana
```

---

## Optional: Grafana admin password from secret

For production, store the Grafana admin password in a secret and reference it in `grafana/deployment.yaml`:

```bash
kubectl create secret generic grafana-admin -n sandbox --from-literal=admin-password=YOUR_PASSWORD
```

Then in `grafana/deployment.yaml`, replace the `GF_SECURITY_ADMIN_PASSWORD` env value with:

```yaml
- name: GF_SECURITY_ADMIN_PASSWORD
  valueFrom:
    secretKeyRef:
      name: grafana-admin
      key: admin-password
```
