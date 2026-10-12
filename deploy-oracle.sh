#!/usr/bin/env bash
# ==============================================================================
# ExamForge — Automated Deployment Script for Oracle Cloud Infrastructure (OCI)
# Works on: Ubuntu 22.04/24.04 LTS & Oracle Linux 8/9 (x86_64 or Ampere A1 ARM64)
# ==============================================================================

set -euo pipefail

echo "=========================================================="
echo "   🚀 Starting ExamForge Deployment on Oracle Cloud VM   "
echo "=========================================================="

# 1. Update OS packages and install prerequisites
echo "==> [1/5] Updating system packages & dependencies..."
if [ -f /etc/oracle-release ] || [ -f /etc/redhat-release ]; then
    sudo dnf update -y
    sudo dnf install -y git curl dnf-plugins-core
    # Docker on Oracle Linux
    if ! command -v docker &> /dev/null; then
        sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
        sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
        sudo systemctl enable --now docker
    fi
else
    # Ubuntu / Debian
    sudo apt-get update -y
    sudo apt-get install -y ca-certificates curl gnupg lsb-release git iptables-persistent
    if ! command -v docker &> /dev/null; then
        sudo install -m 0755 -d /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
        sudo chmod a+r /etc/apt/keyrings/docker.gpg
        echo \
          "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
          $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
        sudo apt-get update -y
        sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
        sudo systemctl enable --now docker
    fi
fi

# Add current user to docker group
sudo usermod -aG docker "$USER" || true

# 2. Configure Host Firewall (OCI VMs enforce strict iptables by default)
echo "==> [2/5] Opening HTTP (80) & HTTPS (443) in OS firewall..."
if command -v ufw &> /dev/null; then
    sudo ufw allow 80/tcp || true
    sudo ufw allow 443/tcp || true
fi

# Oracle Cloud Ubuntu images enforce iptables rules blocking incoming port 80:
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT || true
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT || true
if command -v netfilter-persistent &> /dev/null; then
    sudo netfilter-persistent save || true
fi

# 3. Pull latest code from GitHub
REPO_DIR="$HOME/EXAM-FORGE"
REPO_URL="https://github.com/24eg105t07-create/EXAM-FORGE.git"

echo "==> [3/5] Setting up project repository at $REPO_DIR..."
if [ -d "$REPO_DIR/.git" ]; then
    echo "Updating existing repository..."
    cd "$REPO_DIR"
    git pull origin main
else
    git clone "$REPO_URL" "$REPO_DIR"
    cd "$REPO_DIR"
fi

# 4. Prepare Environment Variables
if [ ! -f .env ]; then
    echo "Creating .env configuration..."
    cp .env.example .env
fi

# 5. Build and launch containers
echo "==> [4/5] Building and starting Docker containers..."
sudo docker compose down --remove-orphans || true
sudo docker compose up -d --build

# 6. Verify Service Status
echo "==> [5/5] Checking deployment status..."
sleep 5
sudo docker compose ps

PUBLIC_IP=$(curl -s https://api.ipify.org || echo "<YOUR_ORACLE_PUBLIC_IP>")

echo ""
echo "=========================================================="
echo "   🎉 ExamForge Successfully Deployed on Oracle Cloud!   "
echo "=========================================================="
echo "   🌐 Web Portal:    http://$PUBLIC_IP"
echo "   📄 API Docs:      http://$PUBLIC_IP/swagger-ui/"
echo "   🔐 Default Admin: admin@college.edu / Admin@123"
echo "=========================================================="
echo ""
echo "Note: Ensure you have added an Ingress Rule in Oracle Cloud:"
echo "VCN -> Default Security List -> Add Ingress Rule:"
echo "  - Source CIDR: 0.0.0.0/0"
echo "  - IP Protocol: TCP"
echo "  - Destination Port: 80, 443"
echo "=========================================================="
