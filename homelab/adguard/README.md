# AdGuard Home — CURRENT

CT 110 at `10.10.0.4`. Rebuild: `setup.md`. Invariants: `../REQUIREMENTS.md`.

| Node | Where | IP | Role | State |
|------|-------|----|------|--------|
| `adguard` | LXC 110 on `vmbr-svc` | `10.10.0.4` | DNS only. VyOS upstream for Trusted, Guest, IoT | CURRENT |

Gateway `10.10.0.1`. Do not reuse `.2` / `.3` (unused) / `.5`. No AdGuard DHCP.

```text
Trusted / Guest / IoT
    → 10.10.x.1  (DHCP, unchanged)
    → VyOS dns forwarding
    → AdGuard 10.10.0.4
         ├─ *.home.arpa
         └─ DoT (US WAN via Services NAT)

Remote WireGuard (later, `../wireguard/`)
    → 10.10.80.1 or 10.10.81.1 (VyOS recursor)
      not 10.10.0.4

VLAN 40
    → 1.1.1.1 via India-GW
```

VyOS queries AdGuard as `10.10.0.1` (AdGuard Allowed clients). VLANs
10/20/30 do not query `10.10.0.4` themselves. IoT still has no Internet; DNS to `10.10.30.1`
is VyOS input, and AdGuard’s DoT is Services→WAN. VLAN 40 stays on
`1.1.1.1` via India-GW.

VyOS has one upstream (`10.10.0.4`). DHCP option 6 stays the VLAN
gateway. AdGuard down → VLANs 10/20/30 lose DNS; VLAN 40 is unchanged.
Guest/IoT can resolve `home.arpa` if they ask; they get no search domain.

The AdGuard LXC’s own OS resolvers are `1.1.1.1` / `1.0.0.1`, not VyOS,
so apt and DoT bootstrap do not depend on the recursor that depends on
AdGuard.
