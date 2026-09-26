# Host interfaces

These files are **known-good snapshots** of `/etc/network/interfaces` on the Proxmox host.

A human copies the matching file and runs `ifreload`. Do not use the Proxmox Network UI Apply button.

Git tracks `interfaces` (CURRENT) and `interfaces.until-cutover` (pre-cutover archive). Copy from this directory on the MacBook.

CURRENT:

```bash
scp interfaces root@10.10.10.3:/etc/network/interfaces
```

Archive (do not use unless reverting the host to the pre-cutover ASUS LAN):

```bash
scp interfaces.until-cutover root@192.168.50.200:/etc/network/interfaces
```

On the host, `/etc/network/interfaces.d` must not have competing files. Then:

```bash
ifreload -a --syntax-check
```

```bash
ifreload -a
```

The host is `10.10.10.3` on `vmbr0.10`. If `ifreload` strands SSH: BMC iKVM. Disks, guests, recovery: `../proxmox.md`.
