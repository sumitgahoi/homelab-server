# Homelab topology

As-built wiring and addressing. Behavior: `REQUIREMENTS.md`.

```text
  Internet ── S33 ────────── nic1 ── vmbr1 ── VyOS eth1

  CBS350 GE1 ── nic0 ── vmbr0 ── VyOS eth0 (LAN trunk)
       │                 ├── .10 trusted   10.10.10.1
       │                 ├── .20 guest     10.10.20.1
       │                 ├── .30 iot       10.10.30.1
       │                 └── .40 india     10.10.40.1
       ├── GE2 ── UniFi U6+ (native 10, tagged 20/30/40)
       └── Proxmox 10.10.10.3 on vmbr0.10

  vmbr1 ── no host IP
  vmbr-svc (no NIC) ── VyOS eth2  10.10.0.1
                   ├── UniFi OS       10.10.0.2
                   ├── AdGuard        10.10.0.4    (CT 110)
                   └── India-GW       10.10.0.5    ← DEPRECATED (CT 108)

  wg0, wg1, and wg2 live (see wireguard/; VLAN 40 is wg2)
```

| Index | NIC | Bridge | VM slot | MAC | VyOS | Role |
|-------|-----|--------|---------|-----|------|------|
| 0 | `nic0` | `vmbr0` | `net0` | `02:00:00:00:00:00` | `eth0` | LAN trunk → CBS350 GE1 |
| 1 | `nic1` | `vmbr1` | `net1` | `02:00:00:00:00:01` | `eth1` | WAN (no host IP) |
| 2 | — | `vmbr-svc` | `net2` | `02:00:00:00:00:02` | `eth2` | Services `10.10.0.1/24` |

BMC is the onboard Realtek 1G (AST2600), not an OS NIC. Guest NIC MACs: `vyos/install.md`.

## Networks

| Network | VLAN | Subnet | Gateway |
|---------|------|--------|---------|
| Trusted | 10 | `10.10.10.0/24` | `10.10.10.1` |
| Guest | 20 | `10.10.20.0/24` | `10.10.20.1` |
| IoT | 30 | `10.10.30.0/24` | `10.10.30.1` |
| India | 40 | `10.10.40.0/24` | `10.10.40.1` |
| Services | — | `10.10.0.0/24` | `10.10.0.1` |

Trunks allow 10/20/30/40. Policy: `REQUIREMENTS.md`. Wi-Fi: `unifi.md`. DHCP pools: `vyos/setup.md`.

## Addresses

VLAN 10 (`proxmox/interfaces`):

| Address | Device |
|---------|--------|
| `10.10.10.1` | VyOS `eth0.10` |
| `10.10.10.2` | CBS350 |
| `10.10.10.3` | Proxmox `vmbr0.10` |
| `10.10.10.10` | dev VM (VM 101), `dev/` |
| `10.10.10.99` | admin laptop OOB |

Services (`vmbr-svc`, not a VLAN):

| Address | Device |
|---------|--------|
| `10.10.0.1` | VyOS `eth2` |
| `10.10.0.2` | UniFi OS Server (LXC 107) |
| `10.10.0.3` | unused |
| `10.10.0.4` | AdGuard (CT 110) |
| `10.10.0.5` | `tailscale-india` (CT 108) — DEPRECATED; reserved until destroy |

`asus-nuc` is not on this subnet. WireGuard: `wireguard/`.

## CBS350

Mgmt `10.10.10.2/24`, gw `10.10.10.1`. No SVIs on 20/30/40. Unassigned ports fall to VLAN 1. Live gear may still have leftover VLAN 99 (Still ahead). Switch restore: `cbs350/README.md`.

| Port | Mode | VLAN | Use |
|------|------|------|-----|
| 1 | trunk | native 1, tagged 10/20/30/40 | Proxmox `nic0` |
| 2 | trunk, PoE | native 10, tagged 20/30/40 | UniFi U6+ |
| 3 | access | 10 | BMC |
| 4 | access | 10 | spare (NAS is deferred) |
| 5 | access | 10 | PS5 |
| 6 | access | 10 | Apple TV |
| 7 | access | 10 | Switch 2 |
| 8–11 | access | 10 | spare trusted |
| 12 | access | 10 | admin OOB — do not repurpose |
| 13–22 | access | 30 | IoT |
| 23–24 | access | 40 | India |
| SFP 1–4 | shutdown | — | unused |

Guest has no wired port (wireless-only). VLAN 1 has no SVI.

## Recovery

| Tier | When | Path |
|------|------|------|
| 1 | Proxmox UI | `https://10.10.10.3:8006` |
| 2 | VyOS down | CBS350 port 12, static `10.10.10.99/24`, no GW |
| 2b | switch dead | laptop ↔ `nic0`, VLAN 10 |
| 3 / BMC | no network / OS dead | iKVM |

## Still ahead

| Item | Notes |
|------|--------|
| Camera VLAN | Not designed. No VLAN id yet |
| NAS | `10.10.10.4` on CBS350 port 4 |
| VLAN 99 | Strip leftovers on the live switch if any remain |
| Beryl 7 | Not deployed. `beryl.md` |
| CT 108 | Stop and keep the disk (`wireguard/wg2.md` section 7). Destroy later; `10.10.0.5` stays reserved until then (`tailscale-india/`) |
