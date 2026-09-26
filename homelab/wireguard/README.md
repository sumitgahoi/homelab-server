# WireGuard

Live on VyOS. Behavior: `../REQUIREMENTS.md`. Known-good CLI: `../vyos/commands.txt` (`private-key` is `redacted`).

| Interface | Where | Prefix | Listen | Role |
|-----------|--------|--------|--------|------|
| `wg0` | VyOS | `10.10.80.0/24` | UDP `51820` | Trusted road warrior. `wg0.md` |
| `wg1` | VyOS | `10.10.81.0/24` | UDP `51821` | Guest road warrior. `wg1.md` |
| `wg2` | VyOS | `10.10.82.0/30` | UDP `51822` | VLAN 40 → `asus-nuc` `.2`. `wg2.md` |
| `wg-india` | NUC | same `/30` | — | NUC side of `wg2`. Linux `wg-quick` |

```text
Phone / Mac / later Beryl
    ├─ wg0  →  Trusted
    └─ wg1  →  Guest (Internet only)

VLAN 40  →  wg2  →  NUC wg-india  →  India ISP

Travel India  →  Tailscale app  →  asus-nuc
    (not via the house)
```

Do not put WireGuard on a Services LXC.

Endpoints use `nj.sumitgahoi.me` (A only, `../vyos/ddns.md`). A spare AAAA profile is `v6.sumitgahoi.me`. Clients often try AAAA first and do not fall back, so the names stay separate. Do not put an AAAA on `nj`. The NUC has no physical access and uses the A name only.

Beryl: `../beryl.md`.
