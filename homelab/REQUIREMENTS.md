# Network requirements

What the network must do, and why. Implementation lives in `proxmox/interfaces` (after cutover), `proxmox/interfaces.until-cutover`, `vyos/commands.txt`, and `cbs350/running-config`. Topology lives in `README.md`. Rebuild procedures live in the matching `setup.md` / `README.md` runbooks.

Git records known-good snapshots. A human applies them. Git is not an apply engine.

## Why this design

- **One Proxmox host.** It is a hypervisor, not a router. It does not DHCP, NAT, or firewall client traffic.
- **CBS350 tags and untags.** It does not route. Known-good config is `cbs350/running-config` (restore: `cbs350/README.md`). The port-use table in `README.md` is as-built wiring. Cisco merge does not delete omitted nodes; restore from factory.
- **VyOS is the only client-facing router**, and the only DHCP server. Trusted/Guest/IoT DNS and US-WAN NAT stay on VyOS. India-GW is a Services hop (not a second house router): it NATs VLAN 40 into Tailscale. Clients still use `10.10.40.1`.
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

VyOS DHCP on all four client VLANs, pools `.100`–`.250`. Trusted, Guest, and IoT DNS is the VLAN gateway; VyOS recurses to Cloudflare (`1.1.1.1` / `1.0.0.1`) via the US WAN. India DHCP advertises those same Cloudflare addresses as resolvers; VyOS does not recurse for VLAN 40. NAT to WAN (`eth1`) is Trusted, Guest, and Services only. India is never source-NATed out `eth1`.

### Behavior

**Trusted** may use the Internet, other VLANs, Services, and VyOS management (including SSH).

**Guest** may use the Internet. Guest must not reach internal networks (other VLANs, Services, or VyOS management). DHCP/DNS to its gateway is allowed.

**IoT** must not use the Internet and must not reach Trusted, Guest, India, or Services. DHCP/DNS to its gateway is allowed. VyOS is not an NTP server.

**India** Internet has exactly one IPv4 egress: India-GW `10.10.0.5` on Services, which consumes the existing `asus-nuc` Tailscale exit node in India. Clients keep gateway `10.10.40.1` and do not run Tailscale. VyOS policy-routes `10.10.40.0/24` to table 40 (next-hop `10.10.0.5`) and must not use the main default route / `eth1` for that source. Fail closed: if India-GW, Tailscale on India-GW, or `asus-nuc` is down, VLAN 40 has no Internet even when the US WAN is healthy. No India→`eth1` forward accept; no India masquerade on `eth1`. India must not initiate into Trusted, Guest, IoT, Services hosts, or VyOS management. DHCP to `10.10.40.1` is allowed. DNS is forwarded traffic to the advertised resolvers via India-GW; it must not use VyOS/Cloudflare on the US WAN. If the India path is down, DNS fails with it. IPv6 on VLAN 40 is not provided; do not give India clients a US IPv6 path.

Known-good for this path is `vyos/commands.txt` plus `tailscale-india/setup.md`. As-built: India SSID/VLAN/DHCP work (`unifi.md`); India-GW (CT 108) is deployed.

**Services** may use the Internet. Services must not initiate connections into Trusted, Guest, IoT, or India. Trusted may initiate into Services; established/related return traffic is allowed. Forward default-drop is the isolation; do not add explicit Services→internal drop rules. UniFi OS Server is on Services (`10.10.0.2`). India-GW is on Services (`10.10.0.5`). AdGuard and `tailscale-us` are not deployed.

**WAN** NATs Trusted, Guest, and Services. Unsolicited inbound from WAN is denied (WAN DHCP client traffic excepted).

**UniFi** is Wi-Fi only (controller on Services, AP on CBS350 GE2). VyOS remains the client-facing router, DHCP server, US-side DNS forwarder, US-WAN NAT device, and firewall. UniFi must not provide those functions. Services is not a UniFi network. One SSID per client VLAN (names are not locked): Trusted→VLAN 10, Guest→VLAN 20, IoT→VLAN 30, India→VLAN 40. Isolation remains VyOS, not AP-only. As-built: `unifi.md`.

**Management** of VyOS and Proxmox is from Trusted (VLAN 10) once `vmbr0.10` exists, including OOB on the switch when VyOS is down. Until then, Proxmox is reached at `192.168.50.200` on the ASUS LAN. Guest, IoT, and India must not administer VyOS.

## PLANNED

- S33 2.5G directly on `nic1`; retire the house ASUS. Isolation policy does not change.
- AdGuard on Services (`10.10.0.4`, CT 110): DNS only. VyOS stays the DHCP server. Trusted, Guest, and IoT keep DHCP option 6 as the VLAN gateway. VyOS `dns forwarding` has a single upstream, `10.10.0.4`. AdGuard holds `*.home.arpa` and DoT upstreams. IoT still must not use the Internet; IoT DNS is allowed only to its gateway. Guest and IoT may resolve `home.arpa` if they query it; they do not get that search domain. AdGuard down means those three VLANs lose DNS. India DNS is unchanged (PBR → India-GW). See `adguard/`.
- Tailscale: not on VyOS. `tailscale-us` (`10.10.0.3`) = subnet router + US exit. See `tailscale-us.md`.
- House IPv6 is still not in the routing design. When it is, India IPv6 must still have no US-WAN fallback; do not enable India IPv6 until an India IPv6 path exists.
- Do not add a generic Services↔Trusted mesh. Trusted→Services stays the baseline; add narrowly scoped Services-originated exceptions when a real service requires them. Hairpin NAT only if a future service needs it.

## DEFERRED

- Camera VLAN
- NAS `10.10.10.4`
- Stripping leftover VLAN 99 on live gear, if any remain

## Do not casually reopen

- Proxmox must not become the house router
- Locked 0/1/2 numbering and pinned VyOS MACs
- `vyos/commands.txt` is the known-good VyOS configuration. A human applies `set` / `delete` from `vyos/setup.md`. Do not bulk-`load` a `config.boot` onto a running router; that produced unexpected round-trip behavior for some firewall state.
- VM 100 is created and installed by hand (`vyos/install.md`); Git does not own guest lifecycle after that
- VyOS installer ISO is downloaded on Proxmox to `local:iso/vyos.iso` when that file is missing
- Git tracks Proxmox `interfaces`; the operator copies the file and runs `ifreload`. Do not use the Network UI Apply button
- India fail-closed (no US WAN fallback, including DNS and IPv6)
- Do not introduce Ansible, Terraform, generators, or CI/CD to deploy this homelab
