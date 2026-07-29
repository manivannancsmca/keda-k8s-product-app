# Product Service - KEDA Autoscaling

## How this ScaledObject works

- KEDA creates an HPA named `keda-hpa-product-service-scaler`.
- It queries three metric sources every **15 seconds**.
- Each trigger evaluates independently. The HPA uses the **maximum** desired replica count across all triggers.
- **Scale-up** is aggressive (can double pods in 15s) to handle traffic spikes.
- **Scale-down** is conservative (5-minute stabilization window) to prevent flapping.

---

## Step-by-Step Deployment Guide

### Step 1: Build the Application

```bash
# Navigate to project root
cd product-service

# Build the JAR
mvn clean package -DskipTests

# Build Docker image
docker build -t product-service:1.0.0 .
```

```bash
# Create cluster
kind create cluster --config kind-config.yaml

# Load image into Kind
kind load docker-image product-service:1.0.0 --name product-cluster

# Verify cluster
kubectl cluster-info
kubectl get nodes
```

### Install Metrics Server

The Metrics Server is mandatory for HPA. It collects resource metrics (CPU/Memory) from Kubelets.

```bash
# Install Metrics Server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Patch Metrics Server to work in Kind (skip TLS verification)
kubectl patch deployment metrics-server -n kube-system --type='json' -p='[
  {
    "op": "add",
    "path": "/spec/template/spec/containers/0/args/-",
    "value": "--kubelet-insecure-tls"
  }
]'

# Wait for rollout
kubectl rollout status deployment/metrics-server -n kube-system

# Verify
kubectl top nodes
kubectl top pods -n kube-system
```

#### Platform-specific patch commands

**Windows Command Prompt (cmd.exe)** — run as a single line:

```cmd
kubectl patch deployment metrics-server -n kube-system --type=json -p="[{\"op\":\"add\",\"path\":\"/spec/template/spec/containers/0/args/-\",\"value\":\"--kubelet-insecure-tls\"}]"
```

**PowerShell** — use single quotes around the JSON:

```powershell
kubectl patch deployment metrics-server -n kube-system --type=json -p '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
```

**Git Bash**:

```bash
kubectl patch deployment metrics-server \
  -n kube-system \
  --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
```

### Install KEDA

```bash
# Add Helm repo (if using Helm)
helm repo add kedacore https://kedacore.github.io/charts
helm repo update

# Install KEDA
helm install keda kedacore/keda --namespace keda --create-namespace

# Or use raw manifests
# kubectl apply --server-side -f https://github.com/kedacore/keda/releases/download/v2.14.0/keda-2.14.0.yaml

# Verify installation
kubectl get pods -n keda
# Should show: keda-operator, keda-operator-metrics-apiserver, keda-admission-webhooks
```

### Deploy MySQL

```bash
kubectl apply -f k8s/mysql-secret.yaml
kubectl apply -f k8s/mysql-configmap.yaml
kubectl apply -f k8s/mysql-service.yaml
kubectl apply -f k8s/mysql-statefulset.yaml

# Wait for MySQL to be ready
kubectl rollout status statefulset/mysql

# Verify PVC was created
kubectl get pvc
```

### Deploy Product Service

```bash
kubectl apply -f k8s/product-configmap.yaml
kubectl apply -f k8s/product-deployment.yaml

# Wait for deployment
kubectl rollout status deployment/product-service

# Verify pods
kubectl get pods -l app=product-service
kubectl logs -l app=product-service --tail=50
```

### Deploy KEDA ScaledObject

```bash
kubectl apply -f k8s/keda-scaledobject.yaml

# Verify KEDA created the HPA
kubectl get hpa

# Check ScaledObject status
kubectl get scaledobject
kubectl describe scaledobject product-service-scaler
```

---

## Verification and Testing

```bash
# Port-forward to test locally
kubectl port-forward svc/product-service 8080:8080 &

# Test health endpoint
curl http://localhost:8080/actuator/health

# Create a product
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro M5",
    "description": "High-performance laptop",
    "price": 2499.99,
    "stockQuantity": 50,
    "category": "ELECTRONICS",
    "imageUrl": "https://example.com/macbook.jpg"
  }'

# Get all products
curl http://localhost:8080/api/v1/products

# Get by category
curl http://localhost:8080/api/v1/products/category/ELECTRONICS

# Get categories
curl http://localhost:8080/api/v1/products/categories

# Get statistics
curl http://localhost:8080/api/v1/products/statistics
```

### Verify Metrics Exposure

```bash
# Check Prometheus metrics
```

**Linux:**

```bash
curl http://localhost:8080/actuator/prometheus | grep http_server_requests
```

**Windows (cmd):**

```cmd
curl http://localhost:8085/actuator/prometheus | findstr http_server_requests
```

**PowerShell:**

```powershell
(curl http://localhost:8085/actuator/prometheus).Content | Select-String "http_server_requests"
```

```bash
# Verify Metrics Server sees pod metrics
kubectl top pod -l app=product-service
```

### Test Autoscaling

Install a load testing tool:

```bash
# Using hey (HTTP load generator)
# Install: go install github.com/rakyll/hey@latest
```

**Linux:**

```bash
# Or use kubectl run with a simple shell loop
kubectl run -it --rm load-generator --image=busybox:1.36 --restart=Never -- /bin/sh -c "
  while true; do
    wget -q -O- http://product-service:8080/api/v1/products
  done
"
```

**Windows:**

```bash
kubectl delete pod load-generator --force --grace-period=0
kubectl run -it --rm load-generator --image=busybox:1.36 --restart=Never -- /bin/sh -c "while true; do wget -q -O- http://product-service:8080/api/v1/products; done"
```

In another terminal, watch scaling:

```bash
# Watch pods scale up
watch kubectl get pods -l app=product-service

# Watch HPA events
kubectl get hpa -w
```

**Expected behavior:**

1. Load starts → CPU/memory increases.
2. After ~15 seconds (`pollingInterval`), KEDA detects high load.
3. HPA scales pods from 1 → 2 → 4 → up to 10.
4. When load stops, after 5 minutes (`cooldownPeriod`), pods scale back down to 1.

```bash
# Watch KEDA logs
kubectl logs -n keda -l app=keda-operator --tail=100 -f
```
