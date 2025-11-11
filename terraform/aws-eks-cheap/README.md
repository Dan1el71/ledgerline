# AWS EKS (Low-Cost) Terraform

This Terraform project creates a cost-optimized EKS cluster for Ledgerline.

## What this creates

- VPC with 2 public subnets across 2 AZs.
- No NAT Gateway (cost reduction).
- EKS control plane.
- One EKS managed node group (Spot, x86_64 by default).

## Cost-oriented defaults

- `node_capacity_type = "SPOT"`
- `node_instance_types = ["t3.medium"]`
- `node_desired_size = 1`
- No NAT Gateway.

These defaults reduce cost, but they also reduce resilience and performance headroom.

## Architecture decision: x86_64 nodes

The CI Docker job builds on `ubuntu-latest` (amd64) and does not request `platforms:` or
multi-arch manifests. The EKS node group therefore uses `AL2023_x86_64_STANDARD` with `t3.*`
instances (and `db.t3.*` / `cache.t3.*` for the managed data services) so that every workload
can run the images produced by CI.

If you switch to Graviton (`t4g.*`/`m6i.*`), change the AMI type in `main.tf` and every instance
class in `variables.tf`/`terraform.tfvars.example`, then add `platforms: linux/amd64,linux/arm64`
to the Docker build job in `.github/workflows/ci.yml` (or build both images separately). Keep the
node architecture and image platforms in sync; a mismatch fails at pod start with
`exec format error`.

## Pricing references (verified on February 28, 2026)

- EKS control plane standard support: **$0.10 per cluster-hour** (extended support: **$0.60 per cluster-hour**):
  - https://aws.amazon.com/eks/pricing/
- Spot Instances can provide **up to 90% discount** vs On-Demand:
  - https://aws.amazon.com/ec2/spot/
- T4g delivers **up to 40% better price performance** over T3 for supported workloads:
  - https://aws.amazon.com/ec2/instance-types/t4/
- NAT Gateway has hourly + data processing charges (example shown on AWS pricing page):
  - https://aws.amazon.com/en/vpc/pricing/
- Public IPv4 addresses are billed hourly (examples show $0.005/hour per public IPv4):
  - https://aws.amazon.com/es/vpc/pricing/

## Prerequisites

- Terraform 1.6+
- AWS credentials configured (for example via `aws configure`)
- AWS CLI v2
- `kubectl`

## Usage

1. Copy example variables:

```bash
cp terraform.tfvars.example terraform.tfvars
```

2. Initialize and deploy:

```bash
terraform init
terraform plan
terraform apply
```

3. Configure kubeconfig:

```bash
aws eks update-kubeconfig --region <your-region> --name <output-cluster-name>
```

4. Deploy app manifests:

```bash
kubectl apply -k ../../k8s/all
```

## Important tradeoffs

- This is **dev/test oriented**.
- Spot capacity can be interrupted.
- Single node means low fault tolerance.
- Public subnet nodes reduce cost but are less secure than private-subnet patterns.

For stronger production posture, switch to:

- `node_capacity_type = "ON_DEMAND"` (or mixed)
- at least 2 nodes across AZs
- private subnets + NAT Gateway
- managed data services (RDS/MSK/ElastiCache)

If you prefer x86_64 nodes to simplify image builds, set `node_instance_types` to values like `t3.medium` and update the AMI type in `main.tf`.

## Cleanup

```bash
terraform destroy
```
