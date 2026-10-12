# ExamForge — Oracle Cloud Infrastructure (OCI) Deployment Guide

This guide walks you through deploying **ExamForge** to **Oracle Cloud Infrastructure (OCI)** using the **Always Free** tier (Compute VM with Docker).

---

## 📋 Overview of Deployment Architecture

```
[ Internet User / College Student ]
                │
                ▼ (HTTP Port 80 / HTTPS Port 443)
┌────────────────────────────────────────────────────────┐
│  Oracle Cloud VM (Compute Instance - Always Free)      │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  examforge-frontend (Nginx Container :80)        │  │
│  │  - Serves React Vite SPA                         │  │
│  │  - Reverse-proxies /api/ to backend              │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           │                            │
│  ┌────────────────────────▼─────────────────────────┐  │
│  │  examforge-backend (Spring Boot Java 21 :8080)   │  │
│  │  - Stateless REST API & JWT Engine               │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           │                            │
│  ┌────────────────────────▼─────────────────────────┐  │
│  │  examforge-mysql (MySQL 8.0 Container :3306)     │  │
│  │  - Flyway migrations & persistent data volume    │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

---

## ⚡ Quick 1-Command Deployment (Fastest)

If you already have an Oracle Cloud Compute Instance running (Ubuntu or Oracle Linux), SSH into it and run:

```bash
curl -fsSL https://raw.githubusercontent.com/24eg105t07-create/EXAM-FORGE/main/deploy-oracle.sh | bash
```

That script automatically:
1. Installs Docker & Docker Compose
2. Opens OS-level iptables firewall rules for Port 80 & 443
3. Clones the repository
4. Launches the full stack (`MySQL` + `Spring Boot` + `React Nginx`)
5. Displays your live public web portal link!

---

## 🛠️ Step-by-Step Oracle Cloud Setup (From Scratch)

### Step 1: Create an Oracle Cloud Compute Instance

1. Log in to [cloud.oracle.com](https://cloud.oracle.com).
2. Open the Navigation Menu ☰ ➔ **Compute** ➔ **Instances**.
3. Click **Create Instance**.
4. Configure the instance:
   * **Name**: `examforge-server`
   * **Placement**: Leave default Availability Domain.
   * **Image**: **Canonical Ubuntu 22.04 / 24.04** (or Oracle Linux 8/9).
   * **Shape**:
     * *Always Free Eligible*: **VM.Standard.A1.Flex** (Ampere ARM - 2 to 4 OCPUs, 12GB to 24GB RAM) or **VM.Standard.E2.1.Micro** (AMD x86, 1GB RAM).
   * **Networking**:
     * Primary VCN: Create new or select existing.
     * Assign a public IPv4 address: **Yes**.
   * **Add SSH keys**:
     * Select **Generate a key pair for me** and **Save Private Key** (e.g. `ssh-key.key`), or paste your existing public key.
5. Click **Create** and wait 60–90 seconds for status to change to **Running**.
6. Note down the **Public IP Address** (e.g., `129.153.xx.xx`).

---

### Step 2: Open Ingress Rules in Oracle Cloud VCN Firewall

By default, Oracle Cloud VCN blocks all ports except SSH (22). You must allow HTTP port 80:

1. On the Instance details page, click on the **Subnet** link under Primary VNIC.
2. Under **Security Lists**, click **Default Security List for...**.
3. Under **Ingress Rules**, click **Add Ingress Rules**.
4. Enter:
   * **Source Type**: `CIDR`
   * **Source CIDR**: `0.0.0.0/0`
   * **IP Protocol**: `TCP`
   * **Source Port Range**: `All`
   * **Destination Port Range**: `80,443`
   * **Description**: `Allow HTTP and HTTPS for ExamForge`
5. Click **Add Ingress Rules**.

---

### Step 3: Connect via SSH

On your computer (PowerShell or Terminal):

```powershell
ssh -i path/to/ssh-key.key ubuntu@<YOUR_ORACLE_PUBLIC_IP>
```
*(Replace `ubuntu` with `opc` if using Oracle Linux)*.

---

### Step 4: Run the Deployment Script

Once connected to your Oracle VM terminal, run:

```bash
curl -fsSL https://raw.githubusercontent.com/24eg105t07-create/EXAM-FORGE/main/deploy-oracle.sh | bash
```

The script will compile, containerize, and start all services.

---

### Step 5: Verify Deployment

Check running containers:
```bash
sudo docker compose ps
```

Expected output:
```
NAME                 IMAGE                   STATUS                   PORTS
examforge-backend    examforge-backend       Up (healthy)             8080/tcp
examforge-frontend   examforge-frontend      Up                       0.0.0.0:80->80/tcp
examforge-mysql      mysql:8.0               Up (healthy)             3306/tcp
```

---

### Step 6: Access ExamForge in Your Browser

Open your browser and navigate to:
👉 **`http://<YOUR_ORACLE_PUBLIC_IP>`**

* **API Docs**: `http://<YOUR_ORACLE_PUBLIC_IP>/swagger-ui/`
* **Admin Login**: `admin@college.edu` / `Admin@123`
* **Teacher Login**: `teacher.sharma@college.edu` / `Teacher@123`
* **Student Login**: `student.alex@college.edu` / `Student@123`

---

## 🔒 Optional: Add Free SSL / HTTPS (Let's Encrypt)

If you attach a custom domain name (e.g. `examforge.yourcollege.edu`) pointing to your Oracle Public IP:

```bash
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d examforge.yourcollege.edu
```
Certbot will automatically configure HTTPS encryption and automatic certificate renewals.
