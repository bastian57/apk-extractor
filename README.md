# Apk Extractor

Ekstraktor APK on-device — fondasi HP untuk engine **MESIN CUK**.

- Daftar semua aplikasi terinstal (user/sistem), search, sort (nama/ukuran/terbaru)
- Extract 1-tap ke `Download/ApkExtractor/` (base + split APK aware)
- Batch extract via long-press multi-select
- Detail forensik: package, versi, min/target SDK, SHA-256, permissions, tanggal install/update
- Mini `rev strings` on-device: pindai `classes.dex`, tarik printable strings
- Share APK via Android share sheet

Tanpa root, offline-first. `QUERY_ALL_PACKAGES` (sideload only).

## Build

Push ke `main` → GitHub Actions build debug APK → rolling release `latest`.
