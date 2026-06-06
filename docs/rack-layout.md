# Rack layout — Eaton SR18UB (18U)

Physical placement for the **basement** rack. Parts list and power: **`equipment-inventory.md`**. AV signal flow: **`av-theatre.md`**.

**Last updated:** 2026-06-06

**Cabinet:** **18U**, rails adjust **3–32.5″** depth. Numbering **U1 = bottom**, **U18 = top**. Goal: **maximize rack U**; only **two items** live outside the cabinet.

## Outside the rack (minimal)

| Item | Placement | Why not in rack |
|------|-----------|-----------------|
| **Proxmox tower** | **Floor**, flush left or right of rack | **~10U** tall with **PA120 SE** + planned **5060 Ti** |
| **CyberPower CST1500SUC** | **On top** of cabinet | **Mini-tower** (~**14″** tall standing); saves **~3U** inside; still on **socket B** → PDU chain |

**Power unchanged:** duplex **socket A** → **Furman** (in rack) · **socket B** → **UPS on top** → **PDU** (in rack) → homelab gear.

## Inside the rack (~15U used + **3U** reserve)

**0U (rear posts):** **Tripp Lite PDU** — vertical **0U** on rear rail; homelab plugs face rear.

**Rear wall (no U):** **Furman PST-8** — keyhole mount on **side panel** or rear rail plate (Furman manual); **8′ cord** reaches **socket A**.

```text
  U18 ─┬─ 1U  Cisco CBS350-24FP-4G (rack ears, cables to rear)
       │
  U17 ─┤  1U  Cable manager / brush panel (patch to Denon, switch, modem)
       │
 U16 ──┤
 U15 ──┤  4U  Vented shelf — Denon AVR-X3700H (~6.6″ H, 15.3″ D, 28 lb)
 U14 ──┤      HDMI to family room from rear; pre-outs down to Hypex
 U13 ──┤
       │
 U12 ──┤  2U  Vented shelf — **Buckeye** Hypex NCx500 (**14″ D × 3.5″ H**; not rack-ear mount)
 U11 ──┤      RCA from Denon; speaker binding posts toward rear
       │
 U10 ──┤
  U9 ──┤  3U  Vented shelf — PS5 (vertical), Switch 2 dock, Apple TV
  U8 ──┤      Short HDMI up to Denon; Ethernet to CBS350
       │
  U7 ──┤  2U  Shelf — Arris S33 modem (coax in from wall; **2.5G** patch to Proxmox WAN NIC)
  U6 ──┤
       │
  U5 ──┤
  U4 ──┤  2U  **Reserved** — future **Buckeye NCx252MP** 4ch (**14″ D × 3.5″ H**, vented shelf)
  U3 ──┤  1U  Blank / airflow (keep clear under AV shelves)
       │
  U2 ──┤  2U  Optional: shallow drawer / KVM / tool shelf
  U1 ──┘
```

## Mounting notes

| Gear | Mount | Tips |
|------|-------|------|
| **CBS350** | **1U** rack ears | Depth **13.6″** — fits SR18UB; leave **~2U** service slack behind for SFP/PoE cabling |
| **Denon** | **4U vented shelf** | Remove or fold **Wi‑Fi/BT antennas**; leave **2″** rear gap for HDMI/power; **ECO mode** if heat stacks |
| **Hypex NCx500** | **2U vented shelf** (**decided**) | **Buckeye** desktop case — **no rack ears**; **14″ × 14″ × 3.5″** |
| **NCx252MP 4ch** *(future)* | **2U vented shelf** at **U4–U5** | **Buckeye** **12″ × 13″ × 3.5″** — same shelf approach as **NCx500** |
| **PS5 / Switch / Apple TV** | **3U open shelf** | **PS5 vertical**; gap between consoles for exhaust; short HDMI runs to Denon |
| **S33** | **2U shelf** (shares with spare space) | **Coax** strain-relief; **2.5G** short patch to **Proxmox** (floor run along rack side) |
| **Furman** | **Rear/side keyhole** | **Now:** Denon, **NCx500**, sources · **Future:** + **NCx252MP** — **8 outlets** on **PST-8** |
| **PDU** | **0U rear vertical** | From **UPS on top**; feeds **CBS350** + **Proxmox** (floor cable into case) |

## Cabling order (install once, label everything)

1. **Coax** → **S33** (U7) → **2.5G** → **Proxmox WAN** (floor)
2. **CBS350** (U18) → **Proxmox** mgmt + LAN NIC + **PS5 / Apple TV / Switch**
3. **Denon** HDMI out → ceiling/wall homerun to **LG B7**
4. **Denon** pre-outs → **NCx500** (+ **NCx252MP** when added); speaker wires → rear brush panel → family room homeruns
5. **Denon** sub **LFE** → family room homerun
6. **Furman** (AV power) · **UPS → PDU** (homelab power)

## If you later want Proxmox **in** the rack

Swap **U3–U1** for a **10U heavy-duty 4-post shelf** and move **UPS** to the floor — you lose reserve U and tighten airflow. **Floor tower + UPS on top** is the better default for **18U**. Adding **NCx252MP** uses **U4–U5** only — still fits with current layout.
