# Changelog

## 2.102.2

- Added a command to open the config screen
- Removed duplicate logger names
- Switched to reflection for ItemLocks compat
- Fixed ratelimit executor preventing shutdown

## 2.2.1

- Updated Russian translation (rfin0)
- Re-enabled ItemLocks compat

## 2.0.0

- Updated to mc26.1
- Temporarily disabled ItemLocks compat
- Mod versioning scheme is now `major.mc.minor`:
  - `major` is incremented on 'significant' feature changes, or breaking API changes (if
    applicable).
  - `mc` is never reset, and is incremented on every MC release, irrespective of whether a mod
    update was required.
  - `minor` is reset when `major` is changed, and is incremented on every update that does not
    change either of the previous two numbers.
