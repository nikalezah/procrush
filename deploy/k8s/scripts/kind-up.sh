#!/usr/bin/env bash
# Create kind cluster, install ingress-nginx, build images, deploy ProCrush.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
K8S_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CLUSTER_NAME="${KIND_CLUSTER_NAME:-procrush}"
INGRESS_URL="https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/kind/deploy.yaml"

require() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

require kind
require kubectl
require docker

if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER_NAME"; then
  echo "Creating kind cluster $CLUSTER_NAME ..."
  kind create cluster --name "$CLUSTER_NAME" --config "$K8S_DIR/kind-config.yaml"
else
  echo "Kind cluster $CLUSTER_NAME already exists."
fi

kubectl config use-context "kind-$CLUSTER_NAME"

echo "Installing ingress-nginx ..."
kubectl apply -f "$INGRESS_URL"
echo "Waiting for ingress-nginx controller (admission jobs + deployment rollout) ..."
kubectl wait --namespace ingress-nginx \
  --for=condition=complete job/ingress-nginx-admission-create \
  --timeout=120s
kubectl wait --namespace ingress-nginx \
  --for=condition=complete job/ingress-nginx-admission-patch \
  --timeout=120s
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=180s

"$ROOT/deploy/k8s/scripts/build-images.sh"

echo "Applying Kubernetes manifests ..."
kubectl apply -k "$K8S_DIR/overlays/kind"

echo ""
echo "Waiting for application pods (this may take several minutes on first start) ..."
kubectl wait --namespace procrush \
  --for=condition=ready pod \
  --selector=app=api \
  --timeout=600s || true
kubectl get pods -n procrush

echo ""
echo "ProCrush is deploying."
echo "Add to /etc/hosts:  127.0.0.1 procrush.local"
echo "Open:               http://procrush.local"
echo "API health:         http://procrush.local/api/auth/me (401 without session is OK)"
echo ""
echo "RabbitMQ UI (optional): kubectl port-forward -n procrush svc/rabbitmq 15672:15672"
