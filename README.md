# Aplikasi Monitoring Teknisi — SkyNet RT/RW Net

Aplikasi Android untuk **teknisi lapangan** SkyNet RT/RW Net (Kab. Bekasi). Teknisi memakai
aplikasi ini untuk menerima tugas perbaikan jaringan, memperbarui status pengerjaan, dan
mengirim **lokasi GPS secara realtime** ke admin selama proses perbaikan berlangsung.

Aplikasi ini adalah **klien mobile** dari backend Laravel (repo `Aplikasi-Monitoring`).
Dikerjakan sebagai studi kasus skripsi dengan metode pengembangan **RAD**.

> 📄 Dokumentasi lengkap:
> - **[CLAUDE.md](CLAUDE.md)** — kontrak teknis (API, struktur, konvensi kode)
> - **[CARA_KERJA_APLIKASI.md](CARA_KERJA_APLIKASI.md)** — penjelasan alur aplikasi 
> - **[RENCANA_IMPLEMENTASI.md](RENCANA_IMPLEMENTASI.md)** — arsip rencana & progres per fase
> - **[PENGEMBANGAN_LANJUTAN.md](PENGEMBANGAN_LANJUTAN.md)** — catatan iterasi fitur pasca-MVP

## Fitur Utama

- 🔐 **Login** akun teknisi (token disimpan di DataStore)
- 📋 **Daftar tugas aktif** dengan pull-to-refresh & auto-refresh
- 📝 **Detail tugas** + timeline riwayat pengerjaan (work logs)
- 🔧 **Alur status searah**: `ditugaskan` → `sedang memperbaiki` → `selesai`
- 📍 **Pelacakan GPS realtime** via Foreground Service selama perbaikan berlangsung
- 🔔 **Notifikasi tugas baru** lewat Firebase Cloud Messaging (FCM)
- 👤 **Profil** + logout

## Stack Teknologi

| Layer | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose |
| Arsitektur | MVVM (ViewModel + StateFlow) |
| Networking | Retrofit 2 + OkHttp 3 |
| Dependency Injection | Hilt |
| Penyimpanan Token | DataStore Preferences |
| Push Notifikasi | Firebase Cloud Messaging |
| GPS | FusedLocationProviderClient (Foreground Service) |
| Navigasi | Navigation Compose |
| Image Loading | Coil |

## Persiapan & Konfigurasi

### 1. Firebase
Unduh `google-services.json` dari Firebase Console (project yang sama dengan backend), lalu
letakkan di folder `app/`. **File ini tidak di-commit** (sudah ada di `.gitignore`).

### 2. Base URL backend & Build Variants
Backend Laravel sudah di-hosting di VPS dengan domain HTTPS resmi: `https://skynet-monitoring.tech/api/`.

| Build Variant | BASE_URL | Karakteristik |
|---|---|---|
| `deviceRelease` | `https://skynet-monitoring.tech/api/` | **Produksi teknisi:** Form Login bersih (tanpa input debug IP), koneksi HTTPS aman, signed debug key agar bisa langsung jalan di HP fisik. |
| `deviceDebug` | `https://skynet-monitoring.tech/api/` | **Pengembangan:** Default mengarah ke VPS (tanpa perlu ketik IP), namun input "Alamat Server" tetap tersedia jika sewaktu-waktu ingin tes backend lokal di LAN. |
| `emulatorDebug` | `http://10.0.2.2:8000/api/` | Untuk emulator Android terhadap Laravel lokal host machine. |

Untuk override alamat backend di varian `device` (misal uji coba dengan Laragon di WiFi lokal), tambahkan atau ubah di `local.properties`:
```properties
deviceBaseUrl=http://192.168.x.x:8000/api/
```

## Build & Run

> Windows (PowerShell): gunakan `.\gradlew.bat`. Pilih **Build Variants** di panel samping Android Studio:
> - `deviceRelease`: untuk aplikasi rilis teknisi siap pakai
> - `deviceDebug`: untuk mode debug
> - `emulatorDebug`: untuk emulator

```bash
# Build APK
./gradlew assembleDeviceRelease      # APK rilis teknisi (tampilan bersih)
./gradlew assembleDeviceDebug        # APK debug HP fisik
./gradlew assembleEmulatorDebug      # untuk emulator

# Verifikasi cepat kompilasi + Hilt (~20 dtk)
./gradlew compileEmulatorDebugKotlin --console=plain

# Unit test (host JVM)
./gradlew testEmulatorDebugUnitTest

# Lint
./gradlew lint
```

> ⚠️ Jangan pakai `--no-daemon` (kombinasi AGP 9 / Gradle 9 / Windows bisa hang di `kspDebugKotlin`).

## Pengujian

Unit test (di `app/src/test/`) memakai JUnit4 + `kotlinx-coroutines-test` + **mockk** + **turbine**.
Cakupan saat ini **51 test**: semua repository, ViewModel, `safeApiCall`, `DateUtils`, `WorkDuration`.

```bash
./gradlew testEmulatorDebugUnitTest --console=plain
```

## Struktur Singkat

```
app/src/main/java/com/skynet/monitoring/
├── di/          # Hilt: NetworkModule, RepositoryModule
├── data/        # api (Retrofit, model), repository, local (DataStore)
├── ui/          # Compose: screens, components, navigation, theme
├── service/     # LocationService (GPS), MonitoringFirebaseService (FCM)
└── util/        # Constants, DateUtils, UiState, ApiCall, dll.
```

Detail kontrak API & konvensi: lihat **[CLAUDE.md](CLAUDE.md)**.

## Status

✅ Semua fitur inti (Fase 0–9) sudah diimplementasikan & build hijau.
Sisa pekerjaan: pengujian end-to-end di perangkat dengan backend Laravel berjalan.
