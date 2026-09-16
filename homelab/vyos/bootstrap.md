# VyOS bootstrap (exceptional)

Use this only if Cloud-Init did not yield SSH, or Ansible cannot be used. Desired config remains `config.boot.j2`. Normal apply: `../ansible/vyos/01-config.yml`. Create: `../ansible/proxmox/02-vyos-vm.yml`.

Cloud-Init is bootstrap only: `hw-id`, `eth0.10`, SSH key. Git does not ship the qcow2. MACs are pinned on create (`02:00:00:00:00:00`–`:02`): `eth0` LAN / `eth1` WAN / `eth2` services.

After create, pin the new host key on the Mac (`ssh-keygen -R 10.10.10.1` then `ssh-keyscan`), then apply `config.boot.j2`. See `../ansible/README.md`.

ISO `2026.09.09-0029-rolling` is a console fallback image, not the create-path qcow2.

Laptop: static `10.10.10.99/24` on VLAN 10.

```
configure
set interfaces ethernet eth0 hw-id '02:00:00:00:00:00'
set interfaces ethernet eth1 hw-id '02:00:00:00:00:01'
set interfaces ethernet eth2 hw-id '02:00:00:00:00:02'
set interfaces ethernet eth0 vif 10 address '10.10.10.1/24'
set interfaces ethernet eth0 vif 10 description 'VLAN 10 trusted'
set service ssh
set service ssh disable-host-validation
set system login user vyos authentication public-keys macbook type ssh-ed25519
set system login user vyos authentication public-keys macbook key 'AAAAC3NzaC1lZDI1NTE5AAAAIMnN6+7q3OAtjH3lEeRBW7mz/qCwnt8fCCOtKi1+/80Z'
commit
save
exit
```

Then `ansible-playbook vyos/01-config.yml` from `../ansible/`.

If Ansible cannot be used: copy a rendered `config.boot` to the router, `configure`, `load /path`, `compare`, `commit-confirm 2`, verify SSH, `confirm`, `save`. Render on the Mac with `ansible-playbook vyos/01-config.yml --check` (writes `/tmp/homelab-vyos-config.boot`).
