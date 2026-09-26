# Tailscale India-GW — DEPRECATED

Not current. VLAN 40 uses `wg2` (`../wireguard/wg2.md`). Behavior: `../REQUIREMENTS.md`.

## Former design

```text
VLAN 40
    → VyOS
    → CT 108 / 10.10.0.5
    → Tailscale
    → asus-nuc
    → India Internet
```

- It existed so Tailscale would not run on VyOS. The container was an exit-node client only. It did not advertise routes.
- CT 108 stays stopped, disk kept, `10.10.0.5` reserved, until removal (`../wireguard/wg2.md` section 7, `../README.md`).
- Do not run this beside `wg2`.
- Putting VLAN 40 back on this path is a requirements change, then `setup.md`. Do not apply `setup.md` while table 40 uses `wg2`.
