# Beryl 7 travel router — PLANNED / design notes

Not deployed. Do not treat this file as current infrastructure. Do not stretch home VLANs or home subnets onto the Beryl. Invariants for the house: `REQUIREMENTS.md`. House VLAN 40 is VyOS `wg2` (`wireguard/wg2.md`). CT 108 is still installed and is not that path. US remote access: `wireguard/` (`wg0` and `wg1` live).

This is a **design record**, not a rebuild runbook. There is no `setup.md` yet.

Hardware: GL.iNet **GL-MT3600BE (Beryl 7)**, in `inventory.md`. Independent travel router. Its LAN prefixes must not overlap `10.10.0.0/16`.

## Decision

House US access is **WireGuard on VyOS** (`wg0` private / `wg1` guest). India stays Tailscale (`asus-nuc`). Do not build `tailscale-us`. Do not add a VPS.

```text
Phone / Mac / later Beryl
    ├─ wg0  →  NJ Trusted (Xfinity, AdGuard via VyOS, home resources)
    ├─ wg1  →  NJ Guest (Xfinity only)
    └─ Tailscale → asus-nuc     India Internet (CURRENT)

Home VLAN 40 (CURRENT)
    → VyOS PBR → wg2 → asus-nuc
Home VLAN 40 (CT 108, installed, not the path)
    → was VyOS PBR → india-gw 10.10.0.5 → Tailscale → asus-nuc
```

The Beryl is a **later** sitting. Do not reshape VyOS, VLANs, or Services for it. It may consume the house `wg0` / `wg1` listeners when that sitting happens.

## Why this exists (when we get to it)

The Beryl would have three jobs on hotel/café/public Wi-Fi:

| Beryl network | Intent |
|---------------|--------|
| Private | Path to NJ. Xfinity. AdGuard. Explicit home resources. Fail closed. |
| Guest | Path to NJ. Xfinity **only**. Never RFC1918/home. Fail closed. |
| India | Path to `asus-nuc`. Indian ISP. Never the US house. Fail closed. |

Home Guest (VLAN 20) / `wg1` and Beryl Guest are the same *idea* and different packet paths.

US VPN is on VyOS, not a Services LXC. Do not put a VPN guest in VLAN 10/20/40. India-GW stays on Services.

## Beryl vs Tailscale (open problem)

GL.iNet Tailscale is **beta**. Official docs: do **not** run it together with WireGuard Client.

Private and Guest on the Beryl match house `wg0` / `wg1` (VPN Dashboard is a WireGuard feature). India on the Beryl is still the awkward Tailscale identity. When this sitting happens, do not add a second home VPN for India — put Tailscale on the devices for `asus-nuc`, or use the Beryl as a dumb hotel radio for that SSID.

Firmware notes worth keeping (4.9.0, 2026-07-07): Main / Guest / IoT are the three stock isolated networks; extra LuCI SSIDs are not first-class VPN sources; IoT has no 6 GHz. VPN Dashboard multi-tunnel + kill switch is a WireGuard/OpenVPN feature, not Tailscale.

## Home addresses

```text
10.10.0.1   VyOS
10.10.0.2   UniFi
10.10.0.3   unused
10.10.0.4   AdGuard
10.10.0.5   india-gw         installed, not the VLAN 40 path
10.10.80.1  wg0 private      live      Trusted
10.10.81.1  wg1 guest        live      Internet only
10.10.82.1  wg2              live      VLAN 40 → asus-nuc (.2)
```

No `vpn-us-guest` LXC. No `tailscale-us`. Guest VPN is `wg1` on VyOS.

## What not to do

- Put Tailscale/VPN LXCs in VLAN 10 or 20
- PBR to a next-hop on the same client VLAN
- Stretch home subnets onto the Beryl
- Mix Tailscale and WireGuard Client on the Beryl
- Add a VPS or rebuild `tailscale-us` for travel
- Redo India-GW because of the Beryl

## Firmware sources (research snapshot)

- Beryl 7: https://docs.gl-inet.com/router/en/4/user_guide/gl-mt3600be/
- VPN Dashboard: https://docs.gl-inet.com/router/en/4/interface_guide/vpn_dashboard/
- Tailscale (beta + WG conflict): https://docs.gl-inet.com/router/en/4/interface_guide/tailscale/
- Wireless Main/Guest/IoT: https://docs.gl-inet.com/router/en/4/interface_guide/wireless/
- Firmware: https://dl.gl-inet.com/router/mt3600be/stable

Not the same sitting as the S33 swap, AdGuard, or `wireguard/` apply.
