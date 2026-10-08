# Android Secure Random Reference

- Search method: Google via browser MCP
- Retrieved: 2026-09-25
- Primary source: https://developer.android.com/privacy-and-security/risks/weak-prng

## Relevant findings

Android documents `java.security.SecureRandom` as the cryptographically secure pseudo-random choice and distinguishes it from ordinary `java.util.Random`. The same documentation states that true random-number generation requires specialized hardware and is outside normal application development. ARMCP therefore prefers Linux `/dev/hwrng` through Root for the requested hardware-random click, but uses `SecureRandom` when that device is unavailable so coordinate selection remains unpredictable without pretending the fallback is hardware true random.
