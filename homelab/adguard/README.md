# AdGuard Home

CT 110 at `10.10.0.4` on `vmbr-svc`. Rebuild: `setup.md`. Behavior: `../REQUIREMENTS.md`. Addresses: `../README.md`.

| Node | Where | IP | Role |
|------|-------|----|------|
| `adguard` | LXC 110 on `vmbr-svc` | `10.10.0.4` | DNS only. VyOS upstream for Trusted, Guest, and IoT |

- No AdGuard DHCP. VLAN 40 does not use this guest (India resolver addresses: `../vyos/setup.md`).
- Clients use the VLAN gateway. VyOS queries AdGuard as `10.10.0.1` (Allowed clients). VLANs 10/20/30 do not query `10.10.0.4`.
- AdGuard’s public upstream is DoT, and that traffic leaves as Services → US WAN.
- `wg0` / `wg1` DNS is the tunnel gateway (`10.10.80.1` / `10.10.81.1`), not `10.10.0.4` (`../wireguard/wg0.md`, `wg1.md`).
- The LXC’s own resolvers are `1.1.1.1` / `1.0.0.1`, not VyOS, so apt and DoT bootstrap do not depend on AdGuard.
