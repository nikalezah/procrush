# Create kind cluster, install ingress-nginx, build images, deploy ProCrush.
$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$K8sDir = Resolve-Path (Join-Path $PSScriptRoot "..")
$ClusterName = if ($env:KIND_CLUSTER_NAME) { $env:KIND_CLUSTER_NAME } else { "procrush" }
$IngressUrl = "https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.0/deploy/static/provider/kind/deploy.yaml"

function Require-Command($name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $name"
    }
}

Require-Command kind
Require-Command kubectl
Require-Command docker

$clusters = kind get clusters 2>$null
if ($clusters -notcontains $ClusterName) {
    Write-Host "Creating kind cluster $ClusterName ..."
    kind create cluster --name $ClusterName --config (Join-Path $K8sDir "kind-config.yaml")
} else {
    Write-Host "Kind cluster $ClusterName already exists."
}

kubectl config use-context "kind-$ClusterName"

Write-Host "Installing ingress-nginx ..."
kubectl apply -f $IngressUrl
Write-Host "Waiting for ingress-nginx controller (admission jobs + deployment rollout) ..."
kubectl wait --namespace ingress-nginx `
    --for=condition=complete job/ingress-nginx-admission-create `
    --timeout=120s
kubectl wait --namespace ingress-nginx `
    --for=condition=complete job/ingress-nginx-admission-patch `
    --timeout=120s
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=180s

& (Join-Path $PSScriptRoot "build-images.ps1")

Write-Host "Applying Kubernetes manifests ..."
kubectl apply -k (Join-Path $K8sDir "overlays\kind")

Write-Host ""
Write-Host "Waiting for application pods (this may take several minutes on first start) ..."
kubectl wait --namespace procrush `
    --for=condition=ready pod `
    --selector=app=api `
    --timeout=600s 2>$null

kubectl get pods -n procrush

Write-Host ""
Write-Host "ProCrush is deploying."
Write-Host "Add to hosts file:  127.0.0.1 procrush.local"
Write-Host "Open:               http://procrush.local"
Write-Host "API health:         http://procrush.local/api/auth/me (401 without session is OK)"
Write-Host ""
Write-Host "RabbitMQ UI (optional): kubectl port-forward -n procrush svc/rabbitmq 15672:15672"
