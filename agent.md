# Agent context

**Ask first. Do not apply changes until the owner agrees.**

Repo edits, live gear (Proxmox, VyOS, CBS350, UniFi, ASUS, S33), and extras. Status dumps and Q&A are not permission to write.

## IaC philosophy

    Git = desired state + history
    Human = operator (apply, admin, troubleshooting, inspection, lifecycle)

This is **one Proxmox host**, not a fleet.

Manual work with SSH, `qm`, VyOS CLI, `ifreload`, `ip`, `bridge`, the Proxmox UI, and BMC/KVM is expected. It is not configuration-management failure.

- Experimental / temporary change → manual is fine
- Permanent desired-state change → eventually represent it in Git
- Authoritative apply → Git wins

Do not automate something merely because it can be automated. A short documented manual command is better than apply machinery that adds little value.

Prefer literal configuration, native tools, and readable recovery procedures.

Do not create variables, templates, or abstractions merely to eliminate a few literal lines.

Understand the infrastructure by reading the configuration files.

`.superpowers/` is session scratch, not documentation.

## Before changing network infrastructure

Before changing Proxmox networking, VyOS, VM 100, VLANs, firewall, routing, DHCP, DNS, NAT, Tailscale, switch topology, or related infrastructure:

1. Read this file.
2. Read `homelab/REQUIREMENTS.md`.
3. Read the relevant authoritative config (`homelab/proxmox/interfaces`, `homelab/vyos/config.boot`, `homelab/cbs350/running-config`).
4. Determine CURRENT vs PLANNED vs DEFERRED.
5. Check whether the requested change conflicts with a requirement or invariant.
6. If it conflicts, **stop** and tell the owner before implementing it.
7. If the owner intentionally changes a requirement, update `REQUIREMENTS.md` in the same change.
8. Keep implementation as simple as possible.
9. Do not add Ansible or other apply automation without a concrete reason.

A code/config change that alters network behavior without updating the corresponding requirement is incomplete.

## Docs

One fact, one file. Distinguish **CURRENT / as-built**, **Git desired state**, **PLANNED**, and **DEFERRED**. Do not mix them.

| Fact | Authority |
|------|-----------|
| Required behavior and invariants | `homelab/REQUIREMENTS.md` |
| Current topology / wiring | `homelab/README.md` |
| Proxmox host interfaces | `homelab/proxmox/interfaces` |
| Router configuration | `homelab/vyos/config.boot` |
| Switch configuration | `homelab/cbs350/running-config` |
| How to apply | `homelab/proxmox/README.md` (host interfaces); `homelab/vyos/README.md` (VyOS); `homelab/cbs350/README.md` (switch); `homelab/ssh/README.md` (operator key) |
| UniFi as-built | `homelab/unifi.md` |
| Physical catalog | `inventory.md` |
| Guest list | `homelab/proxmox.md` |

`config.boot` is Git desired state. It has **not** yet been authoritatively applied and verified on the live router.

## As-built (live)

Proxmox mgmt is `192.168.50.200` on `vmbr1` (ASUS LAN) until cutover. Then apply Git `proxmox/interfaces` (`vmbr0.10` = `10.10.10.3`). WAN is still house ASUS (double NAT). UniFi OS Server and U6+ are live. AdGuard, Tailscale, and India-GW are not deployed. Git `config.boot` has not yet been authoritatively applied.

## Layout

See `README.md`.
