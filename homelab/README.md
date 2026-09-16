# Homelab topology

As-built wiring and addressing. Required behavior: `REQUIREMENTS.md`. Host interfaces: `ansible/proxmox/files/interfaces`. Router Git state: `vyos/config.boot.j2` (not yet authoritatively applied on the live router).

**As-built:** LAN = CBS350 + VyOS. WAN = S33 → house ASUS → `nic1` (double NAT). Host `10.10.10.3` on `vmbr0.10`. That ASUS is not `asus-nuc`.

**PLANNED WAN:** S33 2.5G → `nic1` / `vmbr1` → VyOS `eth1`. Retire the house ASUS.

```text
  Internet ── S33 ── ASUS ── nic1 ── vmbr1 ── VyOS eth1     ← as-built
  Internet ── S33 ────────── nic1 ── vmbr1 ── VyOS eth1     ← planned

  CBS350 GE1 ── nic0 ── vmbr0 ── VyOS eth0 (LAN trunk)
       │                 ├── .10 trusted   10.10.10.1
       │                 ├── .20 guest     10.10.20.1
       │                 ├── .30 iot       10.10.30.1
       │                 └── .40 india     10.10.40.1
       └── Proxmox 10.10.10.3 on vmbr0.10

  vmbr-svc (no NIC) ── VyOS eth2  10.10.0.1
```

| Index | NIC | Bridge | VM slot | MAC | VyOS | Role |
|-------|-----|--------|---------|-----|------|------|
| 0 | `nic0` | `vmbr0` | `net0` | `02:00:00:00:00:00` | `eth0` | LAN trunk → CBS350 GE1 |
| 1 | `nic1` | `vmbr1` | `net1` | `02:00:00:00:00:01` | `eth1` | WAN (no host IP) |
| 2 | — | `vmbr-svc` | `net2` | `02:00:00:00:00:02` | `eth2` | Services `10.10.0.1/24` |

MACs are set at VM create (`ansible/proxmox/02-vyos-vm.yml`). Do not delete/re-add guest NICs. BMC = onboard Realtek 1G (AST2600), not OS networking.

## Networks

| Network | VLAN | Subnet | Gateway |
|---------|------|--------|---------|
| Trusted | 10 | `10.10.10.0/24` | `10.10.10.1` |
| Guest | 20 | `10.10.20.0/24` | `10.10.20.1` |
| IoT | 30 | `10.10.30.0/24` | `10.10.30.1` |
| India | 40 | `10.10.40.0/24` | `10.10.40.1` |
| Services | — | `10.10.0.0/24` | `10.10.0.1` |

Trunks allow 10/20/30/40. DHCP `.100`–`.250` on the four client VLANs. Policy: `REQUIREMENTS.md`.

## Addresses (VLAN 10, as-built)

| Address | Device |
|---------|--------|
| `10.10.10.1` | VyOS `eth0.10` |
| `10.10.10.2` | CBS350 |
| `10.10.10.3` | Proxmox `vmbr0.10` |
| `10.10.10.99` | admin laptop OOB |
| `10.10.10.100`–`.250` | DHCP |

Planned Services addresses (not live): `adguard.md`, `tailscale.md`.

## CBS350

Last-known/as-built. **Not** Git-enforced. There is no authoritative switch configuration in this repo.

Mgmt `10.10.10.2/24`, gw `10.10.10.1`. No SVIs on 20/30/40. Apply ≠ save: copy running → startup. Unassigned ports fall to VLAN 1.

| Port | Mode | VLAN | Use |
|------|------|------|-----|
| 1 | trunk | 10,20,30,40 | Proxmox `nic0` |
| 2 | trunk, PoE | 10,20,30,40 | UniFi U6+ (not installed) |
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

Guest has no wired port (wireless-only once an AP exists). Port 12 is recovery when VyOS is down. VLAN 1 hatch `192.168.1.254` is still present until dropped on purpose (deferred).

## Recovery

| Tier | When | Path |
|------|------|------|
| 1 | normal | `https://10.10.10.3:8006` |
| 2 | VyOS down | CBS350 port 12, static `10.10.10.99/24`, no GW |
| 2b | switch dead | laptop ↔ `nic0`, VLAN 10 |
| 3 / BMC | no network / OS dead | iKVM |

Do not use the Proxmox Network UI Apply button for host bridges. Host-bridge activation is manual (`ifreload -a`); see `ansible/README.md`.

## Still ahead

See `REQUIREMENTS.md` (PLANNED / DEFERRED). AdGuard / Tailscale notes: `adguard.md`, `tailscale.md`. Not the same sitting as the S33 swap.
