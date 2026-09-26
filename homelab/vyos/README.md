# VyOS

This directory is a **runbook plus a known-good snapshot**. It is not an apply engine.

```text
setup.md / install.md / verify.md
        = how a human creates, rebuilds, and checks the router

commands.txt
        = known-good configuration (apply this)

config.boot
        = same config, tree form (read this; do not load)
```

Git records both. A human applies `set` / `delete` from `commands.txt`, verifies, then updates Git. Do not bulk-apply `commands.txt`.

## Files

| File | Role |
|------|------|
| `commands.txt` | Authoritative known-good. `show configuration commands`. Password line omitted. SSH public key may remain. Rebuild from this. |
| `config.boot` | Same facts, indented. Easier to read. Password is `redacted`. **Do not `load`.** Export when you export `commands.txt`. Do not hand-edit one file and not the other. |
| `setup.md` | Manual rebuild and day-2 change procedure. IPv4 plus WAN IPv6 on `eth1` only. |
| `install.md` | Create and install VM 100. Then continue in `setup.md`. |
| `verify.md` | Checks after a fresh rebuild or a significant change. |

`load`ing `config.boot` produced unexpected firewall round-trip behavior. That is why apply stays `set` / `delete` from `commands.txt`.

## Rebuild

    create VM 100 (`install.md`)
       ↓
    configure
       ↓
    apply commands from `commands.txt` logically / section-by-section (`setup.md`)
       ↓
    compare
       ↓
    continue
       ↓
    commit-confirm 60
       ↓
    perform network tests
       ↓
    confirm
       ↓
    save

Do **not** blindly paste the entire `commands.txt` into a running router.

## Normal change on an existing router

    understand desired change
       ↓
    manually issue the required set/delete commands
       ↓
    compare
       ↓
    commit-confirm 60
       ↓
    test
       ↓
    confirm
       ↓
    save
       ↓
    regenerate `commands.txt` and `config.boot` from the known-good running config
       ↓
    review the Git diff
       ↓
    commit

60 minutes for `commit-confirm` is intentional: testing can involve multiple networks and systems.

From the MacBook on CBS350 GE12 (`10.10.10.99/24`, no gateway) this house router is `vyos@10.10.10.1`.
