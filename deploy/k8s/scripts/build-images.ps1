# Build ProCrush application images and load them into the kind cluster.
$ErrorActionPreference = "Stop"

$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")
$ClusterName = if ($env:KIND_CLUSTER_NAME) { $env:KIND_CLUSTER_NAME } else { "procrush" }

Set-Location $Root

$images = @(
    @{ Tag = "procrush-api:local"; Dockerfile = "deploy/Dockerfile.api" },
    @{ Tag = "procrush-personality:local"; Dockerfile = "deploy/Dockerfile.personality" },
    @{ Tag = "procrush-matching:local"; Dockerfile = "deploy/Dockerfile.matching" },
    @{ Tag = "procrush-frontend:local"; Dockerfile = "deploy/Dockerfile.frontend" }
)

foreach ($image in $images) {
    Write-Host "Building $($image.Tag) ..."
    docker build -f $image.Dockerfile -t $image.Tag .
    Write-Host "Loading $($image.Tag) into kind cluster $ClusterName ..."
    kind load docker-image $image.Tag --name $ClusterName
}

Write-Host "All images built and loaded."
