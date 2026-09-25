# Dev VM — CURRENT

VM 101 at `10.10.10.10`. Rebuild: `setup.md`. Invariants: `../REQUIREMENTS.md`. Topology: `../README.md`.

| Node | Where | IP | Role | State |
|------|-------|----|------|--------|
| `dev` | VM 101 on `vmbr0` tag 10 | `10.10.10.10` | General-purpose Debian workstation | CURRENT |

```text
Mac (Cursor or VS Code Remote SSH)
    → sumit@10.10.10.10
    → Debian 13 VM
         ├── Docker
         ├── Git
         └── ~/code/<project>/.devcontainer/
```

## Why this shape

The VM is a Trusted client, same as the Mac. `vmbr0` is the VLAN-aware LAN trunk. Proxmox `tag=10` puts the guest in VLAN 10, so the guest OS sees a normal untagged NIC. Trusted may use the Internet, Services, and VyOS/Proxmox management. Guest, IoT, and India still cannot open connections to it. No VyOS, switch, or host-bridge change.

Services (`vmbr-svc`) is the wrong place. That network must not initiate connections into Trusted.

`10.10.10.10` is outside the DHCP pool (`.100`–`.250`) and avoids `.1` VyOS, `.2` CBS350, `.3` Proxmox, `.4` (deferred NAS), and `.99` (admin OOB). The address is static so SSH does not follow a lease. QEMU picks the MAC. Do not use `02:00:00:00:00:00`–`:02` (VyOS).

## Size

| | |
|---|---|
| vCPU | 4, `--cpu host`, one socket |
| RAM | 16384 MiB, balloon off |
| Disk | 100 GB virtio on `local-lvm` (SN770) |
| Boot | start with the host |
| Guest agent | enabled |

The host has 32 GB. VM 100 holds 4 GB (balloon off). The LXCs can use another 5 GB. This VM holds 16 GB. About 7 GB remains for Proxmox when every guest is at its cap. Four vCPUs is enough for one active devcontainer. `qm set 101 --cores 6` raises it later without moving the VM.

## Accounts and SSH

Daily login is `sumit`, with sudo. The Mac's default SSH key is the operator key; `../ssh/macbook.pub` is its public half. `../ssh/disable-passwords.conf` turns off SSH passwords. The console password remains, for the Proxmox console if SSH breaks.

Cursor and VS Code both use that SSH login. Each installs its own remote component on first connect:

| Client | On the VM |
|--------|-----------|
| VS Code | `~/.vscode-server` |
| Cursor | `~/.cursor-server` |

Do not copy one directory onto the other. Do not install code-server. Use one editor at a time on a given project.

## What gets installed

On the VM: Git, Docker Engine (so Dev Containers can use the Docker socket), `qemu-guest-agent`, and `curl`, `ca-certificates`, `htop`, `tmux`, `ripgrep`, `jq`, `unzip`.

Language toolchains stay in the project. Each project under `~/code/` that needs one owns `~/code/<project>/.devcontainer/`. This runbook does not create a project.

The guest is IPv4 only, like the other VLAN clients. DNS is `10.10.10.1` (VyOS, which forwards to AdGuard). Do not point the VM at `10.10.0.4`. No Tailscale on this VM. Away from home, reach it through Trusted WireGuard (`wg0`, live).

AdGuard rewrite `dev.home.arpa` → `10.10.10.10`. The Mac's SSH alias uses that name (`setup.md`).
