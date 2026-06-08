# Rencana Implementasi — Aplikasi Monitoring Teknisi (SkyNet)

> Dokumen acuan pengerjaan. Klien Android untuk backend Laravel `Aplikasi-Monitoring`.
> Studi kasus skripsi, metode RAD. Acuan kontrak API & struktur: lihat `CLAUDE.md`.

## Keputusan yang Sudah Dikonfirmasi

| Topik | Keputusan | Alasan |
|---|---|---|
| **Package name** | `com.skynet.monitoring` | Wajib — cocok dengan `google-services.json` (terdaftar di Firebase sebagai `com.skynet.monitoring`). |
| **Firebase / google-services** | Plugin penuh, file sudah ada | `app/google-services.json` sudah dicopy & terverifikasi. |
| **Backend Laravel** | Tidak terpengaruh rename | Package name murni internal Android; kontrak API tetap sama. |
| **Deep-link FCM** | Ditunda (tidak dikerjakan) | Sesuai catatan CLAUDE.md — hindari risiko bug sebelum fitur inti stabil. |

### Detail Firebase (terverifikasi dari `google-services.json`)
- Project ID: `skynet-monitoring`
- Project number: `162246082763`
- Package terdaftar: `com.skynet.monitoring`

---

## FASE 0 — Rename Package ke `com.skynet.monitoring`

Langkah pertama. Kalau dilewati, build gagal: *"No matching client found for package name ..."*.

1. **`app/build.gradle.kts`** — ubah `namespace` & `applicationId`: `com.example.monitoring_teknisi` → `com.skynet.monitoring`.
2. **Pindah folder**: `app/src/main/java/com/example/monitoring_teknisi/` → `app/src/main/java/com/skynet/monitoring/`. Sama untuk `test/` & `androidTest/`.
3. **Update package declaration** di `MainActivity.kt`, `ui/theme/*.kt`, dan 2 file test.
4. **`settings.gradle.kts`** — `rootProject.name` boleh tetap `"Monitoring-Teknisi"` (tidak berpengaruh ke package).

## FASE 1 — Fondasi Build

**`gradle/libs.versions.toml`** — tambah versi & library:
- Hilt `2.52` + plugin, **KSP** (wajib, bukan kapt, karena Kotlin 2.2)
- Retrofit `2.11`, OkHttp `4.12` (+ logging-interceptor), converter-gson
- DataStore `1.1.x`
- Firebase BoM `33.x` + `firebase-messaging`, google-services plugin
- Play Services Location `21.3.x`
- Navigation Compose `2.8.x`
- Coil `2.7.x`
- lifecycle-viewmodel-compose, lifecycle-runtime-compose, kotlinx-coroutines
- hilt-navigation-compose, material-icons-extended

> ⚠️ **Catatan versi bleeding-edge**: AGP 9.2.1 / compileSdk 36 / Compose BoM 2026.02 sangat baru.
> Saat build pertama mungkin perlu menyesuaikan (menurunkan) versi Hilt/KSP agar kompatibel.

**`build.gradle.kts` (root)** — daftarkan plugin Hilt, KSP, google-services (apply false).

**`app/build.gradle.kts`** — apply plugin hilt/ksp/google-services; tambah semua dependency;
`buildFeatures { compose = true; buildConfig = true }`; `BASE_URL` via `buildConfigField`
(debug = `http://10.0.2.2:8000/api/`).

**`google-services.json`** → sudah di `app/`. Tambahkan ke `.gitignore`.

## FASE 2 — Application & Manifest

- **`MonitoringApp.kt`** — `@HiltAndroidApp`, buat notification channel (GPS service + FCM).
- **`AndroidManifest.xml`**:
  - Permissions: `INTERNET`, `ACCESS_FINE/COARSE_LOCATION`, `FOREGROUND_SERVICE`,
    `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS`
  - `android:name=".MonitoringApp"` di `<application>`
  - `<service android:name=".service.LocationService" foregroundServiceType="location" />`
  - `<service>` untuk `MonitoringFirebaseService` + meta-data default notification channel
  - `usesCleartextTraffic="true"` (backend `http://`, bukan https) — atau network-security-config
    yang mengizinkan `10.0.2.2`.

## FASE 3 — Data Layer

**`data/api/model/`**
- `AuthModels.kt` — `LoginRequest`, `LoginResponse(token, user)`, `User(id,name,email,role)`,
  `FcmTokenRequest`, `MessageResponse`
- `TaskModels.kt` — `Task`, `TaskListResponse(data)`, `TaskDetailResponse(data)`, `WorkLog`,
  `UpdateStatusRequest`, `UpdateStatusResponse`. Pakai `@SerializedName` untuk `report_id`,
  `damage_type`, `assigned_at`, `is_read`, `logged_at`, dll.
- `LocationModels.kt` — `LocationRequest(report_id, latitude, longitude)`
- `NotificationModels.kt` — `NotificationItem`, `NotificationListResponse`
- `ErrorResponse.kt` — `message` + `errors: Map<String,List<String>>?` (parsing 422)

**`data/api/ApiService.kt`** — interface Retrofit semua endpoint (login, logout, fcm-token,
tasks, task detail, status, location, notifications, read).

**`data/local/UserPreferences.kt`** — DataStore, 2 key (`auth_token`, `user_json`).
`getToken()`, `getUserFlow()`, `saveSession()`, `clear()`. Serialize `User` via Gson.

**`di/NetworkModule.kt`** — provide Gson, AuthInterceptor, OkHttpClient (+ logging debug),
Retrofit, ApiService.

**`data/api/AuthInterceptor.kt`** — sesuai contoh CLAUDE.md (`runBlocking { getToken() }`),
tambah header `Accept: application/json`.

**`di/RepositoryModule.kt`** — `@Binds` interface→impl (atau `@Provides` jika repo concrete).

## FASE 4 — Repository (semua return `Result<T>`)

Helper **`safeApiCall`** + parser error sentral (map 401/403/404/422/network → pesan).

- `AuthRepository` — login (simpan sesi), logout (selalu clear lokal walau gagal), updateFcmToken
- `TaskRepository` — getTasks, getTaskDetail, updateStatus
- `LocationRepository` — sendLocation
- `NotificationRepository` — getNotifications, markRead

**`util/UiState.kt`** — `sealed class UiState<out T> { Loading; Success(data); Error(message) }`

## FASE 5 — UI: Navigasi & Auth

- **`ui/navigation/AppNavGraph.kt`** — NavHost. Route: `login`, `tasks`, `tasks/{id}`,
  `tasks/{id}/working`, `notifications`, `profile`. Start destination dari `token`
  (null→login). Bottom nav membungkus tasks/notifications/profile.
- **`MainActivity.kt`** — `@AndroidEntryPoint`, set theme + `AppNavGraph`.
- **`LoginScreen.kt` + `LoginViewModel.kt`** — form email/password, `UiState`, sukses → ambil
  FCM token → `PUT fcm-token` → navigasi ke `tasks`. Error 401/422 via Snackbar.

## FASE 6 — UI: Tasks

- **`TaskListScreen/ViewModel`** — list tugas aktif, pull-to-refresh, `TaskCard`, tap → detail.
  401 → balik login.
- **`TaskDetailScreen/ViewModel`** — info + timeline `work_logs`. Tombol kondisional:
  `ditugaskan`→"Mulai Memperbaiki" (kirim `in_progress`, start service, ke working);
  `sedang_memperbaiki`→arahkan ke working. Alamat: tombol buka Google Maps via intent
  `geo:0,0?q=<address>` (alamat = teks, BUKAN koordinat).
- **`WorkingScreen/ViewModel`** — nama/alamat, timer durasi, status live, tombol "Tandai Selesai"
  (kirim `done`, stop service, balik ke `tasks`). Minta runtime permission lokasi + notif
  sebelum start service.

## FASE 7 — LocationService (Foreground GPS)

**`service/LocationService.kt`** — `@AndroidEntryPoint`, inject `LocationRepository`.
`FusedLocationProviderClient`, interval **15 detik**, POST `/api/location` dgn `report_id`
(dikirim via Intent extra). Persistent notification "SkyNet — GPS aktif • Sedang memperbaiki".
Start/stop dipanggil dari WorkingViewModel.

## FASE 8 — Notifikasi & Profil

- **`NotificationScreen/ViewModel`** — list, badge belum-baca = `count { !isRead }`,
  tap → `markRead`.
- **`ProfileScreen/ViewModel`** — nama/email dari DataStore, tombol logout (clear lokal + login).
- **`ui/components/`** — `StatusBadge`, `TaskCard`, `LoadingIndicator`, `ErrorView`.

## FASE 9 — FCM

**`service/MonitoringFirebaseService.kt`** — `@AndroidEntryPoint`. `onMessageReceived`
(foreground) → tampilkan notif via `NotificationManager`. `onNewToken` → jika ada token login,
kirim `PUT /api/auth/fcm-token`. **Deep-link sengaja TIDAK dikerjakan**. Tap notif → buka `tasks`.

---

## Ringkasan Output (±35 file)

| Kategori | Jumlah file |
|---|---|
| Gradle/config | 4 (toml, 2 gradle, manifest) |
| App/util | 3 (MonitoringApp, UiState, error util) |
| Data (model/api/local/di) | ~12 |
| Repository | 4 |
| UI (screens+VM+nav+components+theme) | ~16 |
| Service | 2 |

## Risiko & Mitigasi

1. **Kompatibilitas versi** (AGP 9 / Kotlin 2.2 / Hilt / KSP) — mitigasi: build inkremental,
   turunkan versi bila konflik.
2. **`runBlocking` di AuthInterceptor** — sesuai CLAUDE.md, dipertahankan (acceptable skala skripsi).
3. **Cleartext HTTP** ke `10.0.2.2` — wajib `usesCleartextTraffic`, kalau tidak request gagal
   diam-diam.
4. **google-services plugin** — build gagal kalau `google-services.json` belum ada di `app/`
   (sudah ada ✔️).

## Catatan Build (terselesaikan)

Saat verifikasi build pertama (AGP 9.2.1 / Gradle 9.4.1 / Kotlin 2.2.10), dua isu versi
bleeding-edge muncul & sudah diperbaiki:
1. **Hilt 2.57.2 → 2.59.2** — Hilt ≤2.58 hanya support AGP 8 ("Android BaseExtension not
   found"). 2.59+ wajib untuk AGP 9.
2. **`android.disallowKotlinSourceSets=false`** ditambah di `gradle.properties` — built-in
   Kotlin AGP 9 melarang KSP mendaftarkan generated source via `kotlin.sourceSets`.
   KSP `2.2.10-2.0.2` sudah benar (versi terbaru untuk Kotlin 2.2.10).

Hasil: `BUILD SUCCESSFUL`, `app-debug.apk` (±20.7 MB) terbentuk. `processDebugGoogleServices`
sukses → `google-services.json` cocok dgn package `com.skynet.monitoring`.

## Status Pengerjaan

- [x] Fase 0 — Rename package ✔️
- [x] Fase 1 — Fondasi build (gradle/deps) ✔️ build hijau
- [x] Fase 2 — Application & Manifest ✔️
- [x] Fase 3 — Data layer ✔️
- [x] Fase 4 — Repository ✔️ (compileDebugKotlin hijau)

> ⚠️ Build CLI: JANGAN `--no-daemon` (hang di kspDebugKotlin ~45 mnt). Pakai daemon biasa:
> `.\gradlew.bat :app:compileDebugKotlin --console=plain` (verifikasi cepat ~20 dtk).
- [ ] Fase 5 — Navigasi & Auth
- [ ] Fase 6 — Tasks (list/detail/working)
- [ ] Fase 7 — LocationService
- [ ] Fase 8 — Notifikasi & Profil
- [ ] Fase 9 — FCM
