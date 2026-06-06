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
| **Dedicated circuit** | **Planned** — electrician-installed | **Basement rack only** — separate from general basement / upstairs circuits |
| PDU | **Eaton PDU1215** | Rack power distribution — homelab gear (Proxmox, modem, switch) |
| Power conditioner | **Furman PST-8 SMP+ EVS LiFT** (15 A) | AV conditioning — **Denon**, **Hypex**, sources |

### Basement dedicated power (planned)

A **dedicated electrical line** will feed the **basement rack area** — isolated from shared household circuits to reduce noise and avoid tripping breakers when **Denon**, **Hypex**, **Proxmox**, and disk spin-up load the rack simultaneously.

| Topic | Guidance |
|-------|----------|
| **Scope** | **Basement rack** — **PDU1215** + **Furman** (+ any rack-mounted gear). **Not** the family room **TV** or **HSU sub** (those stay on existing room circuits). |
| **Typical sizing** | **20 A** dedicated circuit is a common target for AV + homelab rack; confirm load with your electrician (**Denon** + **NCx500** + **850 W PSU** + switch + modem + sources). |
| **Receptacles** | At least **one duplex** at the rack (or **two circuits** if you split **AV/Furman** from **homelab/PDU** — document choice after install). |
| **Install** | Licensed electrician; label panel breaker (**“Basement rack”** or similar). |

Record **breaker size**, **receptacle count**, and **install date** in **`av-theatre.md` § Power** when complete.

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
| PSU | **Seasonic Focus GX-850** (ATX 3.1) | Host power |
| Case | **TBD** | Must fit **3× 3.5″ HDD** and clear the PA120 SE cooler |

### Platform notes

- **270K Plus:** optional efficiency profile **PL1 = PL2 = 65 W** in BIOS for 24/7 use (restore stock **125 / 250 W** for heavy jobs).
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
| **Dedicated basement circuit** | **Planned** — electrician install; feeds **PDU1215** + **Furman** at rack |
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
