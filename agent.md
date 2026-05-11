# Agent context — homelab server documentation

This file gives assistants persistent context for this repository. **Update this file** when goals, stack choices, or doc layout change.

## Project purpose

Document **hardware requirements** and **logical/network design** for a single **homelab server** that acts as the home network’s core: **Proxmox VE**-hosted routing, DNS, filtering, and secure remote access.

## Current hardware inventory (read before recommending services)

**Use this section to size RAM, CPU, storage I/O, and PCIe before suggesting new workloads.** Upgrade path is deferred; assume this machine unless the owner says otherwise.

| Category | Detail |
|----------|--------|
| **Chassis role** | Tower desktop → **bare metal for Proxmox VE** (was daily **Arch Linux** install; will be repurposed). |
| **Motherboard** | ASUS **ROG STRIX Z270E GAMING** (Rev 1.xx), **Intel Z270**, UEFI AMI (legacy board — verify **VT-x**, **VT-d/IOMMU**, **Above 4G decoding** for **SR-IOV VF assignment** to VyOS). |
| **CPU** | Intel **Core i5-7600K** (Kaby Lake), **4 cores / 4 threads** (no HT), **AES-NI**, up to ~4.2 GHz turbo. **Tight for many concurrent heavy VMs**; fine for **VyOS + lean LXCs** if RAM is respected. |
| **RAM** | **16 GB total** — **primary constraint** for Proxmox + VyOS + Pi-hole + Docker + Vaultwarden + file share + music; prefer **LXCs**, **small** guest RAM, avoid **ZFS ARC** eating the host without a cap. |
| **GPU** | **Intel HD Graphics 630** (iGPU) — useful for **local display/console** on the hypervisor; no discrete GPU required for headless routing. |
| **Wireless** | Qualcomm **QCA6174** Wi‑Fi (and BT) — **not** part of the homelab WAN/LAN design; expect **wired** operation. |
| **Ethernet (onboard)** | Intel **I219-V** (`e1000e`) — **1 GbE**, reserved for **Proxmox management only** per NIC policy below. |
| **Ethernet (add-in)** | Intel **X550-T2** (`ixgbe` on host, **`ixgbevf`** in guests) — **dual RJ45**; **VyOS:** **two VFs** (WAN+LAN); **other VMs:** optional **additional VF(s)** per VM; size **`max_vfs`**. |
| **Storage — boot / OS** | **Samsung SSD 960 EVO 250 GB** — **Proxmox VE root** only (`/`); host logs, updates, and **thin** local state. **Do not** default **VM/LXC disks** or **bulk ISO library** here (disk is small). |
| **Storage — VMs / LXCs** | **WD Black SN770 1 TB** — **primary Proxmox datastore**: **VM and LXC disks**, **ISO images**, **templates**; fast tier for **Pi-hole** and other service data that benefits from low latency. Add as **Directory**, **LVM-thin**, or **ZFS** (see hardware doc). |
| **Storage — bulk / NAS** | **WD Red WD30EFRX 3 TB** — **media, file shares, backups**, and other **sequential / capacity** workloads; **not** the default for Pi-hole DB or other **random‑I/O‑heavy** databases unless tuned. |
| **Swap** | Large swap existed on Arch layout — **do not** treat swap as RAM for Proxmox planning; still size guests so the host does not thrash. |

**Implications for agents:** do **not** recommend **GPU-heavy** (Frigate CPU-only at scale, Immich ML, gaming VMs) without calling out **RAM/CPU** limits; prefer **one role per heavy service** or defer to a **second host**. **ECC** is **not** available on this platform — factor into **ZFS** advice.

**`/dev/nvme*n*` note:** On Linux, **NVMe enumeration order** (`nvme0` vs `nvme1`) can differ from physical slot — always map by **serial/model** in Proxmox **Disks** view after install.

## Owner requirements (source of truth)

1. **Hypervisor as base** — **Proxmox VE** runs directly on bare metal; workloads are **VMs and LXCs** (containers).
2. **Disk layout (locked)** — **Samsung 960 EVO 250 GB** = **Proxmox OS only**. **WD Black SN770 1 TB** = **all VM and LXC disks** plus **ISO/template** storage (default fast datastore). **WD Red 3 TB** = **bulk data** (NAS shares, media, backups). See **[`docs/hardware-requirements.md`](docs/hardware-requirements.md)** for Proxmox storage wiring detail.
3. **Hardware host (current)** — Same tower as **[Current hardware inventory](#current-hardware-inventory-read-before-recommending-services)**. **Platform upgrade** (CPU/RAM/motherboard) is **deferred**; revisit when budget or headroom requires it.
4. **NIC policy (locked)**  
   - **Intel X550-T2** — Designated **multi-gig / 10G-T** NIC. **VyOS** receives **exactly two SR-IOV virtual functions (VFs)**: **one VF from each physical port (PF0 and PF1)** — map to **WAN** and **LAN** (e.g. modem on RJ45 **PF0** / VF from that port → **WAN**, LAN on **PF1** / VF from that port → **LAN**; document chosen mapping when cabled). **1 / 2.5 / 5 / 10 Gb/s** per link.  
   - **Other Proxmox VMs** may each use **one or more additional VFs** from the X550 (same **`ixgbevf`** guest model) — e.g. **line-rate storage or backup** on a **LAN PF** VF. Size **`ixgbe` `max_vfs`** so each **PF** exposes enough VFs for **VyOS (1 per PF)** plus any **extra VM** assignments; document **which VF BDF** goes to which guest. **LXCs** generally stay on **bridges** (no PCI VF); VF passthrough is **VM-oriented**.  
   - **Proxmox host** keeps the **physical functions (PFs)** on the X550 so **`ixgbe`** can **parent** SR-IOV VFs. **Do not** use **PF1** (or **PF0**) as a **`vmbr` physical uplink** **while** VyOS uses a **VF from that PF** for **WAN/LAN** — the **VF inside VyOS** is the **production** endpoint on that **RJ45**; bridging the **PF** in parallel is **not clean** and overlaps ownership. See **`docs/network-and-services.md` § SR-IOV nuance — LAN port**.  
   - **Onboard Intel I219-V (1 GbE)** — **Proxmox host management only** (Web UI, SSH, updates). **Do not** rely on it for VyOS WAN/LAN or as the primary path for lab traffic. **LXCs / most VMs** use a **Linux bridge (`vmbr`)** with a **virtio** leg into **VyOS**; **VyOS** routes/NATs to **WAN** and applies **VLAN firewall** policy (see **VLANs** + **recommended topology** in `docs/network-and-services.md`).  
   - **Proxmox and most guests** do not need a second physical NIC: the hypervisor is reached via **onboard**; typical **VMs/LXCs** use **`vmbr-svc`** + **VyOS virtio** (routed); **wired** clients use **private / guest / iot** on the **LAN trunk VF**. **Exception:** specific **VMs** may take an **extra X550 VF** for **direct L2** on the LAN (or rarely WAN) segment — then they are **not** hairpinned through VyOS for **L2** neighbor traffic; still document **L3 default route** inside those guests so **internet** follows your policy.
5. **LAN segmentation (required)** — Exactly **three VLANs** on the **LAN** side (802.1Q on a **managed switch** trunk to the router):  
   - **Private** — trusted devices; **full internet** (subject to normal firewall rules).  
   - **Guest** — untrusted visitors; **internet allowed**; **tighter** isolation from **private** (policy detail in VyOS).  
   - **IoT** — **no internet** (default: **drop forward** from IoT to **WAN**); may allow **limited** access to **private** (e.g. DNS to **Pi-hole** only) — document exceptions when implemented.  
   *Numeric **VLAN IDs**, **IPv4 subnets**, and **`vmbr-svc` addresses** are **locked** in **`docs/network-and-services.md`** (§ VLANs and IPv4 layout, § Internal stub).*  
6. **Network stack (software)**  
   - **VyOS** — **single VM** (choice **A**, locked): **default gateway** with **2× SR-IOV VF** (WAN + LAN trunk) + **1× virtio** on **`vmbr-svc`**. **Not** split across multiple VyOS VMs for WAN/LAN roles.  
   - **Pi-hole** — LAN DNS, caching, ad/tracker blocking; **upstream DNS locked** (**DoT** to **Quad9** + **Cloudflare** — see **`docs/network-and-services.md` § Pi-hole upstream DNS**); **dedicated LXC** on **`vmbr-svc`**.  
   - **Tailscale** — mesh VPN / secure remote access; **dedicated LXC** on **`vmbr-svc`** as **subnet router** (**not** on VyOS — see `docs/network-and-services.md`).

## Planned automation (not in repo yet)

- **Ansible** playbooks/roles will be added later to configure guests and services. Until then, this repo is **documentation only** (no playbooks, scripts, or IaC).

## Repository layout

| Path | Role |
|------|------|
| `agent.md` | Agent and human context; **includes current hardware inventory** — read before sizing new services. |
| `README.md` | Short overview and how to read the docs. |
| `docs/hardware-requirements.md` | CPU, RAM, storage, NIC, power, physical constraints. |
| `docs/network-and-services.md` | Topology, VLANs, which VM runs what, DNS flow, Tailscale placement. |

## Resolved open decisions

| Date | Topic | Outcome |
|------|--------|---------|
| **2026-05-11** | **VyOS topology** | **Single VyOS VM** (option **A**): one guest holds **WAN VF**, **LAN trunk VF** (private / guest / iot), and **`vmbr-svc` virtio**; no separate WAN-only / LAN-only VyOS pair. |
| **2026-05-11** | **VLAN IDs, IPv4 layout, `vmbr-svc` stub** | **Locked** in **`docs/network-and-services.md`**: VLANs **10 / 20 / 30**, subnets **`10.10.10.0/24`**, **`10.10.20.0/24`**, **`10.10.30.0/24`**, internal **`10.10.0.0/24`** (VyOS **`10.10.0.1`**, Pi-hole **`10.10.0.53`**, Tailscale example **`10.10.0.52`**), **tagged-only** LAN trunk to the switch. |
| **2026-05-11** | **Pi-hole upstream DNS** | **DNS-over-TLS**: **Quad9** primary (`9.9.9.9`, `149.112.112.112`, SNI `dns.quad9.net`); **Cloudflare** secondary (`1.1.1.1`, `1.0.0.1`, SNI `one.one.one.one`); **not** ISP DNS by default. See **`docs/network-and-services.md` § Pi-hole upstream DNS**. |

## Open decisions (fill in as you choose)

- **IoT exceptions:** whether IoT may reach **Pi-hole** (or NTP only) on **private**; **mDNS** or **HA hub** exceptions.  
- **Tailscale ACLs:** tags, **subnet routes** advertised by the **`tailscale` LXC**, and admin-console **ACL** intent (high level until Ansible encodes it). **Placement is locked** — dedicated LXC on **`vmbr-svc`**.

## Changelog

| Date | Change |
|------|--------|
| 2026-05-10 | Initial project: `agent.md`, `README.md`, `docs/hardware-requirements.md`, `docs/network-and-services.md`. Requirements: Proxmox VE, multi-speed NIC (1G/2.5G/10G), VyOS, Unbound, AdGuard Home, Tailscale. |
| 2026-05-10 | DNS stack simplified: **Pi-hole** only (removed Unbound + AdGuard Home). |
| 2026-05-10 | Hypervisor locked to **Proxmox VE** (removed XCP-ng/ESXi alternatives from open decisions). |
| 2026-05-10 | **NIC + host locked:** Intel **X550-T2** for VyOS WAN/LAN; onboard **I219-V** for **Proxmox management only**; hardware is the **current Z270 / i5-7600K tower** until a deliberate upgrade. |
| 2026-05-10 | Added **`agent.md` § Current hardware inventory** (full machine table for agent sizing); linked from hardware doc. |
| 2026-05-10 | **VyOS ↔ X550-T2 locked to SR-IOV:** **two VFs** (one per **PF** / RJ45 port) — not whole-card passthrough or virtio for WAN/LAN. |
| 2026-05-10 | **Additional X550 VFs** may be assigned **one (or more) per other Proxmox VM** where line-rate or CPU offload is needed; **`max_vfs`** and BDF mapping must cover VyOS + those VMs. |
| 2026-05-10 | **LAN VLANs required:** **private**, **guest**, **iot** (**no internet**); topology + VF/bridge recommendation in `docs/network-and-services.md`. |
| 2026-05-10 | Documented **PF vs VF on LAN port:** **no PF1 in `vmbr`** as trunk uplink alongside VyOS **VF**; PF is **SR-IOV parent** only for this design. |
| 2026-05-10 | **Dedicated LXC on `vmbr-svc`** (Pi-hole + Tailscale); populated **Proxmox mapping** RAM/vCPU; **Tailscale** locked to **subnet-router LXC**; IoT→Pi-hole wording, **`max_vfs` PF0 vs PF1** note, SR-IOV Pi-hole sentence, **reference topology** redraw. |
| 2026-05-10 | **Disk layout locked:** 960 EVO = Proxmox OS; SN770 = VM/LXC + ISOs; WD Red = bulk/NAS; inventory + `hardware-requirements.md` updated. |
| 2026-05-11 | **VyOS topology locked (A):** single gateway VM; **Resolved open decisions** table. |
| 2026-05-11 | **Agent synced:** VLAN/IPv4/`vmbr-svc` treated as **locked** per `network-and-services.md` (removed duplicate open item). |
| 2026-05-11 | **Pi-hole upstream DNS locked:** DoT to **Quad9** + **Cloudflare**; section in `network-and-services.md`. |
