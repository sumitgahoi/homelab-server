# Tailscale India-GW — DEPRECATED

Do not run Tailscale on VyOS. VLAN 40 clients do not run Tailscale. Invariants: `../REQUIREMENTS.md`. Live VLAN 40 egress is VyOS `wg2` → `asus-nuc` `wg-india` (`../wireguard/wg2.md`). `../vyos/commands.txt` points table 40 at `wg2`. Do not put `10.10.0.5` back while that is the requirement.

CT 108 (`tailscale-india`, `10.10.0.5`) is deprecated. This directory stays. It is the record of the old path and the procedure if Tailscale should carry VLAN 40 again. It is not current infrastructure. Do not apply `setup.md` to the live router.

Stop the container. Do not destroy it in that sitting. Procedure: `../wireguard/wg2.md` section 7 (`pct set 108 --onboot 0`, then `pct stop 108`). Do not `tailscale logout`. `10.10.0.5` stays reserved until a later destroy. Tailscale on `asus-nuc` stays permanently (travel and remote rescue). `wg2` must not change it.

This directory is a **runbook plus design notes**. `setup.md` is the rebuild procedure for this deprecated design. It is not the live VLAN 40 path. There is intentionally no automation around it.

| Node | Where | IP | Role | State |
|------|-------|----|------|--------|
| `asus-nuc` | India (physical) | — | exit node | CURRENT (external) |
| `tailscale-india` | LXC 108 on `vmbr-svc` | `10.10.0.5` | Former India-GW: `--exit-node=asus-nuc` only. No advertised routes. Not on the VLAN 40 path | DEPRECATED — stop, keep the disk |

Tag: `tag:homelab-india-gw`. Gateway `10.10.0.1`. Do not reuse `.2` (UniFi), `.3` (unused), or `.4` (AdGuard). One process cannot advertise a US exit and consume `asus-nuc`.

Do not advertise `10.10.40.0/24` (or other client nets) into the tailnet. India-GW is not a subnet router.

## Packet path

Historical. Live VLAN 40 does not follow this. Clients still keep default router `10.10.40.1`. The live path is `../wireguard/wg2.md`.

```text
India client 10.10.40.x
    → VyOS eth0.40 (10.10.40.1)
    → policy route PBR-INDIA / table 40 → next-hop 10.10.0.5
    → India-GW eth0
    → tailscale0 (exit node asus-nuc)
    → encrypted tunnel
    → asus-nuc (India)
    → Indian ISP
    → Internet
```

Return traffic is established/related through the same path. Trusted/Guest/Services keep the main default route out `eth1`.

## Why it fails closed

This is the old CT 108 mechanism, kept as the deprecated design. It is not how VLAN 40 fails closed today. Live fail-closed is table 40 → `wg2` plus the distance-254 blackhole (`../wireguard/wg2.md`).

Positive allow-list, default deny:

1. **VyOS PBR** on `eth0.40` sends `10.10.40.0/24` to table 40 only. Table 40 default is `10.10.0.5`, with a higher-distance blackhole so an empty table does not fall through to the main US default.
2. **VyOS forward** default-drop. India has no accept out `eth1`. The only India forward accept is out `eth2`, and RFC1918 destinations are dropped first so India cannot pivot into Services/Trusted/Guest/IoT.
3. **VyOS NAT** masquerades Trusted, Guest, and Services to `eth1` only. `10.10.40.0/24` is not a source match. Even a mis-routed India packet would leave without a usable WAN translation.
4. **India-GW FORWARD** default-drop. New forwarded packets are allowed only `10.10.40.0/24` → `tailscale0`. Tailscale exit-node policy routing (table 52) can fall through to the LXC default gateway `10.10.0.1` when the tunnel is down; the FORWARD policy is what stops that becoming US WAN.
5. **India-GW NAT** masquerades only out `tailscale0`. No masquerade out the Services NIC.

Failure modes with US WAN still up:

| Break | What India clients see |
|-------|------------------------|
| India-GW down | Next-hop `10.10.0.5` unreachable; table 40 blackhole / ARP fail; no `eth1` accept |
| Tailscale down or no `tailscale0` | FORWARD drop (oif is not `tailscale0`); no US fallback |
| `asus-nuc` down / exit node unusable | Tunnel has no working Internet; still no `eth1` path |
| Restore India-GW + Tailscale + `asus-nuc` | Path returns |

## DNS

VLAN 40 DHCP option 6 advertises `1.1.1.1` and `1.0.0.1` (not `10.10.40.1`).

Queries are ordinary forwarded UDP/TCP 53 (and DoH/DoT if a client uses them) with destination those resolvers. They follow the same PBR → India-GW → `asus-nuc` path. The resolver sees the Indian ISP address.

VyOS DNS forwarding does not listen on `10.10.40.1` and does not allow-from `10.10.40.0/24`. Input DNS is Trusted/Guest/IoT only.

If India-GW/Tailscale/`asus-nuc` is down, those DNS packets have no valid path. Fail closed. Other VLANs are unchanged.

## IPv6

No India IPv6 path is implemented. Interim: no IPv6 connectivity on VLAN 40 (`no-default-link-local` + `ipv6 disable-forwarding` on `eth0.40`; no RA/DHCPv6). Do not later enable US-side IPv6 on VLAN 40 as a bypass.

## If Tailscale carries VLAN 40 again (DEFERRED)

Not current. `wg2` stays the only India egress until `../REQUIREMENTS.md` is changed on purpose. One egress. This is not a second path beside `wg2`, and it is not a fallback when `wg2` is down.

The design is the one already recorded above. VyOS policy-routes `10.10.40.0/24` to `10.10.0.5`. CT 108 consumes `asus-nuc` as an exit node and does not advertise routes. Clients still do not run Tailscale. Tailscale still does not run on VyOS. Fail-closed is the LXC forward and NAT policy in this file, not table 40 → `wg2`.

Before any command in `setup.md`:

1. Change the India egress requirement from `wg2` to this LXC, in the same sitting as the cut.
2. If the container was only stopped, `pct start 108` and confirm `tailscale0` and the exit node. Then apply the VyOS half from `setup.md` (table 40 next-hop `10.10.0.5`, forward rule 400 out `eth2`). Do not leave `wg2` and this next-hop as two defaults for VLAN 40.
3. If the container was destroyed, follow `setup.md` from the start, including a new Tailscale login. `10.10.0.5` is that guest again.
4. `wg2` may stay up for Trusted SSH to `10.10.82.2`. It must not be the VLAN 40 default while this path is the requirement.

## Rebuild record

`setup.md` is that future procedure, and the known-good deployment of the deprecated guest. Follow it one change at a time. It includes TUN passthrough, forwarding, Tailscale, nftables, VLAN 40 return routing, table 52, the priority-2500 main-table exception, `india-return-route.service`, why `onlink` is required, DNS/location leak tests, IPv6 restrictions, fail-closed tests, and reboot persistence. Step 7 in that file points VLAN 40 at `10.10.0.5`. Do not apply it while table 40 uses `wg2`.

Debian LXC on `vmbr-svc`. 1 vCPU / 512 MiB unless that proves too small. Static `10.10.0.5/24`, gateway `10.10.0.1`. Hostname `tailscale-india`. Needs `/dev/net/tun`. IPv6 forwarding off.

`--accept-dns=false` keeps the GW itself on Services/VyOS DNS (US). `--exit-node-allow-lan-access=true` keeps `10.10.0.0/24` local. Do not add a US default fallback for forwarded `10.10.40.0/24`.

Tailscale admin: allow `tag:homelab-india-gw` to use `asus-nuc` as an exit node. Do not grant subnet-router rights. Exact ACL JSON is not stored here. `asus-nuc` is an existing external exit node; do not rebuild it.

Other Services hosts keep gateway `10.10.0.1`. They must not use `10.10.0.5` as a default route. No `--advertise-routes`.

## Verification

The checks live in `setup.md`. Do not invent live results. US WAN should stay up during the negative tests.

Not the same sitting as the S33 swap. The India SSID is live (`../unifi.md`).
