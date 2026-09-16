# Ansible runbook

Run from this directory.

    Git = desired state
    Ansible = simple apply / disaster recovery
    You = operator

Ansible does not continuously manage this single host. It does not replace SSH, `qm`, VyOS CLI, `ifreload`, or BMC.

Behavior and invariants: `../REQUIREMENTS.md`. Topology: `../README.md`.

| Unit | Authoritative file | Apply | Ansible does |
|------|--------------------|-------|----------------|
| Proxmox host network | `proxmox/files/interfaces` | `ansible-playbook proxmox/01-network.yml` | install file after syntax check |
| VM 100 | `proxmox/02-vyos-vm.yml` | `ansible-playbook proxmox/02-vyos-vm.yml` | create only; fail if VM exists |
| VyOS router | `../vyos/config.boot.j2` | `ansible-playbook vyos/01-config.yml` | authoritative replace + commit-confirm |

```bash
brew install ansible
python3 -m pip install paramiko
ansible-galaxy collection install -r requirements.yml
```

Ansible-core ≥ 2.17. Pin `pve` once: `ssh-keyscan -H 10.10.10.3 >> ~/.ssh/known_hosts`.

Qcow2 is not in Git. Put `local:import/vyos-proxmox-amd64.qcow2` on Proxmox before first create. SSH to `pve` as root is enough; there is no Proxmox API token.

`config.boot.j2` has not yet been authoritatively applied and verified on the live router.

---

## Proxmox interfaces

Authoritative: `proxmox/files/interfaces`.

```bash
ansible-playbook proxmox/01-network.yml --check --diff
ansible-playbook proxmox/01-network.yml
```

That writes `/etc/network/interfaces` after a syntax check. It does **not** activate live networking, verify VLANs, take backups, or recover a broken host.

Then on Proxmox:

```bash
ifreload -a
ip -br addr
ip -br link
bridge vlan show
```

If `ifreload` strands SSH: BMC iKVM.

Git refuses to apply if `/etc/network/interfaces.d` has leftover files.

---

## VM 100

Create-only, via `qm` over SSH. If VM 100 already exists, the playbook fails and does not modify it. If it is absent, Git creates it (qcow2 import, locked NICs/MACs, Cloud-Init bootstrap) and runs `qm start 100`. It does not wait for SSH.

It does not reconcile, inspect drift, repair partial state, or start/stop/destroy an existing VM. Inspect or destroy manually (`qm config 100`, `qm destroy 100` if you mean it).

```bash
ansible-playbook proxmox/02-vyos-vm.yml
```

After create, confirm SSH on `10.10.10.1`, pin the new host key, then apply router config:

```bash
ssh-keygen -R 10.10.10.1
ssh-keyscan -H 10.10.10.1 >> ~/.ssh/known_hosts
ssh vyos@10.10.10.1
ansible-playbook vyos/01-config.yml
```

Cloud-Init is bootstrap only (`hw-id`, `eth0.10`, SSH). Policy is `config.boot.j2`. Console fallback: `../vyos/bootstrap.md`.

---

## VyOS config

Authoritative: `../vyos/config.boot.j2`. Omitted nodes are deleted on apply (`replace: config`). Ansible does not re-assert VLANs, firewall, DHCP, DNS, or NAT as separate checks.

```bash
ansible-playbook vyos/01-config.yml --check --diff
ansible-playbook vyos/01-config.yml
```

Live apply: load → `commit-confirm` (2 minutes) → wait until `10.10.10.1:22` answers → `confirm` → `save`. If SSH does not come back, do not confirm; after ~2 minutes VyOS reloads the last saved config. BMC / port 12 if needed.

`--check --diff` renders to `/tmp/homelab-vyos-config.boot` and compares; it does not commit.

Second identical apply is a no-op (no confirm/save).

Permanent policy changes go in `config.boot.j2`, then this playbook. Experimental `set` on the box is fine until you decide it is permanent.
