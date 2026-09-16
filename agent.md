# Agent context

**Ask first. Do not apply changes until the owner agrees.**

Repo edits, live gear (Proxmox, VyOS, CBS350, UniFi, ASUS, S33), and extras. Status dumps and Q&A are not permission to write.

## IaC philosophy

    Git = desired state + history
    Ansible = a simple apply / disaster-recovery mechanism
    Human = operator (admin, troubleshooting, activation, inspection, lifecycle)

This is **one Proxmox host**, not a fleet.

Manual work with SSH, `qm`, VyOS CLI, `ifreload`, `ip`, `bridge`, the Proxmox UI, and BMC/KVM is expected. It is not configuration-management failure.

- Experimental / temporary change → manual is fine
- Permanent desired-state change → eventually represent it in Git
- Authoritative apply → Git wins

Do not automate something merely because it can be automated. A short documented manual command is better than Ansible machinery that adds little DR or safety value.

## Ansible simplicity

For every proposed Ansible task, variable, assertion, role, helper, template, abstraction, backup, verification step, or lifecycle feature, ask whether it materially contributes to:

1. expressing authoritative desired state,
2. applying authoritative desired state,
3. preventing a realistic catastrophic mistake, or
4. disaster recovery.

If not, prefer not adding it.

Do not optimize for fleet patterns, maximum idempotency, maximum automation, abstraction for its own sake, generic reusable roles, exhaustive verification, duplicated policy assertions, or Ansible “best practices” that make this one-host repo harder to read.

Prefer literal configuration, obvious playbooks, native tools, small amounts of glue, and readable recovery procedures.

Do not create variables merely because a value might change. Do not create loops, dictionaries, or templates merely to eliminate a few literal lines.

Understand the infrastructure by reading the configuration files, not Ansible internals.

Current Ansible contracts (do not expand without a concrete reason):

- `homelab/ansible/proxmox/01-network.yml` — install Git’s interfaces file after a syntax check. Does not activate, verify, or recover networking. Operator: `ifreload -a` and inspect; BMC if stranded.
- `homelab/ansible/proxmox/02-vyos-vm.yml` — **create-only**. If VM 100 exists, fail and touch nothing. No reconcile, repair, start/stop/destroy/rebuild. Prefer native `qm` over the Proxmox API.
- `homelab/ansible/vyos/01-config.yml` — Architecture A: render `config.boot.j2`, authoritative replace, `commit-confirm` (2 minutes), test SSH to `10.10.10.1:22`, then confirm and save. Do not duplicate router policy as Ansible assertions.

`.superpowers/` is session scratch, not documentation.

## Before changing network infrastructure

Before changing Proxmox networking, VyOS, VM 100, VLANs, firewall, routing, DHCP, DNS, NAT, Tailscale, switch topology, or related infrastructure:

1. Read this file.
2. Read `homelab/REQUIREMENTS.md`.
3. Read the relevant authoritative config (`homelab/ansible/proxmox/files/interfaces`, `homelab/vyos/config.boot.j2`, and/or the create-only VM playbook).
4. Determine CURRENT vs PLANNED vs DEFERRED.
5. Check whether the requested change conflicts with a requirement or invariant.
6. If it conflicts, **stop** and tell the owner before implementing it.
7. If the owner intentionally changes a requirement, update `REQUIREMENTS.md` in the same change.
8. Keep implementation as simple as possible.
9. Do not expand Ansible’s responsibilities without a concrete reason.

A code/config change that alters network behavior without updating the corresponding requirement is incomplete.

## Docs

One fact, one file. Distinguish **CURRENT / as-built**, **Git desired state**, **PLANNED**, and **DEFERRED**. Do not mix them.

| Fact | Authority |
|------|-----------|
| Required behavior and invariants | `homelab/REQUIREMENTS.md` |
| Current topology / wiring | `homelab/README.md` |
| Proxmox host interfaces | `homelab/ansible/proxmox/files/interfaces` |
| Router configuration | `homelab/vyos/config.boot.j2` |
| How to apply | `homelab/ansible/README.md` |
| Physical catalog | `inventory.md` |
| Guest list | `homelab/proxmox.md` |

`config.boot.j2` is Git desired state. It has **not** yet been authoritatively applied and verified on the live router.

## As-built (live)

Proxmox + VyOS + CBS350. WAN is still house ASUS (double NAT). AdGuard, Tailscale, and India-GW are not deployed. Apply Git VyOS config from VLAN 10 when the owner asks.

## Layout

See `README.md`.
