# Hardware requirements — homelab server

This document captures **minimum and recommended** hardware for the stated software stack. Adjust for your budget, power envelope, and noise tolerance.

## Role of the machine

- Runs **Proxmox VE** on bare metal with multiple guests (VMs/LXCs).
- **Phase 1:** **VyOS** (routing/firewall). **Target stack:** also **Pi-hole** (DNS + blocking) and **Tailscale** (placement in [network-and-services.md](network-and-services.md)).

## Platform status

| | Hardware |
|--|----------|
| **In service today** | ASUS **Z270E** + **i5-7600K**, **16 GB** RAM — see **[`agent.md`](../agent.md#current-hardware-inventory-read-before-recommending-services)**. |
| **Planned upgrade (chosen)** | **Intel Core Ultra 7 processor 270K Plus** + **ASUS Pro WS W880-ACE SE** — see **§ Planned platform — why 270K Plus + W880** below. |

Disks (**960 EVO** / **SN770** / **WD Red**) and **X550-T2** router NIC are **carried forward** unless noted otherwise.

## Planned platform — why Core Ultra 7 270K Plus + Pro WS W880-ACE SE

**Not** Raptor Lake (13th/14th gen **13700K / 14700K / 14900K**). The chosen CPU is **Arrow Lake Refresh** (**Core Ultra Series 2**), **LGA1851** — [Intel ARK: 270K Plus](https://www.intel.com/content/www/us/en/products/sku/245692/intel-core-ultra-7-processor-270k-plus-36m-cache-up-to-5-50-ghz/specifications.html).

### Why this pair over the main alternatives

| Factor | **270K Plus + W880** | **Typical alternative we passed on** |
|--------|----------------------|--------------------------------------|
| **Workload headroom** | **24 cores / 24 threads** for Proxmox running **VyOS**, **Pi-hole**, **Tailscale**, **Vaultwarden**, **dev node**, **NAS**, **Navidrome**, **Jellyfin**, and optional **Immich / Frigate / Bazzite** | **Ryzen 9 7900** (12C/24T, **88 W** stock PPT) — strong efficiency, but less room for concurrent heavy guests without compromise |
| **Media / inference** | **Xe iGPU**, **Quick Sync**, **OpenVINO** on CPU/GPU/NPU ([Intel specs](https://www.intel.com/content/www/us/en/products/sku/245692/intel-core-ultra-7-processor-270k-plus-36m-cache-up-to-5-50-ghz/specifications.html)) — useful for **Jellyfin** (decode), **Frigate** / **Immich**-style paths vs CPU-only | **Ryzen 9 7900** — minimal desktop iGPU; offload leans on **Coral / discrete GPU** |
| **Virtualization** | **VT-x / VT-d**; **SR-IOV** + **GPU passthrough** (Bazzite) on a **workstation** board with documented **IOMMU** use cases | Same class on modern AMD, but **Intel iGPU + VF** story matched this build |
| **Board role** | **Pro WS W880-ACE SE** — workstation **LGA1851**, **ECC-capable platform** (confirm **CPU + DIMM QVL**), stable firmware target for **24/7 Proxmox** | Retail **Z890** from a bundle — fine for a desktop; **W880** kept for **ECC path** and **WS** feature set |
| **Cost** | Bundle CPU + RAM + consumer board, then **sell Z890** and install CPU on **W880** (~**$1000** class budget discussed) | **Ryzen 9 7900 + AM5** — competitive, but weaker **iGPU** and lower core count at similar “efficiency-first” positioning |
| **Power at stock** | **125 W** base, **250 W** max turbo — **hotter and hungrier** under full all-core load than **7900** | **7900** wins **sustained MT power** (~**88 W** PPT cap) — see **§ Power limits (PL1 / PL2)** to close the gap when idle |

**Ultra 9 285K** was not required: reviews place **270K Plus** near **285K** productivity for much less cost ([PCMag](https://ca.pcmag.com/processors/6502/intel-core-ultra-7-270k-plus), [Phoronix](https://www.phoronix.com/review/intel-core-ultra-7-270k-plus/15)).

### Power limits (PL1 / PL2) — verified behavior

Intel defines stock power for the **270K Plus** as:

| Intel parameter | Stock value (270K Plus) |
|-----------------|-------------------------|
| **Processor base power** (maps to **PL1** in practice) | **125 W** |
| **Maximum turbo power** (**PL2** / MTP class) | **250 W** |

Source: [Intel Core Ultra 7 processor 270K Plus specifications](https://www.intel.com/content/www/us/en/products/sku/245692/intel-core-ultra-7-processor-270k-plus-36m-cache-up-to-5-50-ghz/specifications.html).

**BIOS (ASUS Intel 800-series family):** ASUS documents **Long Duration Package Power Limit** = **PL1** (watts) and **Short Duration Package Power Limit** = **PL2** (watts) under CPU power management — see the **Intel 800 Series** BIOS manual ([ASUS download center](https://www.asus.com/support/download-center/); same PL1/PL2 naming on **Z890** boards, e.g. [Intel 800 Series BIOS manual excerpt](https://dlcdnets.asus.com/pub/ASUS/mb/13MANUAL/J25827_Intel_800_Series_BIOS_manual_EM_WEB.pdf)). The **Pro WS W880-ACE SE** uses the **Intel W880 / LGA1851** stack; on first boot open **Advanced Mode → Ai Tweaker** (or **Advanced**) and locate **CPU Power Management** / **VRM** — confirm the **Long Duration** / **Short Duration Package Power Limit** fields match the manual.

**Homelab efficiency profile (owner choice):** cap both limits to the same value so burst and sustained package power align — e.g.:

| Setting | Value | Effect |
|---------|-------|--------|
| **PL1** (Long Duration Package Power Limit) | **65 W** | Sustained all-core power stays in a **65 W-class** envelope (similar intent to **Ryzen 9 7900**’s **88 W** PPT cap). |
| **PL2** (Short Duration Package Power Limit) | **65 W** | Short turbo spikes are **not** allowed above PL1 — avoids **250 W** bursts. |

Setting **PL1 = PL2 = 65 W** is the same idea validated on Arrow Lake in reviews (e.g. **285K** at **65 W** PL via Intel XTU on **Z890**, [Club386](https://club386.com/heres-what-happens-when-you-run-an-intel-core-ultra-9-285k-at-65w/)) — **~60%** of full-power MT throughput in exchange for much lower power. **Trade-off:** all-core performance drops versus stock **125 / 250 W**; **single-thread** and light background load are largely preserved.

**Notes:**

- This is a **firmware** setting (persist until changed), not a one-time OS tweak — you can restore **125 / 250 W** later for heavy transcodes or batch jobs.
- Also set **Package Power Time Window** / any **Multi-Core Enhancement** or **Intel Baseline / Performance** profile so the board does not override your caps (ASUS **Intel Default** vs **Performance** profiles differ — [TechPowerUp on Z890 defaults](https://www.techpowerup.com/326541/intel-z890-chipset-motherboards-to-launch-with-default-power-profile-out-of-the-box)).
- **65 W** is a starting point; **80–125 W** PL1 is a middle ground if guests feel slow.
- Re-validate **thermals** and **Proxmox** guest performance after changing PL limits.

### Before install checklist (270K Plus + W880)

- [ ] **CPU support:** **270K Plus** listed on [Pro WS W880-ACE SE CPU support](https://www.asus.com/motherboards-components/motherboards/workstation/pro-ws-w880-ace-se/helpdesk_cpu_support/) — update BIOS if needed.
- [ ] **RAM:** plan **64 GB+**; if using **ECC**, match **W880 + CPU QVL** (bundle **Crucial non-ECC** may be interim only).
- [ ] **PL1 / PL2** set in BIOS for intended **efficiency vs performance** profile.
- [ ] **VT-d / IOMMU** enabled; **SR-IOV** and **GPU passthrough** tested per [network doc](network-and-services.md) when adopted.

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

**Phase 1 (baby steps):** attach each **physical port** of the add-in NIC to a **Proxmox Linux bridge** (`vmbr-wan`, `vmbr-lan`, or your names) and give the **VyOS** VM **virtio** interfaces on those bridges — **no SR-IOV** yet. Same **modem 2.5G → WAN bridge** guidance as in **§ WAN speed**.

**Target architecture (locked when you adopt it):** a **dual-port** PCIe NIC supplies **WAN** and **LAN** to **VyOS** as **two SR-IOV VFs** (**one VF per physical port / PF**). **Onboard Intel I219-V** (**1 GbE**) is **Proxmox management only** — not VyOS WAN/LAN or the primary lab data path.

**Current inventory (not locked):** **Intel X550-T2** — two **RJ45** ports, **NBASE-T** (**1 / 2.5 / 5 / 10 Gb/s**), Linux **`ixgbe`** + guest **`ixgbevf`**. Strong fit when the **modem LAN** is **multi-gig** (e.g. **2.5G**) or **10G** and you want headroom.

### WAN speed (~1.3 Gbps and similar)

A **true 1 GbE** link to the modem **tops out near ~940–950 Mb/s** TCP after overhead — you **leave bandwidth unused** on a **~1.3 Gbps** plan. Prefer a NIC + cable + modem LAN port that negotiate **at least 2.5 Gb/s** (or **5G / 10G**) on the **WAN** side. **X550-T2** often syncs at **2.5G** or **5G** to many **cable / fiber ONT** multi-gig ports; confirm **modem port speed** and **Cat5e/Cat6** quality.

**Modem offers both 1 GbE and 2.5 GbE LAN:** cable **VyOS WAN** (the router NIC’s **WAN** / **modem** RJ45) to the **2.5 GbE** jack. Reserve the **1 GbE** jack for **non-primary** use (e.g. **temporary laptop** plug-in, **ISP tech** path, or a **downstream device** that only needs 1 Gb/s) — **not** as the main feed into **VyOS** if you want **full** use of a **~1.3 Gbps** subscription.

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

### Proxmox host — GRUB (legacy Intel tower, SR-IOV)

On this **legacy Linux tower** (ASUS **Z270** / **Kaby Lake** class), **SR-IOV and VF assignment** required explicit **kernel command-line** options in addition to **VT-d / IOMMU** in firmware. The working **`GRUB_CMDLINE_LINUX_DEFAULT`** used on the host is:

```bash
GRUB_CMDLINE_LINUX_DEFAULT="quiet pci=realloc,assign-busses,hpbussize=4 intel_iommu=on iommu=pt"
```

Edit **`/etc/default/grub`**, merge with any other parameters you already need, then apply on Debian/Proxmox (`update-grub` / **`proxmox-boot-tool refresh`** as appropriate) and reboot. **Your board, BIOS, and slot layout may differ** — always validate **IOMMU groups** and **VF enumeration** after changes (`dmesg`, **`/sys/kernel/iommu_groups/`**, Proxmox **node** shell).

### Suggested documentation fields (fill in when cabled)

- **WAN port:** physical port on **router NIC**, negotiated speed, cable to modem — **use modem’s 2.5G (or faster) LAN** when the modem exposes **both 1G and 2.5G** (see **§ WAN speed** above).
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
