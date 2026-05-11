# Homelab server — requirements and design

Documentation for planning a **single homelab host** that runs **Proxmox VE** and guests for **routing (VyOS)**, **DNS filtering (Pi-hole)**, and **remote access (Tailscale)**.

## Quick links

- [Hardware requirements](docs/hardware-requirements.md)
- [Network and services](docs/network-and-services.md)
- [Agent context for assistants](agent.md)

## Status

- **Now:** documentation only.
- **Later:** Ansible automation (playbooks will live in this repo once added).

**Hardware (current):** existing **Intel tower** (Z270 / i5-7600K); **Intel X550-T2** — **VyOS** uses **two SR-IOV VFs** (one per port) for **WAN+LAN**; **other VMs** may use **extra VFs** each where needed; **onboard 1GbE** for **Proxmox management only**. **Disks:** **Samsung 960 EVO 250 GB** = Proxmox OS · **WD SN770 1 TB** = VMs/LXCs + ISOs · **WD Red 3 TB** = bulk/NAS. Platform upgrade is **optional / later**.

## Requirements summary

1. **Proxmox VE** on bare metal.
2. **Intel X550-T2** — **VyOS** on **2× SR-IOV VF** (one per RJ45); **onboard 1 GbE** for **Proxmox management only** (see hardware doc).
3. Software: **VyOS**, **Pi-hole**, **Tailscale** — Pi-hole and Tailscale run as **dedicated LXCs** on internal **`vmbr-svc`** (Tailscale **subnet router**; not on VyOS).  
4. **LAN VLANs:** **private**, **guest**, **iot** (**iot** = no internet); see [network-and-services.md](docs/network-and-services.md).  
5. **Disk roles:** small NVMe = OS, large NVMe = guests + ISOs, HDD = bulk — see [hardware requirements](docs/hardware-requirements.md) § Disk layout.
