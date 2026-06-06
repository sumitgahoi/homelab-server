# Pi-hole

LAN DNS — blocklists, caching, local records. Forwards non-blocked queries upstream over **DNS-over-TLS**.

## Deployment (locked)

- **Dedicated LXC** on **`vmbr-svc`** only — static IP **`10.10.0.53`**
- **Not** on private / guest / IoT VLANs at L2; clients reach it via VyOS routing
- Disk on **SN770** datastore (not bulk HDD)

## Upstream DNS (locked)

| Role | Provider | IPs | TLS SNI |
|------|----------|-----|---------|
| Primary | **Quad9** | `9.9.9.9`, `149.112.112.112` | `dns.quad9.net` |
| Secondary | **Cloudflare** | `1.1.1.1`, `1.0.0.1` | `one.one.one.one` |

Configure in Pi-hole → Settings → DNS (confirm `#hostname` syntax for your Pi-hole version). **Not** ISP DNS by default.

## VyOS integration

- **private / guest DHCP:** option 6 = **`10.10.0.53`**
- **Allow** private → Pi-hole `:53`; guest if desired
- **IoT → Pi-hole `:53`** per **`vyos.md` § IoT firewall**
- Restrict Pi-hole web UI sources in VyOS — not from IoT by default

## Query path

```text
Client → Pi-hole (10.10.0.53) → Quad9 / Cloudflare (DoT) → Internet
```

## Resources

~512 MiB RAM, 1 vCPU — scale blocklists carefully on a 32 GB host.

## Phase

**Phase 2+** — skip until **`vmbr-svc`** exists and VyOS routes the svc subnet.
