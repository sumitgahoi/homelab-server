# Homelab topology

As-built wiring and addressing. Required behavior: `REQUIREMENTS.md`. Host interfaces: `proxmox/interfaces` / `proxmox/interfaces.until-cutover`. Router known-good: `vyos/commands.txt` (rebuild: `vyos/setup.md`). Switch known-good: `cbs350/running-config` (restore: `cbs350/README.md`).

**As-built:** LAN = CBS350 + `vmbr0`. WAN = S33 → house ASUS → `nic1` / `vmbr1` (double NAT). Host `192.168.50.200/24` on `vmbr1` until cutover. UniFi OS Server + U6+ are live (`unifi.md`). India-GW is live (`tailscale-india/`). That ASUS is not `asus-nuc`.

**After WAN/management cutover:** drop the `vmbr1` address; host `10.10.10.3` on `vmbr0.10`, gateway `10.10.10.1`. Known-good file: `proxmox/interfaces`.

**PLANNED WAN:** S33 2.5G → `nic1` / `vmbr1` → VyOS `eth1`. Retire the house ASUS.

```text
  Internet ── S33 ── ASUS ── nic1 ── vmbr1 ── VyOS eth1     ← as-built
  Internet ── S33 ────────── nic1 ── vmbr1 ── VyOS eth1     ← planned

  CBS350 GE1 ── nic0 ── vmbr0 ── VyOS eth0 (LAN trunk)
       │                 ├── .10 trusted   10.10.10.1
       │                 ├── .20 guest     10.10.20.1
       │                 ├── .30 iot       10.10.30.1
       │                 └── .40 india     10.10.40.1
       ├── GE2 ── UniFi U6+ (native 10, tagged 20/30/40)
       └── Proxmox 10.10.10.3 on vmbr0.10   ← after cutover

  vmbr1 ── Proxmox 192.168.50.200           ← until cutover
  vmbr-svc (no NIC) ── VyOS eth2  10.10.0.1
                   ├── UniFi OS       10.10.0.2
                   └── India-GW       10.10.0.5    ← CURRENT (CT 108)
```

| Index | NIC | Bridge | VM slot | MAC | VyOS | Role |
|-------|-----|--------|---------|-----|------|------|
| 0 | `nic0` | `vmbr0` | `net0` | `02:00:00:00:00:00` | `eth0` | LAN trunk → CBS350 GE1 |
| 1 | `nic1` | `vmbr1` | `net1` | `02:00:00:00:00:01` | `eth1` | WAN (host `192.168.50.200` until cutover; then no host IP) |
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

Until cutover: Proxmox `192.168.50.200` on `vmbr1` (ASUS LAN).

VLAN 10 (after cutover, using `proxmox/interfaces`):

| Address | Device |
|---------|--------|
| `10.10.10.1` | VyOS `eth0.10` |
| `10.10.10.2` | CBS350 |
| `10.10.10.3` | Proxmox `vmbr0.10` |
| `10.10.10.99` | admin laptop OOB |
| `10.10.10.100`–`.250` | DHCP |

Services (`vmbr-svc`, not a VLAN):

| Address | Device |
|---------|--------|
| `10.10.0.1` | VyOS `eth2` |
| `10.10.0.2` | UniFi OS Server (LXC 107) |
| `10.10.0.3` | `tailscale-us` — PLANNED |
| `10.10.0.4` | AdGuard — PLANNED |
| `10.10.0.5` | `tailscale-india` (India-GW, CT 108) — CURRENT |

Planned Services addresses: `adguard.md`, `tailscale-us.md`. India-GW rebuild: `tailscale-india/setup.md`. `asus-nuc` is not on this subnet.

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
| 1 | until cutover | `https://192.168.50.200:8006` |
| 1 | after cutover | `https://10.10.10.3:8006` |
| 2 | VyOS down | CBS350 port 12, static `10.10.10.99/24`, no GW |
| 2b | switch dead | laptop ↔ `nic0`, VLAN 10 |
| 3 / BMC | no network / OS dead | iKVM |

Do not use the Proxmox Network UI Apply button for host bridges. Host-bridge activation is manual (`ifreload -a`); see `proxmox/README.md`.

## Still ahead

See `REQUIREMENTS.md` (PLANNED / DEFERRED). AdGuard / US Tailscale notes: `adguard.md`, `tailscale-us.md`. India-GW is already documented as known-good in `tailscale-india/`. Not the same sitting as the S33 swap.
