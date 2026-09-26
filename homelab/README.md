# Homelab topology

As-built wiring and addressing. Required behavior: `REQUIREMENTS.md`. Host interfaces: `proxmox/interfaces` (CURRENT). Pre-cutover host snapshot: `proxmox/interfaces.until-cutover` (archive). Router known-good: `vyos/commands.txt` (rebuild: `vyos/setup.md`). Switch known-good: `cbs350/running-config` (restore: `cbs350/README.md`).

**As-built:** LAN = CBS350 + `vmbr0`. WAN = S33 2.5G → `nic1` / `vmbr1` → VyOS `eth1` (IPv4 + IPv6). House ASUS retired. Host `10.10.10.3/24` on `vmbr0.10`, gateway `10.10.10.1`. UniFi OS Server + U6+ are live (`unifi.md`). VLAN 40 egress is VyOS `wg2` to `asus-nuc` (`wireguard/wg2.md`). CT 108 is deprecated and is not that path. Client VLANs stay IPv4.

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
                   ├── AdGuard        10.10.0.4    ← CURRENT (CT 110)
                   └── India-GW       10.10.0.5    ← DEPRECATED (CT 108; stop, keep the disk)

  wg0, wg1, and wg2 live (see wireguard/; VLAN 40 is wg2)
```

| Index | NIC | Bridge | VM slot | MAC | VyOS | Role |
|-------|-----|--------|---------|-----|------|------|
| 0 | `nic0` | `vmbr0` | `net0` | `02:00:00:00:00:00` | `eth0` | LAN trunk → CBS350 GE1 |
| 1 | `nic1` | `vmbr1` | `net1` | `02:00:00:00:00:01` | `eth1` | WAN (no host IP) |
| 2 | — | `vmbr-svc` | `net2` | `02:00:00:00:00:02` | `eth2` | Services `10.10.0.1/24` |

MACs are set when you create the VM. Do not delete/re-add guest NICs. BMC = onboard Realtek 1G (AST2600), not OS networking.

## Networks

| Network | VLAN | Subnet | Gateway |
|---------|------|--------|---------|
| Trusted | 10 | `10.10.10.0/24` | `10.10.10.1` |
| Guest | 20 | `10.10.20.0/24` | `10.10.20.1` |
| IoT | 30 | `10.10.30.0/24` | `10.10.30.1` |
| India | 40 | `10.10.40.0/24` | `10.10.40.1` |
| Services | — | `10.10.0.0/24` | `10.10.0.1` |

Trunks allow 10/20/30/40. DHCP `.100`–`.250` on the four client VLANs. Policy: `REQUIREMENTS.md`. Wi-Fi: `unifi.md`.

## Addresses

VLAN 10 (`proxmox/interfaces`):

| Address | Device |
|---------|--------|
| `10.10.10.1` | VyOS `eth0.10` |
| `10.10.10.2` | CBS350 |
| `10.10.10.3` | Proxmox `vmbr0.10` |
| `10.10.10.10` | dev VM (VM 101) — CURRENT, `dev/` |
| `10.10.10.99` | admin laptop OOB |
| `10.10.10.100`–`.250` | DHCP |

Services (`vmbr-svc`, not a VLAN):

| Address | Device |
|---------|--------|
| `10.10.0.1` | VyOS `eth2` |
| `10.10.0.2` | UniFi OS Server (LXC 107) |
| `10.10.0.3` | unused (do not assign; was a US Tailscale guest) |
| `10.10.0.4` | AdGuard (CT 110) — CURRENT |
| `10.10.0.5` | `tailscale-india` (CT 108) — DEPRECATED; reserved until destroy |

AdGuard rebuild: `adguard/setup.md`. US VPN is on VyOS, not Services: `wireguard/` (`wg0` and `wg1` live). VLAN 40 path: `wireguard/wg2.md`. CT 108 is deprecated; the Tailscale procedure stays at `tailscale-india/setup.md`. `asus-nuc` is not on this subnet.

## CBS350

Known-good snapshot: `cbs350/running-config`. Restore from factory: `cbs350/README.md`. Duplicate running-config to startup-config after the file is on the switch.

Mgmt `10.10.10.2/24`, gw `10.10.10.1`. No SVIs on 20/30/40. Unassigned ports fall to VLAN 1. Live gear may still have leftover VLAN 99 (deferred strip).

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

Guest has no wired port (wireless-only). Port 12 is recovery when VyOS is down. VLAN 1 has no SVI; factory `192.168.1.254` is removed when Git `running-config` is restored.

## Recovery

| Tier | When | Path |
|------|------|------|
| 1 | Proxmox UI | `https://10.10.10.3:8006` |
| 2 | VyOS down | CBS350 port 12, static `10.10.10.99/24`, no GW |
| 2b | switch dead | laptop ↔ `nic0`, VLAN 10 |
| 3 / BMC | no network / OS dead | iKVM |

Do not use the Proxmox Network UI Apply button for host bridges. Host-bridge activation is manual (`ifreload -a`); see `proxmox/README.md`.

## Still ahead

See `REQUIREMENTS.md` (PLANNED / DEFERRED). WAN IPv6 on `eth1` only is CURRENT (`vyos/setup.md`). AdGuard is CURRENT (`adguard/`). WireGuard: `wireguard/` (`wg0`, `wg1`, and `wg2` live). Dev VM is CURRENT (`dev/`). Beryl 7 travel router (not deployed): `beryl.md`. CT 108 is deprecated (`tailscale-india/`). Stop it; keep the disk. Do not deploy `tailscale-us`.
