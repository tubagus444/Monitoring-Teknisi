# Aplikasi Monitoring Teknisi — SkyNet RT/RW Net

Aplikasi Android untuk **teknisi lapangan** SkyNet RT/RW Net (Kab. Bekasi). Teknisi memakai
aplikasi ini untuk menerima tugas perbaikan jaringan, memperbarui status pengerjaan, dan
mengirim **lokasi GPS secara realtime** ke admin selama proses perbaikan berlangsung.

Aplikasi ini adalah **klien mobile** dari backend Laravel (repo `Aplikasi-Monitoring`).
Dikerjakan sebagai studi kasus skripsi dengan metode pengembangan **RAD**.

> 📄 Dokumentasi lengkap:
> - **[CLAUDE.md](CLAUDE.md)** — kontrak teknis (API, struktur, konvensi kode)
> - **[CARA_KERJA_APLIKASI.md](CARA_KERJA_APLIKASI.md)** — penjelasan alur aplikasi (bahasa awam, untuk sidang)
> - **[RENCANA_IMPLEMENTASI.md](RENCANA_IMPLEMENTASI.md)** — arsip rencana & progres per fase

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

### 2. Base URL backend (product flavors)
Backend Laravel berjalan di `http://localhost:8000`. Karena Android tidak bisa mengakses
`localhost` perangkat host, `BASE_URL` disuntik per **product flavor**:

| Flavor | BASE_URL | Sumber |
|---|---|---|
| `emulator` | `http://10.0.2.2:8000/api/` | hardcoded di `app/build.gradle.kts` |
| `device` | IP LAN laptop (mis. `http://192.168.x.x:8000/api/`) | `local.properties`, key `deviceBaseUrl` |

Untuk uji di HP fisik, tambahkan baris ini ke `local.properties` (tidak ikut commit):
```properties
deviceBaseUrl=http://192.168.x.x:8000/api/
```

## Build & Run

> Windows (PowerShell): gunakan `.\gradlew.bat`. Pilih **Build Variants** di Android Studio
> untuk berpindah flavor `emulator` / `device`.

```bash
# Build debug APK
./gradlew assembleEmulatorDebug      # untuk emulator
./gradlew assembleDeviceDebug        # untuk HP fisik

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
