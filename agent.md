# Agent context

**Ask first. Do not apply changes until the owner agrees.**

Repo edits, live gear (Proxmox, VyOS, CBS350, UniFi, ASUS, S33), and extras. Status dumps and Q&A are not permission to write.

Do not SSH into anything or execute commands against live infrastructure unless the owner explicitly asks for that in the current request.

## How this repository is used

This is a **human-executable, Git-versioned runbook**. It is not an automation system.

```text
                  Git repository
                        │
             ┌──────────┴──────────┐
             │                     │
         setup.md            known-good config
       HOW / WHY             WHAT EXISTS
             │                     │
             └──────────┬──────────┘
                        │
                      human
                        │
                 manually applies
                        │
                     verifies
                        │
                  working system
```

    Git = known-good state + the procedure used to construct it
    Human = the deployment mechanism

The intended loop is:

    understand
       ↓
    run one small/focused command or logical group
       ↓
    verify what happened
       ↓
    continue

If something must be rebuilt a year from now, follow the documentation step-by-step and understand each command. Do not reconstruct the homelab with Ansible, Terraform, generators, deployment scripts, CI/CD, or automated reconciliation.

This is **one Proxmox host**, not a fleet.

Manual work with SSH, `qm`, `pct`, VyOS CLI, `ifreload`, `ip`, `bridge`, the Proxmox UI, and BMC/KVM is expected. Simple manual commands are intentional.

Do not:

- introduce Ansible, Terraform, configuration generators, or apply machinery
- turn ordered commands into large shell scripts merely for convenience
- bulk-apply `vyos/commands.txt` to a running router
- treat `config.boot` as something to hand-maintain or `load`

Prefer literal configuration, native tools, and readable recovery procedures.

Do not create variables, templates, or abstractions merely to eliminate a few literal lines.

Understand the infrastructure by reading the runbooks and the known-good snapshots.

## Before changing network infrastructure

Before changing Proxmox networking, VyOS, VM 100, VLANs, firewall, routing, DHCP, DNS, NAT, Tailscale, WireGuard, switch topology, or related infrastructure:

1. Read this file.
2. Read `homelab/REQUIREMENTS.md`.
3. Read the relevant known-good snapshot and the matching runbook (table below).
4. Determine as-built vs deferred (`homelab/README.md`, Still ahead). A PLANNED design such as Beryl stays in its own file.
5. Check whether the requested change conflicts with a requirement or invariant.
6. If it conflicts, **stop** and tell the owner before implementing it.
7. If the owner intentionally changes a requirement, update `REQUIREMENTS.md` in the same change.
8. Keep implementation as simple as possible.
9. Do not add Ansible, Terraform, or other apply automation.

A documentation or snapshot change that alters described network behavior without updating the corresponding requirement is incomplete.

## Docs

One fact, one file. The live network is as-built or deferred. PLANNED designs stay in their own files. Do not mix them.

| Fact | Authority |
|------|-----------|
| Required behavior and invariants | `homelab/REQUIREMENTS.md` |
| Current topology / wiring; deferred work | `homelab/README.md` (deferred: Still ahead) |
| Proxmox host interfaces (known-good) | `homelab/proxmox/interfaces` (CURRENT). Archive: `interfaces.until-cutover` |
| How to copy host interfaces | `homelab/proxmox/README.md` |
| VyOS known-good CLI | `homelab/vyos/commands.txt` |
| VyOS tree form (read only) | `homelab/vyos/config.boot` (do not `load`) |
| How to rebuild / change VyOS | `homelab/vyos/setup.md` (IPv4 + WAN IPv6) |
| VyOS checks | `homelab/vyos/verify.md` |
| Cloudflare DDNS | `homelab/vyos/ddns.md` |
| How to create VM 100 | `homelab/vyos/install.md` |
| Switch configuration (known-good) | `homelab/cbs350/running-config` |
| How to restore the switch | `homelab/cbs350/README.md` |
| Operator SSH key | `homelab/ssh/README.md` |
| UniFi as-built | `homelab/unifi.md` |
| India-GW (DEPRECATED; deprecated alternative) | `homelab/tailscale-india/setup.md` |
| India-GW design notes (DEPRECATED) | `homelab/tailscale-india/README.md` |
| WireGuard rebuild (`wg0`, `wg1`, `wg2`) | `homelab/wireguard/wg0.md`, `wg1.md`, `wg2.md` |
| WireGuard design notes | `homelab/wireguard/README.md` |
| US Tailscale (will not deploy) | `homelab/tailscale-us/README.md` |
| AdGuard rebuild | `homelab/adguard/setup.md` |
| AdGuard design notes | `homelab/adguard/README.md` |
| Dev VM rebuild | `homelab/dev/setup.md` |
| Dev VM design notes | `homelab/dev/README.md` |
| Beryl 7 travel router (PLANNED) | `homelab/beryl.md` |
| Physical catalog | `inventory.md` |
| Guest list | `homelab/proxmox.md` |

`commands.txt` is the known-good VyOS configuration exported from the live router (`show configuration commands`, password hash removed). A human applies `set` / `delete` commands. It is not a bulk-apply script. `config.boot` is the same facts for reading. Do not `load` it.

## As-built (live)

Wiring, addresses, and deferred work: `homelab/README.md`.

## Layout

See `README.md`.
