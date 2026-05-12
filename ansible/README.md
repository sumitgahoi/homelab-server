# Ansible — VyOS on Proxmox (Phase 1)

Ansible creates a **VyOS VM** on **Proxmox VE** (virtio NICs on **`vmbr-wan`** / **`vmbr-lan`**, **2 vCPU**, **1536 MiB RAM**, **16 GB** disk by default) and can push **Phase 1** config: **WAN DHCP**, **static LAN**, **DHCP server**, **Cloudflare** or **OpenDNS** resolvers for clients — matching **`docs/network-and-services.md` § Phase 1**.

## Prerequisites

1. **Proxmox** has Linux bridges **`vmbr-wan`** and **`vmbr-lan`** with the correct physical ports (modem / LAN switch).
2. **Ansible** ≥ 2.14 on your control machine (`pip install ansible` or OS package).
3. **API user** on Proxmox with rights to create/start VMs (e.g. `root@pam` or a token-capable role).
4. **VyOS** must be **installed** in the guest:
   - **`vyos_provision_mode: create`**: first run creates an **empty** disk; **attach a VyOS ISO** in the Proxmox UI, install from **console**, reboot, then run the **configure** play.
   - **`vyos_provision_mode: clone`**: set **`vyos_clone_vmid`** to a **template VMID** that already has VyOS installed; the play **full-clones** it to **`vyos_vmid`**.

## Install collections

```bash
cd ansible
ansible-galaxy collection install -r requirements.yml
```

## Configure variables

1. Copy the example inventory:

   ```bash
   cp -r inventories/example inventories/production
   ```

2. Edit **`inventories/production/group_vars/all.yml`**: `proxmox_api_host`, `proxmox_node`, bridges, `vyos_vmid`, disks, LAN/DHCP/DNS.

3. Put secrets in **vault** (recommended):

   ```bash
   ansible-vault create inventories/production/group_vars/secrets.yml
   ```

   Example contents:

   ```yaml
   proxmox_api_password: "your-pve-password"
   vyos_ssh_password: "your-vyos-password"
   ```

   Load them from **`inventories/production/group_vars/all.yml`** with:

   ```yaml
   vars_files:
     - secrets.yml
   ```

   Or pass **`-e proxmox_api_password=...`** (avoid shell history).

4. Set **`ansible_host`** for host **`vyos`** in **`hosts.yml`** to an address your control node can use to reach VyOS **SSH** (usually the **LAN IP** after install, e.g. `192.168.1.1`). Uncomment **`ansible_password`** in **`group_vars/vyos_gateway.yml`** if you use password auth (or use SSH keys).

## Run

From the **`ansible/`** directory, using **`inventories/production`**:

```bash
export ANSIBLE_INVENTORY=inventories/production/hosts.yml
# or: ansible-playbook -i inventories/production/hosts.yml ...
```

**Option A — two playbooks**

```bash
ansible-playbook -i inventories/production/hosts.yml playbooks/provision_vyos_vm.yml
# Install VyOS from ISO (create mode), then:
ansible-playbook -i inventories/production/hosts.yml playbooks/configure_vyos_phase1.yml
```

**Option B — one playbook with tags**

```bash
ansible-playbook -i inventories/production/hosts.yml playbooks/site.yml --tags provision
ansible-playbook -i inventories/production/hosts.yml playbooks/site.yml --tags configure
ansible-playbook -i inventories/production/hosts.yml playbooks/site.yml
```

With vault:

```bash
ansible-playbook -i inventories/production/hosts.yml playbooks/site.yml --ask-vault-pass
```

## VyOS CLI flavor

The generated **`set`** commands target **VyOS 1.4+** DHCP/NAT syntax. If you run an older release, adjust **`roles/vyos_phase1/templates/phase1_set_commands.j2`** (see [VyOS documentation](https://docs.vyos.io/)).

## Security note

This role configures **NAT + DHCP + DNS forwarding** only. **Tighten firewall** before exposing services. Review **`docs/network-and-services.md`** as you move past Phase 1.
