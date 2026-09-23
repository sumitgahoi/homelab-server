# House

Source of truth for networking, home theatre, and future gadgets.

This repository is a **human-executable runbook**, not an automation system. Git records the known-good state and the procedure used to construct it. A human applies it, then verifies.

One fact, one file. Label **CURRENT / as-built**, **PLANNED**, and **DEFERRED**. Do not mix them.

| Path | What |
|------|------|
| `agent.md` | How coding agents must work (runbook philosophy + pre-change procedure) |
| `CLAUDE.md` | Claude Code entry point → `agent.md` |
| `inventory.md` | Physical hardware catalog |
| `homelab/REQUIREMENTS.md` | Required network behavior and invariants |
| `homelab/README.md` | Current topology and wiring |
| `homelab/proxmox/interfaces` | Known-good Proxmox `/etc/network/interfaces` (CURRENT: `10.10.10.3` on `vmbr0.10`) |
| `homelab/proxmox/interfaces.until-cutover` | Pre-cutover host snapshot (`192.168.50.200` on `vmbr1`; archive) |
| `homelab/proxmox/README.md` | How a human copies host interfaces and runs `ifreload` |
| `homelab/proxmox.md` | Host disks, guests, recovery |
| `homelab/vyos/commands.txt` | Known-good VyOS configuration (`show configuration commands`) |
| `homelab/vyos/config.boot` | Same config, tree form (read only; do not `load`) |
| `homelab/vyos/setup.md` | How a human rebuilds or changes VyOS (IPv4 + WAN IPv6) |
| `homelab/vyos/install.md` | How a human creates VM 100 |
| `homelab/vyos/verify.md` | Checks after install or a significant VyOS change |
| `homelab/ssh/` | Operator key and sshd drop-in; copy steps in `homelab/ssh/README.md` |
| `homelab/cbs350/running-config` | Known-good switch running-config |
| `homelab/cbs350/README.md` | How a human restores `running-config` onto the switch |
| `homelab/unifi.md` | UniFi OS Server and U6+ (as-built) |
| `homelab/tailscale-india/README.md` | India-GW design and fail-closed notes |
| `homelab/tailscale-india/setup.md` | How a human rebuilds India-GW (CT 108) |
| `homelab/wireguard.md` | Pointer to the WireGuard runbook |
| `homelab/wireguard/README.md` | WireGuard design: wg0/wg1, wg-india (PLANNED) |
| `homelab/wireguard/setup.md` | How a human applies those parts |
| `homelab/tailscale-us/README.md` | US Tailscale guest — will not deploy |
| `homelab/adguard/README.md` | AdGuard design (CURRENT) |
| `homelab/adguard/setup.md` | How a human creates or rebuilds AdGuard (CT 110) and points VyOS at it |
| `homelab/beryl.md` | Beryl 7 travel router design (PLANNED). Not deployed |
| `home-theatre/` | Signal path, channels |
| `rack/` | U-layout, power |

**As-built:** WAN cutover is complete. Proxmox `10.10.10.3` on `vmbr0.10`. S33 on `nic1`; house ASUS retired. UniFi OS Server and U6+ are live (`homelab/unifi.md`). India-GW is live (`homelab/tailscale-india/setup.md`). AdGuard is live (`homelab/adguard/`). VyOS is IPv6-capable on `eth1` only (LAN/Services IPv4). Known-good CLI is `homelab/vyos/commands.txt`. WireGuard on VyOS is PLANNED (`homelab/wireguard/`). Do not deploy `tailscale-us`.

Do not introduce Ansible, Terraform, generators, deployment scripts, or CI/CD for this homelab. Copy host interfaces from `homelab/proxmox/README.md`. Rebuild or change VyOS from `homelab/vyos/setup.md`. Operator SSH key: `homelab/ssh/README.md`.
