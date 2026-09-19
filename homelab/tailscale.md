# Tailscale — PLANNED

Not deployed on the homelab. Do not run Tailscale on VyOS. Do not treat this file as current infrastructure.

| Node | Where | IP | Role |
|------|-------|----|------|
| `asus-nuc` | India (physical) | — | exit node — already live |
| `tailscale-us` | LXC `vmbr-svc` | `10.10.0.52` | subnet router + US exit. No `--exit-node` |
| `tailscale-india` | LXC `vmbr-svc` | `10.10.0.54` | India-GW: `--exit-node=asus-nuc` only. No advertised routes |

One process cannot advertise a US exit and consume `asus-nuc`. VLAN 40 clients do not run Tailscale; VyOS should next-hop them to `.54` when this is implemented.

Tags: `tag:homelab-sr` on `tailscale-us`; `tag:homelab-india-gw` on `tailscale-india`.

Advertise from `tailscale-us` only: `10.10.0.0/24`, `10.10.10.0/24`. Not guest/iot/india.

Gateway for both LXCs: `10.10.0.1`. Do not reuse `.53` (AdGuard).

Not the same sitting as the S33 swap. The India SSID is live (`unifi.md`); India-GW is not. VLAN 40 still has no normal WAN egress and must fail closed off the US WAN once India-GW exists. Invariants: `REQUIREMENTS.md`.
