# Inventory

Catalog only. Design lives in the domain folder.

| Item | Location | Doc |
|------|----------|-----|
| ASUS Pro WS W880-ACE SE · 270K Plus · 32 GB DDR5 · Define R5 | rolling platform | `homelab/proxmox.md` |
| Samsung 960 EVO 250 GB (OS) · WD Black SN770 1 TB (guests) · WD Red 3 TB (bulk) | host | `homelab/proxmox.md` |
| Thermalright PA120 SE · Seasonic GX-850 | host | — |
| Arris S33 (DOCSIS 3.1, 2.5G) | rack U6–U7 | `homelab/README.md` |
| House ASUS router | retired (was on S33; not `asus-nuc`) | — |
| Cisco CBS350-24FP-4G | rack U18 | `homelab/cbs350/running-config`, `homelab/README.md` |
| UniFi U6+ | CBS350 GE2 | `homelab/unifi.md` |
| Eaton SR18UB | basement | `rack/` |
| Eaton Tripp Lite PDU1215 | rack U1 rear | `rack/power.md` |
| CyberPower CST1500SUC | rolling platform | `rack/power.md` |
| Furman PST-8 SMP+ EVS LiFT | rack side/rear | `rack/power.md` |
| Denon AVR-X3700H | rack U13–U16 | `home-theatre/` |
| Buckeye Hypex NCx500 3ch | rack U11–U12 | `home-theatre/` |
| PS5 · Switch 2 · Apple TV 4K | rack U8–U10 | `home-theatre/` |
| LG OLED 65″ B7 | family room | `home-theatre/` |
| 3× B&W CWM73 S2 · 2× CWM663 · 2× CCM662 | family room | `home-theatre/` |
| HSU VTF-15H MK2 | family room | `home-theatre/` |
| `asus-nuc` (Tailscale India exit, `wg-india`) | India (physical) | `homelab/wireguard/wg2.md` |
| GL.iNet GL-MT3600BE Beryl 7 (dual-band Wi-Fi 7 travel router) | travel | — |

Pre-run HDMI, speaker, LFE, and network between basement and family room: **done**.

## Not purchased

| Item | Notes |
|------|-------|
| RTX 5060 Ti 16 GB | Host GPU. Upgrade basement breaker to 20 A first — `rack/power.md` |
| Buckeye NCx252MP 4ch | Surround + Atmos off Denon pre-outs. Shelf reserved U4–U5 |
| Rack shelves / cable manager | 4U Denon (≥16–18″ deep) · 2U Hypex · 3U consoles · 2U S33 · 1U brush |

## Open

- Breaker 15 A → 20 A (12 AWG already in the wall; verify no 14 AWG in the run).
- PS5 model (2020 disc / slim / digital) — console shelf width.
- Camera VLAN and NAS: `homelab/README.md` (Still ahead).
