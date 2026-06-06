# AV home theatre

Logical design for the **Denon AVR-X3700H**-based theatre. Parts list: **`equipment-inventory.md`**.

## Physical layout

| Location | What lives there |
|----------|------------------|
| **Basement rack** | **Denon AVR-X3700H**, **Hypex NCx500**, **Furman**, **PS5**, **Switch 2**, **Apple TV** |
| **First floor — family room** | **LG OLED 65″ B7**, **3× CWM73 S2**, **2× CWM663**, **2× CCM662**, **HSU VTF-15H MK2** |

**Structured wiring is complete** between basement and family room — **HDMI**, **speaker homeruns**, and **sub LFE**. Rack-side terminations go to **Denon / Hypex**; room-side terminations go to **speakers**, **sub**, and **TV**.

**Powered sub:** **HSU** sits in the **family room** on **local AC**. Only the **LFE signal** runs from the basement **Denon** — not amplifier power from the rack.

## Channel layout

| Channel | Speaker (family room) | Amplification (basement) |
|---------|----------------------|---------------------------|
| **Front L** | B&W **CWM73 S2** | **Hypex NCx500** ch 1 ← Denon **pre-out L** |
| **Center** | B&W **CWM73 S2** | **Hypex NCx500** ch 2 ← Denon **pre-out C** |
| **Front R** | B&W **CWM73 S2** | **Hypex NCx500** ch 3 ← Denon **pre-out R** |
| **Surround L** | B&W **CWM663** | Denon **internal amp** → pre-run |
| **Surround R** | B&W **CWM663** | Denon **internal amp** → pre-run |
| **Height L** (Atmos) | B&W **CCM662** | Denon **internal amp** → pre-run |
| **Height R** (Atmos) | B&W **CCM662** | Denon **internal amp** → pre-run |
| **Sub** | **HSU VTF-15H MK2** | Denon **sub out (LFE)** → pre-run RCA |

**Denon speaker config:** assign **Front L/R/C** as **Pre-out** (not amp). Assign **Surround** and **Height / Top Front** (or **Top Middle**) for the **CWM663** and **CCM662** pairs. Sub = **1× LFE**.

## Signal flow

```text
  BASEMENT (rack)                         FIRST FLOOR (family room)
  ─────────────────                       ─────────────────────────

  [ PS5 ] [ Switch 2 ] [ Apple TV ]
            │ HDMI
            ▼
     ┌──────────────────┐
     │  Denon AVR-X3700H │
     │  · Audyssey        │
     │  · pre-outs L/C/R ──► [ Hypex NCx500 ] ── speaker homeruns ──► 3× CWM73 S2
     │  · internal amps ─── speaker homeruns ──────────────────────► 2× CWM663 + 2× CCM662
     │  · sub out (LFE) ─── RCA homerun ─────────────────────────────► [ HSU VTF-15H MK2 ]
     │  · HDMI out        ─── HDMI homerun ──────────────────────────► [ LG OLED 65″ B7 ]
     └──────────────────┘

  Furman → Denon, Hypex, sources (dedicated basement circuit → planned)
  PDU → Proxmox, modem, switch (same dedicated circuit → planned)
  HSU sub → local AC in family room (separate circuit)
  Sources → CBS350 (basement switch)
```

**Audio path:** all **HDMI sources** connect to **Denon inputs** in the **basement** — not directly to the TV. Video to the **LG B7** is always from **Denon HDMI out** via the pre-run **HDMI** homerun.

## Pre-out → Hypex NCx500 wiring

All steps at the **basement rack** — speaker cables continue via existing homeruns to the **family room**.

1. In Denon setup: **Speakers → Front L/R/C → Pre-out** (disable internal amp for those three channels).
2. **RCA pre-outs** on the Denon (**FRONT L**, **FRONT R**, **CENTER**) → **NCx500 line inputs** (match L/C/R to amp channels 1/2/3 — label at rack).
3. **NCx500 speaker outputs** → **LCR homerun terminations** at the rack (polarity matched to **CWM73 S2** in family room).
4. Set Denon **Front L/R/C** levels to **0 dB** pre-out reference before Audyssey; fine-tune only after calibration.
5. **Power-on order:** Furman → **NCx500** → **Denon** → sources. Power-off reverse.

**Long speaker runs:** verify polarity and continuity on each homerun before first power-on. Label rack patch block ↔ room location.

**Gain staging:** start NCx500 gain moderate; run Audyssey at the **family room MLP**; adjust amp gain only if Denon reports pre-out clipping (uncommon at home levels).

## Subwoofer — HSU VTF-15H MK2 (family room)

- **Location:** **family room** — placement per crawl / Audyssey (not in basement).
- **Power:** **local AC outlet** in family room (not through **Furman** in basement).
- **Signal:** **Denon SUBWOOFER PRE OUT** (basement) → pre-run **RCA/LFE** → HSU **LFE / line in**.
- HSU **crossover bypass / LFE mode** ON — **Denon sets the crossover** for all speakers.
- **Phase:** start **0°**; flip only if Audyssey or sweeps show a null at the MLP.

## Crossovers (starting points — confirm after Audyssey)

| Speaker group | Suggested crossover | Notes |
|---------------|---------------------|--------|
| **CWM73 S2** (LCR) | **80 Hz** | Typical for in-wall 8″ class; try **60–100 Hz** if Audyssey suggests |
| **CWM663** (surround) | **80 Hz** | Match bed channels unless surrounds measure very different |
| **CCM662** (Atmos) | **120 Hz** | Small ceiling drivers — often higher crossover is OK |
| **VTF-15H MK2** | **LFE + main** | Denon **LFE** channel; mains high-pass at crossovers above |

Use **Audyssey MultEQ XT32** (X3700H) results as the authority — these are starting points only.

## HDMI and display

| Setting | Recommendation |
|---------|----------------|
| **Denon → TV** | **HDMI Main out** (basement) → pre-run homerun → **LG B7** — input that supports **ARC/eARC** |
| **LG B7** | Enable **HDMI Ultra HD Deep Color** on that input; **eARC** on if using TV apps |
| **Source UHD/HDR** | **Enhanced** / **8K Enhanced** (or equivalent) on Denon inputs used by **PS5** / **Apple TV** |
| **HDCP** | All sources → Denon (basement) → TV (family room) — single chain |

**LG B7 note:** 2017 set — **Dolby Vision** support is limited vs newer panels; **HDR10** from **PS5** / **Apple TV** is the common path. **eARC** on B7 supports **Dolby Atmos** from internal TV apps when routed through ARC — still prefer **sources → Denon** for Atmos bitstreaming where possible.

## Audyssey MultEQ XT32

Run calibration in the **family room** — the mic never goes in the basement.

1. **Mount** the **Audyssey calibration mic** at **primary listening position (MLP)** in the **family room** — ear height, centered on couch.
2. **Run full multi-position calibration** (minimum 3, prefer 6–8 positions) — include seats left/right of MLP if used regularly.
3. **Disable** **Dynamic EQ** and **Dynamic Volume** for critical listening unless you prefer them for TV at low volume.
4. **Subwoofer level:** let Audyssey set it; verify **75 dB** reference with SPL meter if the sub seems hot/cold.
5. **Save** results to a **slot**; export **PDF report** if the app offers it — attach to this repo when done.
6. **Re-run** after moving the **HSU sub**, changing **Furman** outlet grouping, or major **family room** furniture changes.

**External amp:** Audyssey measures **pre-out → NCx500 → homerun → CWM73 S2** as one chain — no separate mic step for the Hypex.

**Long LFE run:** if hum appears on the sub line, try a **ground-loop isolator** on the LFE (last resort) or confirm shield continuity on the pre-run.

## Source notes

| Source | Video | Audio | Network |
|--------|-------|-------|---------|
| **PS5** | **4K HDR** → Denon | **LPCM / Dolby Atmos (HDMI)** | Wired to **CBS350** in basement rack |
| **Nintendo Switch 2** | **4K** (dock) → Denon | **PCM / surround per title** | Wired to switch when possible |
| **Apple TV** | **4K Dolby Vision / HDR** → Denon | **Atmos** where app supports | Wired to **CBS350** in basement rack |

Set **HDMI CEC (HDMI Control)** on Denon and TV so one remote can switch inputs if desired — or disable CEC if it causes double-switching.

## Power

### Basement rack — dedicated circuit (planned)

The **basement rack** will run on a **dedicated electrical line** (separate breaker) — not shared with general basement or family room loads. Target: stable power for **AV + homelab** without dimmer- or appliance-induced noise on the same branch.

```text
  Panel breaker (dedicated) ──► basement rack receptacle(s)
                                      ├── PDU1215 → Proxmox, modem, CBS350
                                      └── Furman PST-8 → Denon, Hypex, PS5, Switch 2, Apple TV
```

**Family room is out of scope** for this circuit — **LG B7** and **HSU sub** use **local room outlets** upstairs.

| Location | Gear | Power source |
|----------|------|--------------|
| **Basement rack** | **Denon**, **Hypex NCx500**, **PS5**, **Switch 2**, **Apple TV** | **Furman PST-8** ← dedicated basement circuit |
| **Basement rack** | **Proxmox host**, switch, modem | **PDU1215** ← same dedicated circuit (or separate receptacle — document at install) |
| **Family room** | **HSU VTF-15H MK2**, **LG B7** | **Local outlets** (existing circuits) |

**After electrician install — record here:**

| Item | Value |
|------|--------|
| Breaker size / panel label | |
| Receptacle count at rack | |
| AV (**Furman**) vs homelab (**PDU**) same or split circuit | |
| Install date | |

## Commissioning checklist

Wiring is done — record these at first power-on:

| Item | Value |
|------|--------|
| Denon **HDMI input** per source | PS5 = · Switch 2 = · Apple TV = |
| **CBS350** port per source | |
| Rack **patch block** labels ↔ family room locations | |
| **Pre-out → NCx500** RCA map | L = ch · C = ch · R = ch |
| **Audyssey** profile slot / date | |
| **Final crossovers** post-calibration | |
| **HSU sub** family-room position / phase | |
