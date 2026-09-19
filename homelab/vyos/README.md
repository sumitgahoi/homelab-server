# VyOS config

`config.boot` is the complete desired router configuration. Git tracks it.

1. Edit `config.boot`.
2. Commit it to Git.
3. Copy it to the VyOS VM.
4. Apply it.
5. Save, or reboot if you replaced `/config/config.boot`.

```bash
scp config.boot vyos@HOST:/tmp/config.boot
ssh vyos@HOST
```

On the VM:

```
configure
load /tmp/config.boot
compare
commit
save
```

`load` replaces the candidate configuration with this file. Nodes omitted from `config.boot` are removed on commit.

From the MacBook on CBS350 GE12 (`10.10.10.99/24`, no gateway) this house router is `vyos@10.10.10.1`. Fresh VM 100: `install.md`.
