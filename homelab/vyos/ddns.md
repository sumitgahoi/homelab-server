# Cloudflare DDNS

VyOS publishes the `eth1` IPv4 as `nj.sumitgahoi.me`. Zone
`sumitgahoi.me`. Dialect: rolling `2026.09.16-0028`.

```text
eth1  →  VyOS  →  A record nj.sumitgahoi.me
```

The record is A only, DNS only (grey cloud). WireGuard ports are UDP.
TTL 120 seconds.

---

# 1 — Cloudflare

In the Cloudflare zone `sumitgahoi.me`, create the name. Cloudflare requires a value in Content; VyOS replaces it.

```text
Type:    A
Name:    nj
Content: 192.0.2.1
Proxy:   DNS only
TTL:     120
```

Create an API token with Zone / DNS / Edit on `sumitgahoi.me` only.
Copy it once. It does not go in Git.

From Trusted:

```bash
dig +time=2 +tries=1 @1.1.1.1 nj.sumitgahoi.me A
dig +time=2 +tries=1 @1.1.1.1 nj.sumitgahoi.me AAAA
```

The A answer is `192.0.2.1`. The AAAA answer is empty.

---

# 2 — VyOS

Tab-complete each node.

```text
configure
set service dns dynamic name cloudflare address interface 'eth1'
set service dns dynamic name cloudflare protocol 'cloudflare'
set service dns dynamic name cloudflare zone 'sumitgahoi.me'
set service dns dynamic name cloudflare host-name 'nj.sumitgahoi.me'
set service dns dynamic name cloudflare ip-version 'ipv4'
set service dns dynamic name cloudflare password 'CLOUDFLARE_API_TOKEN'
```

Leave `username` unset. `ip-version ipv4` keeps the GUA off this name.

```bash
compare
commit-confirm 60
show dns dynamic status
```

The status address is the `eth1` IPv4. From Trusted, `dig +time=2 +tries=1 @1.1.1.1 nj.sumitgahoi.me A` matches it. Main default stays `eth1`. Then `confirm` and `save`.

On export, replace that password with `redacted` in `commands.txt` and `config.boot`.

---

# 3 — NUC

Open Tailscale SSH and leave it open.

In `/etc/wireguard/wg-india.conf`:

```text
Endpoint = nj.sumitgahoi.me:51822
```

```bash
sudo bash -c 'wg syncconf wg-india <(wg-quick strip wg-india)'
sudo grep Endpoint /etc/wireguard/wg-india.conf
sudo wg show wg-india
ip route get 1.1.1.1
```

`wg show` prints the resolved address, not the name. Handshake is recent. `1.1.1.1` is via `enp1s0`.

`/etc/systemd/system/wg-india-reresolve.service`:

```text
[Service]
Type=oneshot
ExecStart=/bin/bash -c 'wg syncconf wg-india <(wg-quick strip wg-india)'
```

`/etc/systemd/system/wg-india-reresolve.timer`:

```text
[Timer]
OnBootSec=2min
OnUnitInactiveSec=3min

[Install]
WantedBy=timers.target
```

```bash
systemctl daemon-reload
systemctl enable --now wg-india-reresolve.timer
```

Phone profiles use the same name: `nj.sumitgahoi.me:51820` for `wg0`, `nj.sumitgahoi.me:51821` for `wg1`.
