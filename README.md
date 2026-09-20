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
| `homelab/proxmox/interfaces` | Known-good Proxmox `/etc/network/interfaces` after VyOS is the router |
| `homelab/proxmox/interfaces.until-cutover` | Known-good host interfaces until cutover (`192.168.50.200` on `vmbr1` only) |
| `homelab/proxmox/README.md` | How a human copies host interfaces and runs `ifreload` |
| `homelab/proxmox.md` | Host disks, guests, recovery |
| `homelab/vyos/commands.txt` | Known-good VyOS configuration (`show configuration commands`) |
| `homelab/vyos/setup.md` | How a human rebuilds or changes VyOS, section by section |
| `homelab/vyos/install.md` | How a human creates VM 100 |
| `homelab/vyos/verify.md` | Checks after install or a significant VyOS change |
| `homelab/ssh/` | Operator key and sshd drop-in; copy steps in `homelab/ssh/README.md` |
| `homelab/cbs350/running-config` | Known-good switch running-config |
| `homelab/cbs350/README.md` | How a human restores `running-config` onto the switch |
| `homelab/unifi.md` | UniFi OS Server and U6+ (as-built) |
| `homelab/tailscale-india/README.md` | India-GW design and fail-closed notes |
| `homelab/tailscale-india/setup.md` | How a human rebuilds India-GW (CT 108) |
| `homelab/tailscale-us.md` | US Tailscale node (PLANNED) |
| `homelab/adguard/README.md` | AdGuard design (PLANNED) |
| `homelab/adguard/setup.md` | How a human creates or rebuilds AdGuard (CT 110) and points VyOS at it |
| `home-theatre/` | Signal path, channels |
| `rack/` | U-layout, power |

**As-built:** Proxmox `192.168.50.200` on `vmbr1` (ASUS LAN) until cutover to `10.10.10.3` on `vmbr0.10`. WAN is still the house ASUS (double NAT). UniFi OS Server and U6+ are live (`homelab/unifi.md`). India-GW is live (`homelab/tailscale-india/setup.md`). VyOS known-good is `homelab/vyos/commands.txt`. AdGuard and `tailscale-us` are not deployed.

Do not introduce Ansible, Terraform, generators, deployment scripts, or CI/CD for this homelab. Copy host interfaces from `homelab/proxmox/README.md`. Rebuild or change VyOS from `homelab/vyos/setup.md`. Operator SSH key: `homelab/ssh/README.md`.
