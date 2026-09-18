# Host interfaces

Git tracks `interfaces` (after VyOS is the router) and `interfaces.until-cutover`. Copy from this directory on the MacBook. Do not use the Proxmox Network UI Apply button.

Until cutover:

```bash
scp interfaces.until-cutover root@192.168.50.200:/etc/network/interfaces
```

After VyOS is the default router:

```bash
scp interfaces root@192.168.50.200:/etc/network/interfaces
```

On the host, `/etc/network/interfaces.d` must not have competing files. Then:

```bash
ifreload -a --syntax-check
```

```bash
ifreload -a
```

After cutover the host should move to `10.10.10.3` on `vmbr0.10`. If `ifreload` strands SSH: BMC iKVM. Disks, guests, recovery: `../proxmox.md`.
