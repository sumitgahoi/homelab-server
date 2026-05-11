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
| **10G / multi-gig NIC** | **Intel X550-T2** (locked choice) — VyOS **WAN + LAN**; see [network-and-services.md](network-and-services.md). |
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

## Networking — Intel X550-T2 + onboard management

**Design choice (locked):** **Intel X550-T2** provides **two RJ45 ports** (two **PFs**) with **1 / 2.5 / 5 / 10 Gb/s** (10GBASE-T / NBASE-T). **VyOS** uses **two SR-IOV VFs** — **one VF per PF** — for **WAN and LAN** (guest **`ixgbevf`**). **Other VMs** may use **additional VFs** (see [network-and-services.md](network-and-services.md)); size **`max_vfs`** accordingly. **Onboard Intel I219-V** is **1 GbE for Proxmox management only** — not used as VyOS WAN/LAN or primary lab uplink.

### Practical notes

- **X550-T2** uses the **`ixgbe`** driver on Proxmox/Linux; verify link speed and cable (**Cat6/Cat6a** for longer 10G-T runs).
- **SR-IOV:** the host retains **PFs** (`ixgbe`); **VyOS** and **other selected VMs** receive **VF** devices via Proxmox **`hostdev`**. Do **not** treat host **PF** IPs as parallel paths into the same L2 as VyOS **WAN/LAN** (or VM VF segments) without **VLAN** discipline (see [network-and-services.md](network-and-services.md)).

### Suggested documentation fields (fill in when cabled)

- **X550 WAN port:** which physical port, negotiated speed, cable type to modem.
- **X550 LAN port:** speed, cable to switch or downstream LAN.
- **VF map:** **BDF → guest** for **VyOS** (2) and **every other VM** using an X550 VF; **`ixgbe max_vfs`** value on the host.
- **Onboard `enp0s31f6` (or equivalent):** static management IP on an **isolated or trusted** management VLAN/L2, if used.

## Power, cooling, chassis

- **PSU:** efficient rating (Gold+ typical); sizing for disk spin-up and NIC peaks.
- **Cooling:** 24/7 quiet fans if the machine lives near living space.
- **Rack vs tower:** affects NIC layout and airflow.

## Out of scope for this doc (but link decisions later)

- UPS model and shutdown integration with Proxmox.
- Physical rack, patch panel, and cable plant.

## Checklist (copy when shopping / commissioning)

- [ ] **X550-T2** installed; **VT-d / IOMMU** enabled in UEFI; **`max_vfs`** covers **VyOS + other VM** VF assignments
- [ ] **Onboard NIC** reserved for **Proxmox management**; management IP and firewall rules documented
- [ ] CPU with **virtualization** + **AES-NI** + **IOMMU** (for X550 **VF** assignment to VyOS)
- [ ] **32 GB RAM** when budget allows (current **16 GB** is tight — see table above)
- [ ] **Disk layout:** 960 EVO = OS; SN770 = VM/LXC + ISO **storage ID** created and set default; HDD = bulk **mount** (e.g. `/mnt/data`) + backup/NAS role
- [ ] ECC if using ZFS for important data
- [ ] Power / cooling / noise acceptable for install location
