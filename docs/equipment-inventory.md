# Equipment inventory

Parts list for **homelab** and **AV home theatre**. Homelab services: `proxmox.md`, `vyos.md`, etc. AV design: **`av-theatre.md`**. Context: **`agent.md`**.

**Last updated:** 2026-06-06

## Physical layout

| Location | Equipment |
|----------|-----------|
| **Basement** | **Eaton rack** — Proxmox host, **VyOS** path (modem, switch), **Denon AVR-X3700H**, **Hypex NCx500**, **Furman**, **PS5**, **Switch 2**, **Apple TV**, **Cisco CBS350** |
| **First floor — family room** | **LG OLED 65″ B7**, all **in-wall / in-ceiling speakers** (LCR, surround, Atmos), **HSU VTF-15H MK2** sub |

**Structured wiring is complete.** Speaker, **LFE**, **HDMI**, and network runs between basement and family room are already in place — commission and document terminations only.

## Internet (ISP and modem)

| Item | Model / plan | Role |
|------|----------------|------|
| ISP | **Xfinity** — **2 Gbps** plan | WAN service |
| Cable modem | **Arris Surfboard S33** | DOCSIS 3.1 modem — **2.5 GbE** LAN to VyOS **WAN** |

Cable **modem 2.5G LAN → router NIC WAN** (S33 has one **2.5G** and one **1G** port — use **2.5G** for VyOS).

## Rack and power

| Item | Model | Role |
|------|-------|------|
| Rack | **Eaton SR18UB** | 18U enclosure — **basement**; homelab + AV electronics |
| **Dedicated circuit** | **1× 20 A** — **12 AWG copper** | **Basement rack** — separate from general household loads |
| PDU | **Tripp Lite** rack PDU | **Socket B** via **UPS** — homelab only; **not** through Furman |
| UPS | **CyberPower CST1500SUC** | **1500 VA / 900 W**, sine wave — **socket B** → **PDU**; homelab path only |
| Power conditioner | **Furman PST-8 SMP+ EVS LiFT** (15 A) | **Socket A** — **Denon**, **Hypex**, sources only |

### Basement dedicated power — **decided**

One **20 A** dedicated circuit (**12 AWG copper**) feeds a **duplex receptacle** at the basement rack. **AV** and **homelab** share the branch but use **separate outlets and separate power strips** — the server never plugs into the Furman.

```text
  Panel ── 20 A "Basement rack" (12 AWG) ──► duplex at rack
                    │
         Socket A ──┴── Furman PST-8 (LiFT) → Denon, Hypex NCx500, PS5, Switch 2, Apple TV
         Socket B ───── CyberPower CST1500SUC → Tripp Lite PDU → Proxmox, S33 modem, CBS350
```

| Metric | Value |
|--------|--------|
| **Branch capacity** | **2,400 W** peak · **~1,920 W** continuous (80% rule) |
| **Planning peak** | **~14 A** — homelab **~6–7 A** (GPU + CPU) + AV transient **~8 A** simultaneous |
| **Headroom** | **~6 A** on a **20 A** breaker — **still single homerun** |

**Why one circuit is sufficient here:**

- **Ampacity** — even aligned worst-case transients stay well under **20 A** with large margin.
- **Noise path** — homelab load is on the **PDU (socket B)**, not daisy-chained through the Furman. **LiFT** filters line noise for everything on **socket A** only. The **Hypex NCx500** uses a regulated **SMPS**, not a mains-hum-sensitive linear transformer amp.
- **Duplex note** — sockets **A** and **B** share the same **hot/neutral**; the Furman is a **filter for its outlets**, not a second breaker. That is enough for this stack — a second **20 A** homerun would add **trip isolation** and **future expansion** headroom, not a requirement for safe or clean operation today.

**Estimated draw (120 V, planning):**

| Group | Typical | Heavy peak |
|-------|---------|------------|
| **AV** — Denon + Hypex + sources | **200–400 W** (~2–3 A) | **~900 W** (~7–8 A) |
| **Homelab** — Proxmox + modem + switch | **150–350 W** (~1–3 A) | **~750 W** (~6–7 A) GPU transcode + PoE + disks |

**Family room (unchanged):** **LG B7** + **HSU VTF-15H MK2** on existing room circuits. If **LFE hum** appears, suspect **signal ground loops** on the long RCA homerun or **shared loads upstairs** before upgrading basement branch count.

### UPS — **decided (homelab only)**

| Item | Model | Role |
|------|-------|------|
| **UPS** | **CyberPower CST1500SUC** | **1500 VA / 900 W**, line-interactive, **true sine** on battery — active-PFC safe |
| **Scope** | **Socket B** → **UPS** → **Tripp Lite PDU** | **Proxmox**, **S33** modem, **CBS350** |
| **Not on UPS** | **Furman / AV** — Denon, Hypex, consoles, Apple TV | **Furman** handles surge/EVS/LiFT for AV |

**Why homelab only:** **Proxmox** + disks + **VyOS VM** need **graceful shutdown** (**NUT** on Proxmox). Brief ride-through keeps **WAN** up while the **modem** is on the UPS-fed PDU. **Denon + Hypex** peaks (~**900 W**) would oversize the UPS for no benefit.

**Sizing (homelab on UPS):**

| Load | Typical | Peak |
|------|---------|------|
| Proxmox + **RTX 5060 Ti** | **200–350 W** | **~550 W** |
| **S33** modem | **~10 W** | **~15 W** |
| **CBS350** | **~50 W** | **~100 W** |
| **Total** | **~300 W** | **~650 W** vs **900 W** UPS rating |

**Install:** **Mini-tower** on a **shelf** in **Eaton SR18UB** (not 2U rackmount). Use **6 battery-backed** outlets for the **PDU** plug. **USB** → **Proxmox** for **NUT** (`usbhid-ups`; add **`usbcore.autosuspend=-1`** in GRUB if USB flaps). Runtime at **~650 W** is short (**~3–5 min** est.) — enough for **NUT** shutdown, not long runtime. Does **not** change **single 20 A** branch sizing.

## Network

| Item | Model | Role |
|------|-------|------|
| Managed switch | **Cisco CBS350-24FP-4G** | 24× PoE+ access, 4× SFP uplinks; **802.1Q** trunk to VyOS **LAN** when VLANs are enabled |

Document which switch port is the **VyOS LAN trunk** and which ports serve **AV / media** clients when cabled.

## Proxmox host

| Item | Model | Role |
|------|-------|------|
| CPU | **Intel Core Ultra 7 270K Plus** (LGA1851, 24C/24T) | Hypervisor — VT-x/VT-d, Xe iGPU, Quick Sync |
| Motherboard | **Gigabyte Z890** | Onboard **2.5 GbE** for **Proxmox management only** |
| RAM | **32 GB DDR5** | Plan **64 GB+** for full service stack |
| Router NIC | **10Gtek X550-T2 clone** (`ixgbe` / `ixgbevf`) | Dual RJ45 — **WAN** + **LAN** to VyOS |
| CPU cooler | **Thermalright Peerless Assassin 120 SE** | 24/7 air cooling |
| PSU | **Seasonic Focus GX-850** (ATX 3.1) | Host power — sufficient for **270K Plus** + **RTX 5060 Ti** (**180 W** TGP) |
| GPU | **NVIDIA GeForce RTX 5060 Ti** (**16 GB** preferred) | **180 W** TGP — passthrough, transcode, or host workloads; **1× PCIe 8-pin** |
| Case | **TBD** | Must fit **3× 3.5″ HDD**, **PA120 SE**, and **GPU** length/clearance |

### Platform notes

- **270K Plus:** optional efficiency profile **PL1 = PL2 = 65 W** in BIOS for 24/7 use (restore stock **125 / 250 W** for heavy jobs).
- **RTX 5060 Ti:** reference **180 W** TGP; host **850 W** PSU is adequate. Enable **Above 4G Decoding** / **Re-Size BAR** in BIOS for passthrough. **16 GB** SKU preferred over **8 GB** for VM/AI headroom.
- **10Gtek X550 clone:** treat like Intel X550-T2; verify **`ixgbe`** + SR-IOV on your Proxmox kernel before adopting VF topology.
- **Onboard 2.5G:** management only — not VyOS WAN/LAN.

## Storage

| Disk | Model | Role |
|------|-------|------|
| OS NVMe | **Samsung 960 EVO 250 GB** | Proxmox VE root only |
| VM / LXC NVMe | **WD Black SN770 1 TB** | Primary datastore — guests, ISOs, templates |
| Bulk HDD | **WD Red WD30EFRX 3 TB** | NAS / media / backups |

**Case constraint:** chassis must support **3× 3.5″** positions if you add more bulk drives later.

## AV home theatre — video

| Item | Model | Role |
|------|-------|------|
| Display | **LG OLED 65″ B7** | **Family room** — **HDMI** from basement **Denon** (pre-run) |

## AV home theatre — processing and amplification

| Item | Model | Role |
|------|-------|------|
| AV receiver | **Denon AVR-X3700H** | **Basement rack** — HDMI switching, **Audyssey**, pre-outs to Hypex |
| Power amp | **Hypex NCx500** — **3-channel** | **Basement rack** — **L / C / R** → pre-run speaker homeruns to family room |
| Subwoofer | **HSU Research VTF-15H MK2** (powered) | **Family room** — **LFE** from basement Denon (pre-run); **local AC** |

## AV home theatre — speakers (in-wall / in-ceiling)

| Item | Model | Qty | Role |
|------|-------|-----|------|
| LCR | **Bowers & Wilkins CWM73 S2** | **3** | **Family room** — L / C / R (fed from basement **Hypex**) |
| Surround | **Bowers & Wilkins CWM663** | **2** | **Family room** — surround pair (fed from basement **Denon** amps) |
| Height / Atmos | **Bowers & Wilkins CCM662** | **2** | **Family room** — ceiling Atmos pair (fed from basement **Denon** amps) |

**Layout (reference):** **3.2.2** bed + **Atmos** — **3× CWM73 S2** (LCR via Hypex) · **2× CWM663** (surround) · **2× CCM662** (Atmos) · **1× HSU VTF-15H MK2** (sub). Signal flow and Audyssey: **`av-theatre.md`**.

## AV home theatre — sources

| Item | Model | Role |
|------|-------|------|
| Game console | **Sony PlayStation 5** | **Basement rack** → **HDMI** to Denon |
| Game console | **Nintendo Switch 2** | **Basement rack** → **HDMI** to Denon |
| Streamer | **Apple TV** | **Basement rack** → **HDMI** to Denon |

Sources and **Denon** live in the **basement**; **HDMI** to **LG B7** and all **speaker** homeruns are pre-wired to the **family room**.

## Open decisions

| Topic | Options | Constraint |
|-------|---------|------------|
| **Case** | Reuse **Fractal Design Define C** or buy new | **3× 3.5″ HDD** mounts; **Define C** has only **2×** native trays — third drive needs adapter or different case (**Define 7**, **Meshify 2**, etc.). Confirm clearance with **PA120 SE**. |
| **Source / AV VLAN** | Flat LAN vs dedicated **private** VLAN port | Phase 1 flat LAN is fine; revisit when VLANs are enabled |

## Cabling

Pre-run wiring is **done**. Label and verify terminations at commission time.

| Link | Basement (rack) | First floor (family room) | Notes |
|------|-----------------|---------------------------|-------|
| **WAN** | **S33** **2.5G** → router NIC | — | Xfinity **2 Gbps** |
| **LAN** | Router NIC → **CBS350** | — | Trunk / access ports TBD |
| **HDMI** | **Denon** main out | **LG B7** | Pre-run **HDMI** homerun; enable **eARC** on TV if needed |
| **LCR speakers** | **NCx500** outputs | **3× CWM73 S2** | Pre-run speaker homeruns |
| **Surround / Atmos** | **Denon** speaker terminals | **2× CWM663**, **2× CCM662** | Pre-run speaker homeruns |
| **Sub LFE** | **Denon** sub out | **HSU VTF-15H MK2** | Pre-run **RCA/LFE**; sub **AC** local to family room |
| **Ethernet** | **CBS350** | — (AV devices in basement) | PS5 / Apple TV / Switch → switch in rack |
