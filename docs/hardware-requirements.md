# Hardware requirements — homelab server

This document captures **minimum and recommended** hardware for the stated software stack. Adjust for your budget, power envelope, and noise tolerance.

## Role of the machine

- Runs **Proxmox VE** on bare metal with multiple guests (VMs/LXCs).
- Hosts **VyOS** (routing/firewall), **Pi-hole** (DNS + blocking), and **Tailscale** (placement detailed in [network-and-services.md](network-and-services.md)).

## Chosen platform (current — upgrade deferred)

**Canonical inventory (models, disks, NICs, constraints)** lives in **[`agent.md`](../agent.md#current-hardware-inventory-read-before-recommending-services)** — agents should read that first when recommending services.

| Item | Planned hardware (summary) |
|------|-------------------|
| **Board** | ASUS **ROG STRIX Z270E GAMING** |
| **CPU** | Intel **Core i5-7600K** (4c/4t) — adequate for a lean stack; **more RAM** helps before chasing CPU. |
| **RAM** | **16 GB** installed today — **below** the 32 GB minimum in the table below for a comfortable multi-VM Proxmox host; **upgrade when practical**, not blocked by this doc. |
| **Storage** | **960 EVO 250 GB** = Proxmox OS · **SN770 1 TB** = VM/LXC + ISOs · **WD Red 3 TB** = bulk — see **§ Disk layout (locked)** below. |
| **10G / multi-gig NIC** | **Intel X550-T2** (**current** inventory — **not** a required SKU) — VyOS **WAN + LAN** via **SR-IOV**; see [network-and-services.md](network-and-services.md) and **§ NIC alternatives** below. |
| **Management NIC** | Onboard **Intel I219-V** — **Proxmox host only**; not the VyOS data path. |

**Platform refresh** (new CPU/RAM/board) remains an **owner decision later**; until then, docs assume this tower.

## CPU

| Concern | Guidance |
|--------|----------|
| **Cores / threads** | VyOS and Pi-hole are light; headroom matters for encryption (VPN, TLS), future VMs, and updates. **4c/8t minimum**; **6–8+ cores** comfortable for growth. |
| **Features** | **AES-NI** (or equivalent) helps VPN/crypto. **VT-x / AMD-V** and **IOMMU** (Intel VT-d / AMD-Vi) if you pass through NICs or plan PCIe passthrough later. |
| **Efficiency** | For 24/7 use, low idle power (modern efficient cores) reduces cost and heat. |

## RAM

| Target | Guidance |
|--------|----------|
| **Minimum** | **32 GB** — Proxmox + VyOS + a few small guests + ZFS arc headroom gets tight fast. |
| **Recommended** | **64 GB** — comfortable for ZFS (if used), extra VMs, and DNS/logging without constant tuning. |

Allocate per guest in the network doc; revisit after Proxmox install.

## Disk layout (locked)

| Disk | Model (inventory) | Role on Proxmox |
|------|-------------------|-----------------|
| **Smaller NVMe** | **Samsung SSD 960 EVO 250 GB** | **Proxmox VE installation target** — root filesystem (`/`), host packages, logs. **Keep small:** avoid making this the default **VM disk** or **ISO library** store (capacity). |
| **Larger NVMe** | **WD Black SN770 1 TB** | **Primary datastore** for **all VM and LXC disks**, **ISO images**, and **templates**. Create a Proxmox **Storage** entry (e.g. **Directory** on a dedicated filesystem, **LVM-thin**, or **ZFS single-disk pool**) and set it as **default** for **Disk image** / **Container** where appropriate. **Pi-hole** and other latency-sensitive service disks live here. |
| **HDD** | **WD Red WD30EFRX 3 TB** | **Bulk capacity** — **NAS / Samba exports**, **media**, **backup targets**, large sequential files. **Not** the default for **Pi-hole query DB** or other **random‑I/O‑heavy** workloads unless you accept slower UI and tune retention. |

**Proxmox UI checklist:** **Datacenter → Storage** — ensure **Disk image** and **Container** (and **ISO image**) point at the **SN770-backed** storage by default; **Directory** for **vzdump** backups can target **HDD** (or SN770 if you prefer speed over capacity).

**Implementation choice (open until install):** whether the **SN770** pool is **ext4 + directory**, **LVM-thin**, or **ZFS** — Ansible will need the **storage ID** name you configure (e.g. `local-lvm`, `vmdata`, `zfs-vm`).

## Storage (general guidance)

| Concern | Guidance |
|--------|----------|
| **Boot / OS** | This project: **250 GB Samsung** dedicated to Proxmox; prosumer NVMe with decent endurance. |
| **Guest disks** | **SN770 1 TB** — keep guests off the **OS disk** by default; avoids filling `/` and keeps DNS/VM I/O on the faster tier. |
| **Logs / Pi-hole DB** | **SN770** preferred (random I/O); **not** the **WD Red** by default. |

**ZFS on Proxmox:** if used on the **SN770**, popular but this platform has **no ECC** — cap **ARC** on **16 GB RAM** hosts; match risk to how much you value that pool.

## Networking — dual-port router NIC + onboard management

**Architecture (locked):** a **dual-port** PCIe NIC supplies **WAN** and **LAN** to **VyOS** as **two SR-IOV VFs** (**one VF per physical port / PF**). **Onboard Intel I219-V** (**1 GbE**) is **Proxmox management only** — not VyOS WAN/LAN or the primary lab data path.

**Current inventory (not locked):** **Intel X550-T2** — two **RJ45** ports, **NBASE-T** (**1 / 2.5 / 5 / 10 Gb/s**), Linux **`ixgbe`** + guest **`ixgbevf`**. Strong fit when the **modem LAN** is **multi-gig** (e.g. **2.5G**) or **10G** and you want headroom.

### WAN speed (~1.3 Gbps and similar)

A **true 1 GbE** link to the modem **tops out near ~940–950 Mb/s** TCP after overhead — you **leave bandwidth unused** on a **~1.3 Gbps** plan. Prefer a NIC + cable + modem LAN port that negotiate **at least 2.5 Gb/s** (or **5G / 10G**) on the **WAN** side. **X550-T2** often syncs at **2.5G** or **5G** to many **cable / fiber ONT** multi-gig ports; confirm **modem port speed** and **Cat5e/Cat6** quality.

### NIC alternatives (SKU flexible — verify before buy)

| Option | Typical driver | Notes |
|--------|----------------|--------|
| **Intel X550-T2** (or **X550-AT2**) | **`ixgbe`** / **`ixgbevf`** | **Default recommendation** in this repo: proven **SR-IOV** on Proxmox, **NBASE-T**, **RJ45** matches most home modems. |
| **Intel X710-T2L** (10GBASE-T) | **`i40e`** / **`i40evf`** or **iAVF** | Newer **10G-T**; check **VyOS** + **Proxmox** support for your image/kernel. Often pricier; good **long-life** choice. |
| **Marvell / Aquantia 10G-T** (e.g. **AQC107**, **AQC113** add-in cards) | **`atlantic`** | **Price/performance**; **SR-IOV** and **Linux** behavior vary by **exact SKU** — **verify** on your target **kernel** and that **VyOS** supports the **VF** path you want. |
| **Mellanox ConnectX-4 Lx / ConnectX-5** (often **SFP+**) | **`mlx5_core`** | Excellent **SR-IOV**; **RJ45** to modem may need the **right transceiver** or **DAC** — only a fit if **modem** exposes **SFP+** or you use a **media converter** you trust. |
| **Dual 2.5G** (Intel **i225** / **i226** class) | **`igc`** | **Enough raw speed** for **~1.3 Gbps**; **SR-IOV** is **not** the usual reason people buy these — **confirm** for your **exact** chip and **Proxmox** kernel before planning **VyOS-on-VF** the same way as **`ixgbe`**. If **SR-IOV** is unavailable, you’d fall back to **virtio** or **whole-NIC passthrough** — a **different** topology than this doc’s default. |

**Shopping rule:** for this design, prioritize **dual-port**, **Linux SR-IOV** you can assign **two VFs to VyOS**, **VyOS driver support**, and **WAN link speed** that matches the **modem** (not only **LAN** speed to the switch).

### Practical notes (current X550 path)

- **`ixgbe`** on Proxmox: verify negotiated speed per port; use **Cat6/Cat6a** for **long** **10G-T** runs.
- **SR-IOV:** host keeps **PFs**; **VyOS** (and optional other VMs) get **VFs** via Proxmox **`hostdev`**. Do **not** mirror **PF IPs** onto the same **L2** as **VyOS WAN/LAN VFs** without strict **VLAN** discipline (see [network-and-services.md](network-and-services.md)).

### Suggested documentation fields (fill in when cabled)

- **WAN port:** physical port, negotiated speed, cable to modem (**match modem’s multi-gig port**).
- **LAN port:** speed, cable to **managed switch** trunk.
- **VF map:** **BDF → guest** for **VyOS** (2) and **every other VM** using a VF; **`max_vfs`** (or equivalent) on the host.
- **Onboard `enp0s31f6` (or equivalent):** static management IP on an **isolated or trusted** management VLAN/L2, if used.

## Power, cooling, chassis

- **PSU:** efficient rating (Gold+ typical); sizing for disk spin-up and NIC peaks.
- **Cooling:** 24/7 quiet fans if the machine lives near living space.
- **Rack vs tower:** affects NIC layout and airflow.

## Out of scope for this doc (but link decisions later)

- UPS model and shutdown integration with Proxmox.
- Physical rack, patch panel, and cable plant.

## Checklist (copy when shopping / commissioning)

- [ ] **Dual-port router NIC** installed (**X550-T2** or equivalent from **§ NIC alternatives**); **VT-d / IOMMU** enabled in UEFI; **`max_vfs`** covers **VyOS + other VM** VF assignments
- [ ] **Onboard NIC** reserved for **Proxmox management**; management IP and firewall rules documented
- [ ] CPU with **virtualization** + **AES-NI** + **IOMMU** (for **VF** assignment to VyOS)
- [ ] **32 GB RAM** when budget allows (current **16 GB** is tight — see table above)
- [ ] **Disk layout:** 960 EVO = OS; SN770 = VM/LXC + ISO **storage ID** created and set default; HDD = bulk **mount** (e.g. `/mnt/data`) + backup/NAS role
- [ ] ECC if using ZFS for important data
- [ ] Power / cooling / noise acceptable for install location
