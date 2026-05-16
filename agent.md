# Agent context — homelab server documentation

This file gives assistants persistent context for this repository. **Update this file** when goals, stack choices, or doc layout change.

## Project purpose

Document **hardware requirements** and **logical/network design** for a single **homelab server** that acts as the home network’s core: **Proxmox VE**-hosted routing, DNS, filtering, and secure remote access.

## Implementation phases (baby steps)

The repo describes the **full target** stack (segmented VLANs, **`vmbr-svc`**, Pi-hole, Tailscale, IoT policy, SR-IOV, and locked addressing). The owner is **starting small** and growing into that design **without discarding** the documentation.

| Phase | Scope |
|-------|--------|
| **Phase 1 (now)** | **VyOS only** on Proxmox: **plain Linux bridges** + **virtio** (or `e1000`) for **WAN** and **LAN** — **no VLAN**, **no SR-IOV**. **Single flat LAN**, **DHCP** on VyOS, client DNS = **Cloudflare** (`1.1.1.1`, `1.0.0.1`) **or** **OpenDNS** (`208.67.222.222`, `208.67.220.220`). **No Pi-hole**, **no Tailscale**, **no `vmbr-svc`** yet. See **`docs/network-and-services.md` § Phase 1 — VyOS-only starter**. |
| **Later** | Enable **VLANs**, **`vmbr-svc`**, **Pi-hole** (and locked DoT upstreams), **Tailscale** ACLs, **IoT rules**, **SR-IOV** — per the rest of **`docs/network-and-services.md`** and **§ Owner requirements** below. |

## Current hardware inventory (read before recommending services)

**Use this section to size RAM, CPU, storage I/O, and PCIe before suggesting new workloads.** **In service today:** Z270 / i5-7600K below. **Planned upgrade:** **Core Ultra 7 270K Plus** + **ASUS Pro WS W880-ACE SE** — rationale and **PL1/PL2** tuning in **`docs/hardware-requirements.md` § Planned platform — why 270K Plus + W880**.

### In service today

| Category | Detail |
|----------|--------|
| **Chassis role** | Tower desktop → **bare metal for Proxmox VE** (was daily **Arch Linux** install; will be repurposed). |
| **Motherboard** | ASUS **ROG STRIX Z270E GAMING** (Rev 1.xx), **Intel Z270**, UEFI AMI (legacy board — verify **VT-x**, **VT-d/IOMMU**, **Above 4G decoding** for **SR-IOV VF assignment** to VyOS). |
| **CPU** | Intel **Core i5-7600K** (Kaby Lake), **4 cores / 4 threads** (no HT), **AES-NI**, up to ~4.2 GHz turbo. **Tight for many concurrent heavy VMs**; fine for **VyOS + lean LXCs** if RAM is respected. |
| **RAM** | **16 GB total** — **primary constraint** for Proxmox + VyOS + Pi-hole + Docker + Vaultwarden + file share + music; prefer **LXCs**, **small** guest RAM, avoid **ZFS ARC** eating the host without a cap. |
| **GPU** | **Intel HD Graphics 630** (iGPU) — useful for **local display/console** on the hypervisor; no discrete GPU required for headless routing. |
| **Wireless** | Qualcomm **QCA6174** Wi‑Fi (and BT) — **not** part of the homelab WAN/LAN design; expect **wired** operation. |
| **Ethernet (onboard)** | Intel **I219-V** (`e1000e`) — **1 GbE**, reserved for **Proxmox management only** per NIC policy below. |
| **Ethernet (add-in)** | **Intel X550-T2** (`ixgbe`; **`ixgbevf`** when using SR-IOV) — **current** dual‑RJ45 card. **Phase 1:** bridge each port into **`vmbr-wan` / `vmbr-lan`** and give VyOS **virtio** — **no VF** yet. **Target:** **two SR-IOV VFs** (WAN+LAN). **SKU is not locked** — see **`docs/hardware-requirements.md` § NIC alternatives** and **multi‑gig WAN** (~**1.3 Gbps** needs **>1 GbE** to the modem). |
| **Storage — boot / OS** | **Samsung SSD 960 EVO 250 GB** — **Proxmox VE root** only (`/`); host logs, updates, and **thin** local state. **Do not** default **VM/LXC disks** or **bulk ISO library** here (disk is small). |
| **Storage — VMs / LXCs** | **WD Black SN770 1 TB** — **primary Proxmox datastore**: **VM and LXC disks**, **ISO images**, **templates**; fast tier for **Pi-hole** and other service data that benefits from low latency. Add as **Directory**, **LVM-thin**, or **ZFS** (see hardware doc). |
| **Storage — bulk / NAS** | **WD Red WD30EFRX 3 TB** — **media, file shares, backups**, and other **sequential / capacity** workloads; **not** the default for Pi-hole DB or other **random‑I/O‑heavy** databases unless tuned. |
| **Swap** | Large swap existed on Arch layout — **do not** treat swap as RAM for Proxmox planning; still size guests so the host does not thrash. |

**Implications for agents:** do **not** recommend **GPU-heavy** (Frigate CPU-only at scale, Immich ML, gaming VMs) without calling out **RAM/CPU** limits; prefer **one role per heavy service** or defer to a **second host**. **ECC** is **not** available on this platform — factor into **ZFS** advice.

**`/dev/nvme*n*` note:** On Linux, **NVMe enumeration order** (`nvme0` vs `nvme1`) can differ from physical slot — always map by **serial/model** in Proxmox **Disks** view after install.

## Owner requirements (source of truth)

**Long-term target.** The items below describe the **full** homelab the documentation is steering toward. **Phase 1** deliberately implements **only a subset** (VyOS-only, flat LAN, bridges + virtio, public DNS via DHCP — see **§ Implementation phases**). Ignore or defer bullets that do not apply until you move to the next phase.

1. **Hypervisor as base** — **Proxmox VE** runs directly on bare metal; workloads are **VMs and LXCs** (containers).
2. **Disk layout (locked)** — **Samsung 960 EVO 250 GB** = **Proxmox OS only**. **WD Black SN770 1 TB** = **all VM and LXC disks** plus **ISO/template** storage (default fast datastore). **WD Red 3 TB** = **bulk data** (NAS shares, media, backups). See **[`docs/hardware-requirements.md`](docs/hardware-requirements.md)** for Proxmox storage wiring detail.
3. **Hardware host** — Same tower as **[Current hardware inventory](#current-hardware-inventory-read-before-recommending-services)** today. **Planned upgrade:** **Intel Core Ultra 7 270K Plus** on **ASUS Pro WS W880-ACE SE** (see **`docs/hardware-requirements.md` § Planned platform**). **BIOS power caps:** **PL1 = PL2 = 65 W** optional profile for 24/7 efficiency when not saturating CPU.
4. **NIC policy — Phase 1 vs target, PCIe SKU flexible**  
   - **Phase 1:** **VyOS WAN/LAN** via **Proxmox `vmbr`** bridges with **physical NIC ports** as slaves and **virtio** into the VyOS VM — **no SR-IOV**, **no PF/VF split** rules yet. **Document** bridge names and which RJ45 is WAN vs LAN.  
   - **Target architecture (locked when you adopt it):** **VyOS** uses **exactly two SR-IOV VFs** — **one VF per physical port** on a **dual-port NIC** (**WAN** + **LAN**). Map **modem → WAN port’s VF**, **managed switch trunk → LAN port’s VF**; document **RJ45 ↔ role** when cabled. **Do not** use either **PF** as a **Proxmox `vmbr` uplink** on the **same RJ45** **while** VyOS holds the **production VF** for that port — the **VF** owns that cable; the **PF** stays for **SR-IOV parenting** only. See **`docs/network-and-services.md` § SR-IOV nuance — LAN port**.  
   - **Other Proxmox VMs** may each use **one or more additional VFs** from the **same card’s PFs** (guest driver matches the NIC: e.g. **`ixgbevf`** for **`ixgbe`**, **`i40evf`** / **iAVF** for **`i40e`**). Size host **`max_vfs`** so each **PF** covers **VyOS (1 VF)** plus extras; document **PCI BDF → guest**. **LXCs** stay on **bridges** unless you redesign.  
   - **Onboard Intel I219-V (1 GbE)** — **Proxmox host management only** (Web UI, SSH, updates). **Do not** rely on it for VyOS WAN/LAN or as the primary path for lab traffic.  
   - **Target:** **LXCs / most VMs** use a **Linux bridge (`vmbr`)** with a **virtio** leg into **VyOS**; **`vmbr-svc`** + **VyOS virtio** for internal services; **wired** clients use **private / guest / iot** on the **LAN trunk VF**. **Exception:** specific **VMs** may take an **extra VF** on the **LAN PF** for **direct L2** on that segment — then they are **not** hairpinned through VyOS for **L2** neighbor traffic; still document **L3 default route** inside those guests so **internet** follows your policy.  
   - **PCIe NIC model (not locked):** **Current inventory:** Intel **X550-T2** — **NBASE-T** (**1 / 2.5 / 5 / 10 Gb/s** per port), mature **`ixgbe`** + **SR-IOV** on Proxmox. **Alternatives** and **~1.3 Gbps+ ISP** guidance: **`docs/hardware-requirements.md` § NIC alternatives**.
5. **LAN segmentation (target — not Phase 1)** — Exactly **three VLANs** on the **LAN** side (802.1Q on a **managed switch** trunk to the router):  
   - **Private** — trusted devices; **full internet** (subject to normal firewall rules).  
   - **Guest** — untrusted visitors; **internet allowed**; **tighter** isolation from **private** (policy detail in VyOS).  
   - **IoT** — **no internet**; **VyOS forward** policy **locked** in **`docs/network-and-services.md` § IoT firewall exceptions** (**allow** **Pi-hole `10.10.0.53:53`**, **NTP** to **VyOS `10.10.30.1:123`**; **deny** cross-VLAN **mDNS**, **private/guest** by default, **Home Assistant** until you add an **explicit** rule).  
   *Numeric **VLAN IDs**, **IPv4 subnets**, and **`vmbr-svc` addresses** are **locked** in **`docs/network-and-services.md`** (§ VLANs and IPv4 layout, § Internal stub).*  
6. **Network stack (software — target; Phase 1 = VyOS only)**  
   - **VyOS** — **single VM** (choice **A**, locked for target): **default gateway**. **Phase 1:** **2× virtio** (WAN + flat LAN) on **`vmbr`** bridges. **Target:** **2× SR-IOV VF** (WAN + LAN **trunk**) + **1× virtio** on **`vmbr-svc`**. **Not** split across multiple VyOS VMs for WAN/LAN roles.  
   - **Pi-hole** — *(Phase 2+)* LAN DNS, caching, ad/tracker blocking; **upstream DNS locked** (**DoT** to **Quad9** + **Cloudflare** — see **`docs/network-and-services.md` § Pi-hole upstream DNS**); **dedicated LXC** on **`vmbr-svc`**.  
   - **Tailscale** — *(Phase 2+)* mesh VPN / secure remote access; **dedicated LXC** on **`vmbr-svc`** as **subnet router** (**not** on VyOS); **tags, approved subnet routes, and ACL intent** are **locked** in **`docs/network-and-services.md` § Tailscale ACLs and subnet routes**.

## Planned automation

- **Ansible** lives under **`ansible/`** — see **`ansible/README.md`** to **provision** a **VyOS** VM on **Proxmox** and apply **Phase 1** config (bridges + virtio, DHCP, Cloudflare/OpenDNS) via the **`vyos_proxmox`** role and **`playbooks/site.yml`**. Further playbooks may be added later.

## Repository layout

| Path | Role |
|------|------|
| `agent.md` | Agent and human context; **includes current hardware inventory** — read before sizing new services. |
| `README.md` | Short overview and how to read the docs. |
| `docs/hardware-requirements.md` | CPU, RAM, storage, NIC, power, physical constraints. |
| `docs/network-and-services.md` | **Phase 1** starter + **target** topology (VLANs, Pi-hole, Tailscale, SR-IOV). |
| `ansible/` | **Ansible** — Proxmox VyOS VM + **Phase 1** VyOS config (see **`ansible/README.md`**). |

## Resolved open decisions

| Date | Topic | Outcome |
|------|--------|---------|
| **2026-05-11** | **VyOS topology** | **Single VyOS VM** (option **A**): one guest holds **WAN VF**, **LAN trunk VF** (private / guest / iot), and **`vmbr-svc` virtio**; no separate WAN-only / LAN-only VyOS pair. |
| **2026-05-11** | **VLAN IDs, IPv4 layout, `vmbr-svc` stub** | **Locked** in **`docs/network-and-services.md`**: VLANs **10 / 20 / 30**, subnets **`10.10.10.0/24`**, **`10.10.20.0/24`**, **`10.10.30.0/24`**, internal **`10.10.0.0/24`** (VyOS **`10.10.0.1`**, Pi-hole **`10.10.0.53`**, Tailscale example **`10.10.0.52`**), **tagged-only** LAN trunk to the switch. |
| **2026-05-11** | **Pi-hole upstream DNS** | **DNS-over-TLS**: **Quad9** primary (`9.9.9.9`, `149.112.112.112`, SNI `dns.quad9.net`); **Cloudflare** secondary (`1.1.1.1`, `1.0.0.1`, SNI `one.one.one.one`); **not** ISP DNS by default. See **`docs/network-and-services.md` § Pi-hole upstream DNS**. |
| **2026-05-11** | **IoT firewall exceptions** | **Locked** in **`docs/network-and-services.md` § IoT firewall exceptions**: **DROP** IoT→WAN; **ALLOW** IoT→**`10.10.0.53:53`** (Pi-hole); **ALLOW** IoT→**`10.10.30.1:123`** (NTP on VyOS); **DROP** IoT→private/guest by default, **DROP** cross-VLAN **mDNS**; **no** default **Home Assistant** — add **explicit** per-hub rules when needed. |
| **2026-05-11** | **Tailscale ACLs and subnet routes** | **Locked** in **`docs/network-and-services.md` § Tailscale ACLs and subnet routes**: machine tag **`tag:homelab-sr`** on subnet-router LXC; **approve** **`10.10.0.0/24`** + **`10.10.10.0/24`** only by default (**not** guest/IoT); **owner/admin–scoped** ACL intent; **VyOS** egress + **`svc` → private** relay notes. |
| **2026-05-12** | **Phased rollout (“baby steps”)** | **Phase 1** = **VyOS-only**, **flat LAN**, **Proxmox bridges + virtio** (no VLAN, no SR-IOV), **DHCP**, client DNS **Cloudflare or OpenDNS**; rest of repo = **target** design preserved. See **`agent.md` § Implementation phases** and **`docs/network-and-services.md` § Phase 1**. |

## Open decisions (fill in as you choose)

*None right now. Add rows here when hardware, ISP, trust boundaries, or automation scope changes.*

## Changelog

| Date | Change |
|------|--------|
| 2026-05-10 | Initial project: `agent.md`, `README.md`, `docs/hardware-requirements.md`, `docs/network-and-services.md`. Requirements: Proxmox VE, multi-speed NIC (1G/2.5G/10G), VyOS, Unbound, AdGuard Home, Tailscale. |
| 2026-05-10 | DNS stack simplified: **Pi-hole** only (removed Unbound + AdGuard Home). |
| 2026-05-10 | Hypervisor locked to **Proxmox VE** (removed XCP-ng/ESXi alternatives from open decisions). |
| 2026-05-10 | **NIC + host:** VyOS WAN/LAN on **dual-port add-in** via **SR-IOV** (inventory **X550-T2** at the time); onboard **I219-V** for **Proxmox management only**; hardware is the **current Z270 / i5-7600K tower** until a deliberate upgrade. |
| 2026-05-10 | Added **`agent.md` § Current hardware inventory** (full machine table for agent sizing); linked from hardware doc. |
| 2026-05-10 | **VyOS ↔ router NIC:** **two VFs** (one per **PF** / RJ45 port) — not whole-card passthrough or virtio for WAN/LAN. |
| 2026-05-10 | **Additional VFs** on the same card may be assigned **one (or more) per other Proxmox VM** where line-rate or CPU offload is needed; **`max_vfs`** and BDF mapping must cover VyOS + those VMs. |
| 2026-05-10 | **LAN VLANs required:** **private**, **guest**, **iot** (**no internet**); topology + VF/bridge recommendation in `docs/network-and-services.md`. |
| 2026-05-10 | Documented **PF vs VF on LAN port:** **no PF1 in `vmbr`** as trunk uplink alongside VyOS **VF**; PF is **SR-IOV parent** only for this design. |
| 2026-05-10 | **Dedicated LXC on `vmbr-svc`** (Pi-hole + Tailscale); populated **Proxmox mapping** RAM/vCPU; **Tailscale** locked to **subnet-router LXC**; IoT→Pi-hole wording, **`max_vfs` PF0 vs PF1** note, SR-IOV Pi-hole sentence, **reference topology** redraw. |
| 2026-05-10 | **Disk layout locked:** 960 EVO = Proxmox OS; SN770 = VM/LXC + ISOs; WD Red = bulk/NAS; inventory + `hardware-requirements.md` updated. |
| 2026-05-11 | **VyOS topology locked (A):** single gateway VM; **Resolved open decisions** table. |
| 2026-05-11 | **Agent synced:** VLAN/IPv4/`vmbr-svc` treated as **locked** per `network-and-services.md` (removed duplicate open item). |
| 2026-05-11 | **Pi-hole upstream DNS locked:** DoT to **Quad9** + **Cloudflare**; section in `network-and-services.md`. |
| 2026-05-11 | **IoT firewall exceptions locked:** new § in `network-and-services.md`; **Resolved** + **Open decisions** in `agent.md` updated. |
| 2026-05-11 | **Tailscale ACLs + subnet routes locked:** new § in `network-and-services.md`; **Resolved** table + **Open decisions** cleared in `agent.md`. |
| 2026-05-11 | **NIC SKU clarified:** **X550-T2** is **current inventory**, not a hard lock; **`docs/hardware-requirements.md` § NIC alternatives** + WAN speed note (~**1.3 Gbps**); **`agent.md`** / **`network-and-services.md`** / **README** generalized where needed. |
| 2026-05-11 | **Modem dual LAN:** doc note — **VyOS WAN** on **2.5G** modem port when **1G + 2.5G** both available (`hardware-requirements.md` § WAN speed). |
| 2026-05-12 | **Baby steps rollout:** **Phase 1** VyOS-only / flat LAN / bridges+virtio / Cloudflare or OpenDNS DHCP; **README**, **`agent.md`**, **`network-and-services.md`**, **`hardware-requirements.md`** updated; **target** design retained. |
| 2026-05-12 | **Ansible:** `ansible/` — **`proxmox_vyos_vm`** + **`vyos_phase1`** roles and **`playbooks/site.yml`** for Proxmox VM + Phase 1 VyOS config. |
| 2026-05-13 | **Ansible:** merged into single **`vyos_proxmox`** role + **`playbooks/site.yml`** only (tags for provision/configure); **`module_defaults`** for Proxmox API on the provision play. |
| 2026-05-16 | **Planned platform:** **Core Ultra 7 270K Plus** + **ASUS Pro WS W880-ACE SE** — decision rationale, power comparison vs **Ryzen 9 7900**, verified **PL1/PL2** BIOS caps in **`docs/hardware-requirements.md`**. |
