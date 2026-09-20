# VyOS

This directory is a **runbook plus a known-good snapshot**. It is not an apply engine.

```text
setup.md / install.md / verify.md
        = how a human creates, rebuilds, and checks the router

commands.txt
        = known-good configuration exported from the live router
```

Git records both. A human applies `set` / `delete` commands, verifies, then updates Git.

Do not introduce Ansible, Terraform, generators, or a script that bulk-applies `commands.txt`.

## Files

| File | Role |
|------|------|
| `commands.txt` | Authoritative known-good VyOS configuration. Produced on the live router with `show configuration commands`. The `encrypted-password` line is deliberately omitted. The SSH public key may remain. |
| `setup.md` | Manual rebuild and day-2 change procedure. Apply commands logically / section-by-section. |
| `install.md` | Create and install VM 100. Then continue in `setup.md`. |
| `verify.md` | Checks after a fresh rebuild or a significant change. |

There is no hand-maintained `config.boot` in this repository.

Hand-editing `config.boot` and `load`ing it caused unexpected round-trip behavior for some VyOS firewall state. VyOS-generated `set` commands are the preferred human-readable representation. Do not add `config.boot` back as a second source of truth.

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
    regenerate `commands.txt` from the known-good running config
       ↓
    review the Git diff
       ↓
    commit

60 minutes for `commit-confirm` is intentional: testing can involve multiple networks and systems.

From the MacBook on CBS350 GE12 (`10.10.10.99/24`, no gateway) this house router is `vyos@10.10.10.1`.
