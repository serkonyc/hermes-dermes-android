# v4.19 Upgrade Plan (approved topics 2026-10-24)

## 1. Digest: multi-source + on-device LLM summary
- Sources fetched on phone (no keys): existing Hermes digest.json + RSS:
  OpenAI blog, Anthropic news, Google AI blog, Mistral news (RSS/XML), Hacker News front page (hn.algolia.com API, CORS-free).
- Parse RSS with android.org.xml pull parser; merge items, cap ~25.
- Local LFM2.5-8B generates 1-screen overview ("Что нового сегодня") at top card;
  reuses ChatActivity native runtime (load->generate->unload, RAM-safe).
- Offline: cached copy, summary kept with cache.

## 2. Device: full metrics screen
- CPU: cores, cur/max freq per core (sysfs scaling_cur_freq), 1s delta load %.
- Thermal: PowerManager.getThermalStatus (no CPU temp on Android w/o root - honest label) + battery temp real.
- Battery: level, status, voltage, tech, current now (BatteryManager hidden API via reflection guarded), plugged.
- Memory: ActivityManager.MemoryInfo total/avail/threshold + App heap/limit (Runtime + getMemoryInfo).
- Disk: StatFs total/free on /data.
- GPU: GLES renderer string + version (EGL14 query), CPU ABI.
- Network: interfaces, IPs, DNS, MTU (NetworkInterface), active network type.
- Sensors: temperature sensors enumerated where available.
- Battery + load refresh every 2 s.

## 3. History -> Phone/App activity
- App opens count/day, session duration (onPause/onResume delta), game runs + swipes counters (RunnerActivity posts).
- Bars for 7 days (reuse BarsView), honest note about what is measured.
- Old request/token counters dropped.

## Non-goals
- No root reads (/sys thermal zones are root-only on One UI).
- No cloud summarization.
