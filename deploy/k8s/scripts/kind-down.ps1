$ErrorActionPreference = "Stop"

$ClusterName = if ($env:KIND_CLUSTER_NAME) { $env:KIND_CLUSTER_NAME } else { "procrush" }

$clusters = kind get clusters 2>$null
if ($clusters -contains $ClusterName) {
    Write-Host "Deleting kind cluster $ClusterName ..."
    kind delete cluster --name $ClusterName
} else {
    Write-Host "Kind cluster $ClusterName does not exist."
}
