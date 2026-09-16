# Power

One dedicated basement duplex, **15 A breaker** on **12 AWG** (verified 2026-09-12). Wire is rated 20 A; the breaker is the limit.

```text
Panel -- 15 A --> duplex
  A --> Furman PST-8 --> Denon, NCx500, PS5, Switch 2, Apple TV
  B --> CST1500SUC --> PDU1215 --> Proxmox, S33, CBS350
```

Proxmox is not on the Furman. HSU and LG B7 are on family-room circuits.

Planning peak ~14 A (GPU + AV transient) vs 15 A / 12 A continuous — no margin. First fix: swap to 20 A after an electrician confirms the homerun is 12 AWG end-to-end (no 14 AWG). 5-15R receptacle may stay.

Until then: CPU PL1=PL2=65 W; Denon ECO; do not stack GPU jobs with movie peaks.

UPS is homelab only (NUT on Proxmox). AV peaks would oversize it. USB from UPS to Proxmox. PDU has no master switch.
