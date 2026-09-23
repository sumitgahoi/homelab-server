# Operator SSH key

`macbook.pub` is the MacBook key used to log into Proxmox and Linux guests. VyOS has this key in `../vyos/commands.txt` (`disable-password-authentication`).

If SSH breaks on Proxmox, BMC iKVM.

From this directory:

```bash
ssh-copy-id -i macbook.pub root@10.10.10.3
```

```bash
ssh-copy-id -i macbook.pub USER@HOST
```

Then copy `disable-passwords.conf` (Proxmox or a Debian/Ubuntu guest):

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
