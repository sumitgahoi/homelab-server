# Beryl 7 travel router — PLANNED / design notes

Not deployed. Do not treat this file as current infrastructure. Do not stretch home VLANs or home subnets onto the Beryl. House behavior: `REQUIREMENTS.md`. Wiring: `README.md`. VLAN 40: `wireguard/wg2.md`. US remote access: `wireguard/`.

This is a **design record**, not a rebuild runbook. There is no `setup.md` yet.

Hardware: GL.iNet **GL-MT3600BE (Beryl 7)**, in `inventory.md`. Independent travel router. Its LAN prefixes must not overlap `10.10.0.0/16`. House addresses: `README.md`. Tunnel prefixes: `wireguard/README.md`.

## Decision

House US access is WireGuard on VyOS (`wg0` / `wg1`). Travel India is the Tailscale app to `asus-nuc`, not a path through the house.

```text
Phone / Mac / later Beryl
    ├─ wg0  →  NJ Trusted (Xfinity, AdGuard via VyOS, home resources)
    ├─ wg1  →  NJ Guest (Xfinity only)
    └─ Tailscale → asus-nuc     India Internet (existing path)

Home VLAN 40 (existing path)
    → VyOS PBR → wg2 → asus-nuc
Home VLAN 40 (CT 108, DEPRECATED)
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

Do not put a VPN guest on a client VLAN for this. US remote access stays on VyOS (`REQUIREMENTS.md`).

## Beryl vs Tailscale (open problem)

GL.iNet Tailscale is **beta**. Official docs: do **not** run it together with WireGuard Client.

Private and Guest on the Beryl match house `wg0` / `wg1` (VPN Dashboard is a WireGuard feature). India on the Beryl is still the awkward Tailscale identity. When this sitting happens, do not add a second home VPN for India — put Tailscale on the devices for `asus-nuc`, or use the Beryl as a dumb hotel radio for that SSID.

Firmware notes worth keeping (4.9.0, 2026-07-07): Main / Guest / IoT are the three stock isolated networks; extra LuCI SSIDs are not first-class VPN sources; IoT has no 6 GHz. VPN Dashboard multi-tunnel + kill switch is a WireGuard/OpenVPN feature, not Tailscale.

## What not to do

- Put Tailscale/VPN LXCs in VLAN 10 or 20
- PBR to a next-hop on the same client VLAN
- Stretch home subnets onto the Beryl
- Mix Tailscale and WireGuard Client on the Beryl
- Add a VPS or rebuild `tailscale-us` for travel
- Rebuild deprecated CT 108 because of the Beryl

## Firmware sources (research snapshot)

- Beryl 7: https://docs.gl-inet.com/router/en/4/user_guide/gl-mt3600be/
- VPN Dashboard: https://docs.gl-inet.com/router/en/4/interface_guide/vpn_dashboard/
- Tailscale (beta + WG conflict): https://docs.gl-inet.com/router/en/4/interface_guide/tailscale/
- Wireless Main/Guest/IoT: https://docs.gl-inet.com/router/en/4/interface_guide/wireless/
- Firmware: https://dl.gl-inet.com/router/mt3600be/stable
