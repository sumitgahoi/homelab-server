# Network requirements

What the network must do, and why. Implementation lives in `proxmox/interfaces` (after cutover), `proxmox/interfaces.until-cutover`, `vyos/config.boot`, and `cbs350/running-config`. Topology lives in `README.md`.

Git desired state is not automatically the live router. `config.boot` has not yet been authoritatively applied and verified.

## Why this design

- **One Proxmox host.** It is a hypervisor, not a router. It does not DHCP, NAT, or firewall client traffic.
- **CBS350 tags and untags.** It does not route. Git desired state is `cbs350/running-config` (apply: `cbs350/README.md`). The port-use table in `README.md` is as-built wiring. Cisco merge does not delete omitted nodes; apply from factory.
- **VyOS is the only router**, and the only DHCP, DNS, and NAT for the house LAN.
- **One LAN trunk** carries client VLANs. WAN is a separate NIC/bridge. Services sit on an isolated bridge with no physical NIC.
- **Numbering is locked:** 0 = LAN, 1 = WAN, 2 = services (`nic` / `vmbr` / `net` / MAC `02:00:00:00:00:0N` / `eth`). Do not delete/re-add VyOS guest NICs.

## CURRENT

### Topology (required)

- LAN trunk: `nic0` → `vmbr0` → VyOS `eth0`
- WAN: `nic1` → `vmbr1` → VyOS `eth1`. Until cutover the host has `192.168.50.200/24` on `vmbr1` (ASUS LAN, **only** default gateway `192.168.50.1`). After cutover, remove that address; the host has no WAN IP.
- Services: `vmbr-svc` → VyOS `eth2` = `10.10.0.1/24`
- Proxmox management: until cutover, `192.168.50.200` on `vmbr1` only (no `vmbr0.10`). After cutover: `vmbr0.10` = `10.10.10.3/24`, gateway `10.10.10.1`. Do not put two `gateway` lines in `/etc/network/interfaces`.
- VyOS management and Trusted gateway: `10.10.10.1`

As-built WAN is still double NAT (S33 → house ASUS → `nic1`). That ASUS is not `asus-nuc`.

IPv6 is not part of the current routing design. Do not treat that as a permanent disable.

### Client networks

| Network | VLAN | Subnet | Gateway |
|---------|------|--------|---------|
| Trusted | 10 | `10.10.10.0/24` | `10.10.10.1` |
| Guest | 20 | `10.10.20.0/24` | `10.10.20.1` |
| IoT | 30 | `10.10.30.0/24` | `10.10.30.1` |
| India | 40 | `10.10.40.0/24` | `10.10.40.1` |
| Services | — | `10.10.0.0/24` | `10.10.0.1` |

VyOS DHCP on all four client VLANs, pools `.100`–`.250`. DNS for those clients is the VLAN gateway. Upstream DNS is Cloudflare (`1.1.1.1` / `1.0.0.1`) via the US WAN. NAT to WAN (`eth1`) is Trusted and Guest only.

### Behavior

**Trusted** may use the Internet, other VLANs, Services, and VyOS management (including SSH).

**Guest** may use the Internet. Guest must not reach internal networks (other VLANs, Services, or VyOS management). DHCP/DNS to its gateway is allowed.

**IoT** must not use the Internet and must not reach Trusted, Guest, India, or Services. DHCP/DNS to its gateway is allowed. VyOS is not an NTP server.

**India** must not use the normal WAN. It must not reach other internal networks or VyOS management. DHCP/DNS to its gateway is allowed. DNS recursion via VyOS/Cloudflare on the US WAN is an acceptable temporary implementation because India has no general Internet forwarding. This is not the long-term DNS design.

**Services** has no general Internet access and must not initiate connections into client VLANs. Trusted may initiate into Services; established return traffic is allowed. Services guests (AdGuard/Tailscale) are not deployed.

**WAN** NATs Trusted and Guest only. Unsolicited inbound from WAN is denied (WAN DHCP client traffic excepted).

**Management** of VyOS and Proxmox is from Trusted (VLAN 10) once `vmbr0.10` exists, including OOB on the switch when VyOS is down. Until then, Proxmox is reached at `192.168.50.200` on the ASUS LAN. Guest, IoT, and India must not administer VyOS.

## PLANNED

- S33 2.5G directly on `nic1`; retire the house ASUS. Isolation policy does not change.
- UniFi AP: one SSID per client VLAN. Exact SSID names are not locked. Required mapping: Trusted→VLAN 10, Guest→VLAN 20, IoT→VLAN 30, India→VLAN 40. Isolation remains VyOS, not AP-only.
- AdGuard on Services (`10.10.0.53`): DNS only. VyOS stays the DHCP server. DHCP option 6 may later point appropriate clients at AdGuard. See `adguard.md`.
- Tailscale: not on VyOS. `tailscale-us` (`10.10.0.52`) = subnet router + US exit. `tailscale-india` (`10.10.0.54`) = India-GW via `asus-nuc`. See `tailscale.md`.
- When India-GW exists, VLAN 40 IPv4 and IPv6 have **exactly one** permitted Internet egress: India-GW. Neither family may fall back to the normal VyOS WAN. If that path is down, fail closed. DNS must follow the India path and must not leak via the US WAN.
- Services Internet NAT / hairpin only if a future service needs it. Do not add a generic Services↔Trusted mesh. Trusted→Services stays the baseline; add narrowly scoped Services-originated exceptions when a real service requires them.

## DEFERRED

- Camera VLAN
- NAS `10.10.10.4`
- Stripping leftover VLAN 99 on live gear, if any remain

## Do not casually reopen

- Proxmox must not become the house router
- Locked 0/1/2 numbering and pinned VyOS MACs
- Native `config.boot` in Git is the complete desired VyOS config; `load` removes omitted nodes
- VM 100 is created and installed by hand; Git does not own guest lifecycle after that
- VyOS installer ISO is downloaded on Proxmox to `local:iso/vyos.iso` when that file is missing
- Git tracks Proxmox `interfaces`; operator copies the file and runs `ifreload`. Do not use the Network UI Apply button
- India fail-closed (no US WAN fallback) once India-GW is implemented
