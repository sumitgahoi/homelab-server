# Network requirements

What the network must do, and why. Implementation lives in `proxmox/interfaces` (CURRENT host), `vyos/commands.txt`, and `cbs350/running-config`. Topology lives in `README.md`. Rebuild procedures live in the matching `setup.md` / `README.md` runbooks. `proxmox/interfaces.until-cutover` is the pre-cutover host snapshot (archive).

Git records known-good snapshots. A human applies them. Git is not an apply engine.

## Why this design

- **One Proxmox host.** It is a hypervisor, not a router. It does not DHCP, NAT, or firewall client traffic.
- **CBS350 tags and untags.** It does not route. Known-good config is `cbs350/running-config` (restore: `cbs350/README.md`). The port-use table in `README.md` is as-built wiring. Cisco merge does not delete omitted nodes; restore from factory.
- **VyOS is the only client-facing router**, and the only DHCP server. Trusted/Guest/IoT DNS and US-WAN NAT stay on VyOS. India Internet for VLAN 40 is fail-closed through `asus-nuc` (CURRENT: India-GW + Tailscale; PLANNED: `wg-india`). Clients still use `10.10.40.1` and do not run Tailscale.
- **One LAN trunk** carries client VLANs. WAN is a separate NIC/bridge. Services sit on an isolated bridge with no physical NIC.
- **Numbering is locked:** 0 = LAN, 1 = WAN, 2 = services (`nic` / `vmbr` / `net` / MAC `02:00:00:00:00:0N` / `eth`). Do not delete/re-add VyOS guest NICs.

## CURRENT

### Topology (required)

- LAN trunk: `nic0` → `vmbr0` → VyOS `eth0`
- WAN: `nic1` → `vmbr1` → VyOS `eth1`. Host has no address on `vmbr1`.
- Services: `vmbr-svc` → VyOS `eth2` = `10.10.0.1/24`
- Proxmox management: `vmbr0.10` = `10.10.10.3/24`, gateway `10.10.10.1`. Do not put two `gateway` lines in `/etc/network/interfaces`.
- VyOS management and Trusted gateway: `10.10.10.1`

WAN cutover is complete: S33 2.5G on `nic1`; house ASUS retired. `asus-nuc` is the India exit, not that ASUS.

Client and LAN IPv6 are not in the routing design. WAN IPv6 on `eth1` only is CURRENT (`vyos/setup.md`). Do not advertise IPv6 toward VLANs.

### Client networks

| Network | VLAN | Subnet | Gateway |
|---------|------|--------|---------|
| Trusted | 10 | `10.10.10.0/24` | `10.10.10.1` |
| Guest | 20 | `10.10.20.0/24` | `10.10.20.1` |
| IoT | 30 | `10.10.30.0/24` | `10.10.30.1` |
| India | 40 | `10.10.40.0/24` | `10.10.40.1` |
| Services | — | `10.10.0.0/24` | `10.10.0.1` |

VyOS DHCP on all four client VLANs, pools `.100`–`.250`. Trusted, Guest, and IoT DNS is the VLAN gateway; VyOS has a single upstream, AdGuard `10.10.0.4`. India DHCP advertises Cloudflare (`1.1.1.1` / `1.0.0.1`); VyOS does not recurse for VLAN 40. NAT to WAN (`eth1`) is Trusted, Guest, and Services only. India is never source-NATed out `eth1`.

### Behavior

**Trusted** may use the Internet, other VLANs, Services, and VyOS management (including SSH).

**Guest** may use the Internet. Guest must not reach internal networks (other VLANs, Services, or VyOS management). DHCP/DNS to its gateway is allowed.

**IoT** must not use the Internet and must not reach Trusted, Guest, India, or Services. DHCP/DNS to its gateway is allowed. VyOS is not an NTP server.

**India** Internet has exactly one IPv4 egress: India-GW `10.10.0.5` on Services, which consumes the existing `asus-nuc` Tailscale exit node in India. Clients keep gateway `10.10.40.1` and do not run Tailscale. VyOS policy-routes `10.10.40.0/24` to table 40 (next-hop `10.10.0.5`) and must not use the main default route / `eth1` for that source. Fail closed: if India-GW, Tailscale on India-GW, or `asus-nuc` is down, VLAN 40 has no Internet even when the US WAN is healthy. No India→`eth1` forward accept; no India masquerade on `eth1`. India must not initiate into Trusted, Guest, IoT, Services hosts, or VyOS management. DHCP to `10.10.40.1` is allowed. DNS is forwarded traffic to the advertised resolvers via India-GW; it must not use VyOS/Cloudflare on the US WAN. If the India path is down, DNS fails with it. IPv6 on VLAN 40 is not provided; do not give India clients a US IPv6 path.

Known-good for this path is `vyos/commands.txt` plus `tailscale-india/setup.md`. As-built: India SSID/VLAN/DHCP work (`unifi.md`); India-GW (CT 108) is deployed.

**Services** may use the Internet. Services must not initiate connections into Trusted, Guest, IoT, or India. Trusted may initiate into Services; established/related return traffic is allowed. Forward default-drop is the isolation; do not add explicit Services→internal drop rules. UniFi OS Server is on Services (`10.10.0.2`). AdGuard is on Services (`10.10.0.4`, CT 110): DNS only. VyOS stays the DHCP server. Trusted, Guest, and IoT keep DHCP option 6 as the VLAN gateway. AdGuard holds `*.home.arpa` and DoT upstreams. IoT still must not use the Internet; IoT DNS is allowed only to its gateway. Guest and IoT may resolve `home.arpa` if they query it; they do not get that search domain. AdGuard down means those three VLANs lose DNS. India DNS stays PBR (today India-GW; after `wg-india`, still PBR). India-GW is on Services (`10.10.0.5`). A US Tailscale guest will not be deployed. `10.10.0.3` is unused.

**WAN** NATs Trusted, Guest, and Services. Unsolicited inbound from WAN is denied (WAN DHCPv4/DHCPv6 client and ICMPv6 on `eth1` excepted). `eth1` always has `address dhcp`, `address dhcpv6`, and `ipv6 address autoconf`. Never DHCPv6-PD. `eth0.10`/`.20`/`.30`/`.40` and `eth2` stay IPv6-locked (`no-default-link-local` + `disable-forwarding`). `firewall ipv6` input/forward default-drop. No RA into the house. No IPv6 SSH; management stays Trusted IPv4.

**UniFi** is Wi-Fi only (controller on Services, AP on CBS350 GE2). VyOS remains the client-facing router, DHCP server, US-side DNS forwarder, US-WAN NAT device, and firewall. UniFi must not provide those functions. Services is not a UniFi network. One SSID per client VLAN (names are not locked): Trusted→VLAN 10, Guest→VLAN 20, IoT→VLAN 30, India→VLAN 40. Isolation remains VyOS, not AP-only. As-built: `unifi.md`.

**Management** of VyOS and Proxmox is from Trusted (VLAN 10), including OOB on the switch when VyOS is down. Guest, IoT, and India must not administer VyOS.

## PLANNED

- WireGuard on VyOS (`wireguard/`). WAN IPv6 on `eth1` is already CURRENT. This is still a separate sitting. Not on a Services LXC. Not Tailscale-US. `wg0` `10.10.80.0/24` UDP `51820` = Trusted; `wg1` `10.10.81.0/24` UDP `51821` = Guest; `wg-india` `10.10.82.0/30` UDP `51822` = VLAN 40 → `asus-nuc` (NUC initiates; never `AllowedIPs = 0.0.0.0/0` on the NUC; VyOS must not install India’s `0.0.0.0/0` into **main**). Inside the tunnels stays IPv4. Road-warrior DNS is the tunnel gateway. VLAN 40 DNS stays `1.1.1.1` via PBR. No VPS. WAN holes: UDP `51820`–`51822` on `eth1` (IPv4 and IPv6). Default `Endpoint` is a DDNS **A** name. Spare AAAA name if `eth1` has a GUA — not both on one name. Prefer A. No GUA = IPv6 inbound WG unused; IPv4 still works.
- After `wg-india` fail-closed tests pass, retire CT 108. Tailscale leaves the US house. Tailscale remains on `asus-nuc` and on travel devices (India exit). It is not a VLAN 40 fallback.
- Tailscale: not on VyOS. Not on a US LXC after CT 108 is retired. Do not build `tailscale-us`.
- Client/LAN IPv6 stays out of the routing design. When house IPv6 is designed, India IPv6 must still have no US-WAN fallback; do not enable India IPv6 until an India IPv6 path exists.
- Do not add a generic Services↔Trusted mesh. Trusted→Services stays the baseline; add narrowly scoped Services-originated exceptions when a real service requires them. Hairpin NAT only if a future service needs it.

## DEFERRED

- Camera VLAN
- NAS `10.10.10.4`
- Stripping leftover VLAN 99 on live gear, if any remain

## Do not casually reopen

- Proxmox must not become the house router
- Locked 0/1/2 numbering and pinned VyOS MACs
- `vyos/commands.txt` is the known-good VyOS configuration. A human applies `set` / `delete` from `vyos/setup.md`. `vyos/config.boot` is the same config in tree form for reading. Do not bulk-`load` a `config.boot` onto a running router; that produced unexpected round-trip behavior for some firewall state.
- VM 100 is created and installed by hand (`vyos/install.md`); Git does not own guest lifecycle after that
- VyOS installer ISO is downloaded on Proxmox to `local:iso/vyos.iso` when that file is missing
- Git tracks Proxmox `interfaces`; the operator copies the file and runs `ifreload`. Do not use the Network UI Apply button
- India fail-closed (no US WAN fallback, including DNS and IPv6)
- Do not introduce Ansible, Terraform, generators, or CI/CD to deploy this homelab
- Do not add a VPS as a VPN hub or rebuild `tailscale-us` for US remote access
- Do not advertise IPv6 toward VLANs or IPv6-forward into the house. WAN IPv6 on `eth1` is VyOS WAN policy; WireGuard may use it as a spare inbound path.
