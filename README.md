# Homelab server — requirements and design

Documentation for planning a **single homelab host** that runs **Proxmox VE** and guests for **routing (VyOS)**, **DNS filtering (Pi-hole)**, and **remote access (Tailscale)**.

## Baby steps (current rollout)

The **full design** (VLANs, **`vmbr-svc`**, Pi-hole, Tailscale, IoT policy, SR-IOV) stays in the repo as the **north star**. The owner is **starting small**:

1. **VyOS only** for now — no Pi-hole or Tailscale VMs yet.  
2. **No VLAN, no SR-IOV** — Proxmox **Linux bridges** + **virtio** WAN/LAN into VyOS.  
3. **Basic router** — **NAT**, **firewall** basics, **DHCP** on a **single flat LAN**; client DNS = **Cloudflare** (`1.1.1.1` / `1.0.0.1`) **or** **OpenDNS** (`208.67.222.222` / `208.67.220.220`) via DHCP.  
4. **Grow later** into the locked sections of [network-and-services.md](docs/network-and-services.md) and [agent.md](agent.md) without throwing away earlier work.

**Where to read:** [network-and-services.md](docs/network-and-services.md) (**§ Phase 1 — VyOS-only starter**) and [agent.md](agent.md) (**§ Implementation phases**).

## Quick links

- [Hardware requirements](docs/hardware-requirements.md)
- [Network and services](docs/network-and-services.md)
- [Ansible — VyOS on Proxmox (Phase 1)](ansible/README.md)
- [Agent context for assistants](agent.md)

## Status

- **Now:** documentation only; **Phase 1** build-out is **VyOS-only** (see **Baby steps** above).
- **Later:** More Ansible roles as the design grows; **`ansible/`** already covers **Phase 1** VyOS on Proxmox ([ansible/README.md](ansible/README.md)).

**Hardware (current):** existing **Intel tower** (Z270 / i5-7600K); **dual-port add-in NIC** (inventory: **Intel X550-T2**). **Planned upgrade:** **Core Ultra 7 270K Plus** + **ASUS Pro WS W880-ACE SE** — see [hardware requirements](docs/hardware-requirements.md) § **Planned platform**. **Phase 1:** **bridges + virtio** to VyOS for WAN/LAN. **Target:** **SR-IOV VFs** + VLAN trunk (see [network-and-services.md](docs/network-and-services.md)). **Onboard 1GbE** for **Proxmox management only**. **NIC model is not locked** — see [hardware requirements](docs/hardware-requirements.md) § **NIC alternatives**. **Disks:** **Samsung 960 EVO 250 GB** = Proxmox OS · **WD SN770 1 TB** = VMs/LXCs + ISOs · **WD Red 3 TB** = bulk/NAS.

## Requirements summary

1. **Proxmox VE** on bare metal.
2. **Dual-port router NIC** (inventory **X550-T2** today) — **Phase 1:** **bridges + virtio** to VyOS; **target:** **2× SR-IOV VF** (one per RJ45); **onboard 1 GbE** for **Proxmox management only**; **alternatives** in hardware doc.
3. **Software (target):** **VyOS**, **Pi-hole**, **Tailscale** — Pi-hole and Tailscale as **dedicated LXCs** on **`vmbr-svc`** when you reach that phase. **Phase 1:** **VyOS only**.  
4. **LAN (target):** **private / guest / iot** = **VLAN 10 / 20 / 30**, subnets **`10.10.10.0/24`**, **`10.10.20.0/24`**, **`10.10.30.0/24`**; **`vmbr-svc`** = **`10.10.0.0/24`**; trunk **tagged-only** to VyOS — see [network-and-services.md](docs/network-and-services.md). **Phase 1:** one **flat** LAN only.  
5. **Disk roles:** small NVMe = OS, large NVMe = guests + ISOs, HDD = bulk — see [hardware requirements](docs/hardware-requirements.md) § Disk layout.
