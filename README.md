# Homelab server — requirements and design

Documentation for planning a **single homelab host** that runs **Proxmox VE** and guests for **routing (VyOS)**, **DNS filtering (Pi-hole)**, and **remote access (Tailscale)**.

## Quick links

- [Hardware requirements](docs/hardware-requirements.md)
- [Network and services](docs/network-and-services.md)
- [Agent context for assistants](agent.md)

## Status

- **Now:** documentation only.
- **Later:** Ansible automation (playbooks will live in this repo once added).

**Hardware (current):** existing **Intel tower** (Z270 / i5-7600K); **dual-port add-in NIC** (inventory: **Intel X550-T2**) — **VyOS** uses **two SR-IOV VFs** (one per port) for **WAN+LAN**; **other VMs** may use **extra VFs** where the card supports it; **onboard 1GbE** for **Proxmox management only**. **NIC model is not locked** — see [hardware requirements](docs/hardware-requirements.md) § **NIC alternatives** (e.g. **~1.3 Gbps** WAN needs **>1 GbE** to the modem). **Disks:** **Samsung 960 EVO 250 GB** = Proxmox OS · **WD SN770 1 TB** = VMs/LXCs + ISOs · **WD Red 3 TB** = bulk/NAS. Platform upgrade is **optional / later**.

## Requirements summary

1. **Proxmox VE** on bare metal.
2. **Dual-port router NIC** (inventory **X550-T2** today) — **VyOS** on **2× SR-IOV VF** (one per RJ45); **onboard 1 GbE** for **Proxmox management only**; **alternatives** in hardware doc.
3. Software: **VyOS**, **Pi-hole**, **Tailscale** — Pi-hole and Tailscale run as **dedicated LXCs** on internal **`vmbr-svc`** (Tailscale **subnet router**; not on VyOS).  
4. **LAN VLANs:** **private / guest / iot** = **VLAN 10 / 20 / 30**, subnets **`10.10.10.0/24`**, **`10.10.20.0/24`**, **`10.10.30.0/24`**; **`vmbr-svc`** = **`10.10.0.0/24`**; trunk **tagged-only** to VyOS — see [network-and-services.md](docs/network-and-services.md).  
5. **Disk roles:** small NVMe = OS, large NVMe = guests + ISOs, HDD = bulk — see [hardware requirements](docs/hardware-requirements.md) § Disk layout.
