# Operator SSH key

`macbook.pub` is the public half of the MacBook's default SSH key. That key logs into Proxmox and Linux guests. VyOS has this key in `../vyos/commands.txt` (`disable-password-authentication`).

If SSH breaks on Proxmox, BMC iKVM.

From the Mac, on Trusted. `ssh-copy-id` installs the default key:

```bash
ssh-copy-id root@10.10.10.3
```

```bash
ssh-copy-id USER@HOST
```

Then, from this directory, copy `disable-passwords.conf` (Proxmox or a Debian/Ubuntu guest):

```bash
scp disable-passwords.conf root@10.10.10.3:/etc/ssh/sshd_config.d/disable-passwords.conf
```

```bash
scp disable-passwords.conf USER@HOST:/tmp/disable-passwords.conf
```

On the guest, as root:

```bash
cp /tmp/disable-passwords.conf /etc/ssh/sshd_config.d/disable-passwords.conf
```

```bash
systemctl reload ssh
```
