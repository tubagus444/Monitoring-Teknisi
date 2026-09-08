# Aplikasi Monitoring Teknisi — SkyNet RT/RW Net (Android)

Aplikasi Android untuk teknisi lapangan SkyNet RT/RW Net, Kab. Bekasi. Digunakan untuk
menerima tugas perbaikan jaringan, update status pengerjaan, dan mengirim lokasi GPS
secara realtime ke admin.

Proyek ini adalah **klien mobile** dari backend Laravel di repo `Aplikasi-Monitoring`.
Studi kasus skripsi, metode RAD.

## Status Implementasi

✅ Semua fitur inti (Fase 0–9) sudah diimplementasikan & build hijau (`app-debug.apk` terbentuk).
Detail progres per fase ada di `RENCANA_IMPLEMENTASI.md` (arsip); iterasi/fitur **pasca-MVP**
(mis. penyesuaian Modul Pelanggan) dicatat di `PENGEMBANGAN_LANJUTAN.md`. Dokumen ini tetap
menjadi acuan kontrak (API, struktur, konvensi) — bukan catatan progres.

Sisa pekerjaan: pengujian end-to-end di perangkat dengan backend Laravel berjalan.

## Stack Teknologi

| Layer | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose |
| Arsitektur | MVVM (ViewModel + StateFlow) |
| Networking | Retrofit 2 + OkHttp 3 |
| Dependency Injection | Hilt |
| Database Lokal (Cache) | Room (SQLite) |
| Penyimpanan Token | DataStore Preferences |
| Push Notifikasi | Firebase Cloud Messaging (FCM) |
| GPS | FusedLocationProviderClient (Foreground Service) |
| Navigasi | Navigation Compose |
| Image Loading | Coil |

## Perintah Umum

> **Product flavors:** ada 2 flavor environment — `emulator` (BASE_URL `10.0.2.2`) & `device`
> (BASE_URL dari `local.properties` key `deviceBaseUrl`, untuk HP fisik di WiFi). Nama task jadi
> ber-flavor: `assembleEmulatorDebug`, `compileEmulatorDebugKotlin`, `testEmulatorDebugUnitTest`,
> dst. Pilih varian di Android Studio via panel **Build Variants**.

```bash
# Build debug APK (pilih flavor)
./gradlew assembleEmulatorDebug      # untuk emulator
./gradlew assembleDeviceDebug        # untuk HP fisik

# Run unit test (cukup satu flavor — kode sama)
./gradlew testEmulatorDebugUnitTest

# Run instrumented test
./gradlew connectedEmulatorDebugAndroidTest

# Lint
./gradlew lint
```

> **Build di Windows (PowerShell):** gunakan `.\gradlew.bat`, contoh `.\gradlew.bat :app:assembleEmulatorDebug`.
> Untuk verifikasi cepat kode + Hilt cukup `.\gradlew.bat :app:compileEmulatorDebugKotlin --console=plain`
> (~20 dtk, skip dexing/packaging). `assemble*Debug` hanya saat butuh APK utuh.
>
> ⚠️ **JANGAN pakai `--no-daemon`** — di AGP 9 / Gradle 9 / Windows kombinasinya **hang di
> `kspDebugKotlin`** (proses java idle, log berhenti). Pakai daemon biasa (default). Kalau
> terlanjur nyangkut: `Stop-Process -Id <pid java> -Force` lalu build ulang.

### Catatan kompatibilitas versi (AGP 9.x / Kotlin 2.2)

Proyek memakai AGP 9.2.1 + Kotlin 2.2.10 + built-in Kotlin. Dua syarat wajib:
- **Hilt ≥ 2.59** (2.58 ke bawah hanya support AGP 8 → error "Android BaseExtension not found").
- **`android.disallowKotlinSourceSets=false`** di `gradle.properties` (built-in Kotlin AGP 9
  melarang KSP mendaftarkan generated source via `kotlin.sourceSets`).

## Konfigurasi Environment

### Base URL API

Backend Laravel berjalan di `http://localhost:8000` (Laragon, Windows). Android tidak bisa
akses `localhost` langsung. `BASE_URL` **tidak hardcoded di Kotlin** — disuntik per product
flavor via `buildConfigField` di `app/build.gradle.kts`, lalu dikonsumsi di `NetworkModule`
sebagai `BuildConfig.BASE_URL`:

| Flavor | BASE_URL | Sumber |
|---|---|---|
| `emulator` | `http://10.0.2.2:8000/api/` | hardcoded di `build.gradle.kts` |
| `device` | IP LAN laptop (mis. `http://192.168.x.x:8000/api/`) | `local.properties` key `deviceBaseUrl` |

```kotlin
// NetworkModule.kt — konsumsi nilai dari flavor
Retrofit.Builder().baseUrl(BuildConfig.BASE_URL)
```

> **Override alamat server saat tes (DEBUG saja).** Agar tak perlu rebuild tiap ganti IP/jaringan,
> layar Login menampilkan field **"Alamat Server"** (mis. `192.168.0.105:8000` atau
> `https://api.skynet.id`). Nilainya disimpan di DataStore (key `server_url`) dan
> `ServerUrlInterceptor` menimpa **skema/host/port** tiap request (path `/api/...` dari `BASE_URL`
> tetap). Berlaku langsung tanpa restart; kosongkan field → kembali ke `BuildConfig.BASE_URL`.
> Field + interceptor **hanya aktif di build debug** (`BuildConfig.DEBUG`) — build release selalu
> pakai `BASE_URL` resmi & tak bisa ditimpa. `UserPreferences.clear()` (logout) sengaja **tidak**
> menghapus `server_url`. Normalisasi input ada di `normalizeServerUrl()`.

### Firebase

- Download `google-services.json` dari Firebase Console (project yang sama dengan backend)
- Letakkan di folder `app/`
- `google-services.json` **jangan di-commit** — tambahkan ke `.gitignore`

## Struktur Package

```
app/src/main/java/com/skynet/monitoring/
├── MonitoringApp.kt             # Application (@HiltAndroidApp)
├── MainActivity.kt              # Single-activity host Compose
├── di/
│   ├── NetworkModule.kt         # Retrofit, OkHttp, ApiService
│   └── RepositoryModule.kt      # Binding interface → implementasi repository
├── data/
│   ├── api/
│   │   ├── ApiService.kt        # Semua endpoint Retrofit (interface)
│   │   ├── AuthInterceptor.kt   # Sisipkan header Authorization + Accept
│   │   ├── ServerUrlInterceptor.kt # (DEBUG) Override host/port server dari field Login
│   │   └── model/               # Data class request & response
│   │       ├── AuthModels.kt
│   │       ├── TaskModels.kt
│   │       ├── TaskStatus.kt    # Enum status tugas
│   │       ├── TaskCategory.kt  # Enum kategori tugas (pelanggan/jaringan/pemeliharaan) + label
│   │       ├── LocationModels.kt
│   │       ├── NotificationModels.kt
│   │       └── ErrorResponse.kt # Bentuk umum body error backend
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── TaskRepository.kt
│   │   ├── LocationRepository.kt
│   │   └── NotificationRepository.kt
│   └── local/
│       └── UserPreferences.kt   # DataStore: simpan token + data user
├── ui/
│   ├── BaseViewModel.kt         # Event sekali-pakai (events: Flow<E>)
│   ├── MainViewModel.kt         # State global (token/sesi) untuk routing awal
│   ├── navigation/
│   │   ├── AppNavGraph.kt       # NavHost
│   │   └── Routes.kt            # Definisi semua route
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── LoginScreen.kt
│   │   │   └── LoginViewModel.kt
│   │   ├── tasks/
│   │   │   ├── TaskListScreen.kt
│   │   │   ├── TaskListViewModel.kt
│   │   │   ├── TaskDetailScreen.kt
│   │   │   └── TaskDetailViewModel.kt
│   │   ├── working/
│   │   │   ├── WorkingScreen.kt
│   │   │   └── WorkingViewModel.kt
│   │   ├── notifications/
│   │   │   ├── NotificationScreen.kt
│   │   │   └── NotificationViewModel.kt
│   │   └── profile/
│   │       ├── ProfileScreen.kt
│   │       └── ProfileViewModel.kt
│   ├── components/              # Composable reusable: StatusBadge, CategoryBadge, TaskCard, BrandLogo, StateViews, AddPhotoButton
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
├── service/
│   ├── LocationService.kt       # Foreground Service pengiriman GPS berkala
│   └── MonitoringFirebaseService.kt  # FirebaseMessagingService (FCM)
└── util/
    ├── Constants.kt             # Konstanta GPS (interval, displacement) dll.
    ├── DateUtils.kt             # Format/parse ISO-8601 (toIso8601, format)
    ├── WorkDuration.kt          # Hitung durasi pengerjaan
    ├── UiState.kt               # sealed interface UiState<out T>
    ├── ApiCall.kt               # safeApiCall — wrap network call → Result
    ├── UiStateLoader.kt         # collectResult() — load()/refresh() ke UiState
    └── ImageCompressor.kt       # content:// Uri → JPEG <5MB MultipartBody.Part (EXIF-aware)
```

## Kontrak API Backend

Base URL: `/api/` — semua request kecuali login wajib menyertakan header:
```
Authorization: Bearer {token}
```

---

### Autentikasi

**Login**
```
POST /api/auth/login
Body: { "email": "string", "password": "string" }

Response 200:
{
  "token": "1|xxxxxxxxxxx",
  "user": { "id": 1, "name": "Budi", "email": "budi@skynet.id", "role": "teknisi" }
}

Response 401: { "message": "Email atau password salah" }
Response 422: { "message": "...", "errors": { "email": ["..."] } }   // validasi gagal (field kosong/format salah)
```

> Login hanya bisa dengan akun ber-role `teknisi`. Akun admin tidak bisa masuk via API.
> **Tidak ada endpoint registrasi** — akun teknisi dibuat admin lewat web. App TIDAK punya
> layar daftar/sign-up; jangan men-scaffold-nya.

**Logout**
```
POST /api/auth/logout

Response 200: { "message": "Logout berhasil" }
```

> `logout` hanya menghapus token di sisi server. App **tetap wajib** membersihkan token +
> user dari DataStore secara lokal walau request gagal (mis. offline), lalu arahkan ke Login.

**Update FCM Token**
```
PUT /api/auth/fcm-token
Body: { "fcm_token": "string" }

Response 200: { "message": "FCM token diperbarui" }
```

---

### Tugas

**Daftar Tugas** — hanya tugas milik teknisi yang login, status aktif saja
```
GET /api/tasks

Response 200:
{
  "data": [
    {
      "id": 1,
      "report_id": 5,
      "status": "ditugaskan",
      "category": "pelanggan",
      "headline": "Pak Ahmad",
      "customer": "Pak Ahmad",
      "address": "Jl. Mawar No.3, Cikarang",
      "damage_type": "Kabel Putus",
      "notes": "Sinyal hilang total sejak kemarin",
      "assigned_at": "2025-06-01T08:00:00+07:00",
      "phone": "081234567890",
      "ip_address": "192.168.10.5",
      "subscription_package": "20 Mbps"
    }
  ]
}
```

`status` hanya akan berisi `"ditugaskan"` atau `"sedang_memperbaiki"` di endpoint ini.
Tugas yang sudah `"selesai"` tidak muncul.

> **Kategori tugas (Modul Pelanggan).** Sejak backend punya Modul Pelanggan, tiap tugas ber-`category`:
> `"pelanggan"` | `"jaringan"` | `"pemeliharaan"`.
> - **Judul tampilan: SELALU pakai `headline`** (selalu terisi), JANGAN `customer`.
> - Field pelanggan (`customer`, `phone`, `ip_address`, `subscription_package`) **bisa `null`**
>   pada tugas `jaringan`/`pemeliharaan` — UI wajib aman-null. Di model Kotlin, field-field ini
>   nullable; `Task.displayTitle` (`headline` → `customer` → fallback) jadi judul aman-null terpusat.
> - Label kategori (Indonesia): `pelanggan` → "Gangguan Pelanggan", `jaringan` → "Gangguan
>   Jaringan/Infrastruktur", `pemeliharaan` → "Pemeliharaan" (lihat enum `TaskCategory`).

> **`address` adalah teks biasa, BUKAN koordinat.** Tabel `damage_reports` tidak menyimpan
> lat/lng pelanggan — jadi app TIDAK bisa menampilkan pin pelanggan di peta atau fitur
> "navigasi ke lokasi". GPS yang dikirim app hanya posisi teknisi (untuk dipantau admin).
> Untuk pelanggan `address` = alamat; untuk jaringan/pemeliharaan = lokasi/area terdampak.
> Tampilkan `address` sebagai teks saja (atau buka di Google Maps via intent `geo:0,0?q=<address>`).

**Detail Tugas** — sama dengan list, ditambah `work_logs` & `house_photos`
```
GET /api/tasks/{id}

Response 200:
{
  "data": {
    "id": 1,
    "report_id": 5,
    "status": "ditugaskan",
    "category": "pelanggan",
    "headline": "Pak Ahmad",
    "customer": "Pak Ahmad",
    "address": "Jl. Mawar No.3, Cikarang",
    "damage_type": "Kabel Putus",
    "notes": "Sinyal hilang total",
    "assigned_at": "2025-06-01T08:00:00+07:00",
    "phone": "081234567890",
    "ip_address": "192.168.10.5",
    "subscription_package": "20 Mbps",
    "house_photos": [
      "http://10.0.2.2:8000/storage/customer-photos/demo.jpg"
    ],
    "repair_photos": [
      {
        "url": "http://10.0.2.2:8000/storage/report-photos/xyz.jpg",
        "caption": "Kondisi sesudah",
        "technician": "Budi",
        "uploaded_at": "2025-06-01T10:15:00+07:00"
      }
    ],
    "work_logs": [
      {
        "status": "ditugaskan",
        "technician": "Budi",
        "description": null,
        "logged_at": "2025-06-01T08:00:00+07:00"
      }
    ]
  }
}
```

> **`house_photos` hanya ada di endpoint DETAIL** (tidak di list). Array URL **absolut** foto
> rumah pelanggan; `[]` untuk non-pelanggan / belum ada foto. URL mengikuti host API — muat apa
> adanya via Coil (jangan susun manual; asalkan `BASE_URL` benar, URL foto otomatis benar).
> Karena di-serve via HTTP biasa dari folder `storage` backend, butuh `usesCleartextTraffic="true"`
> (sudah aktif). Detail tugas **adaptif per kategori**: `pelanggan` → tampilkan kartu kontak
> (customer, phone dgn aksi telepon/WhatsApp, ip_address, subscription_package) + galeri
> `house_photos`; `jaringan`/`pemeliharaan` → sembunyikan kartu & galeri (cukup headline + address).

> **`repair_photos` (DETAIL saja).** Array **objek** `{url, caption, technician, uploaded_at}` —
> foto **bukti hasil kerja**, berlaku semua kategori (beda dari `house_photos` yang hanya URL string
> & khusus pelanggan). Urut naik waktu unggah; `[]` bila kosong. `caption`/`uploaded_at`/`technician`
> bisa `null`. Di model Kotlin: `RepairPhoto`. Tampilkan sebagai galeri (caption di bawah thumbnail)
> di Detail Tugas. Diunggah lewat endpoint foto di bawah; setelah unggah sukses, app **memuat ulang**
> detail agar galeri ter-refresh.

**Update Status Tugas**
```
POST /api/tasks/{id}/status
Body: { "status": "in_progress", "description": "Ganti konektor RJ45 & rapikan kabel." }

Response 200: { "message": "Status diperbarui", "status": "sedang_memperbaiki" }
Response 422: { "message": "Perubahan status tidak valid dari status saat ini" }
```

> **Penting — Status Mapping (hidden contract):**
>
> | Android kirim  | Database menyimpan    |
> |----------------|-----------------------|
> | `in_progress`  | `sedang_memperbaiki`  |
> | `done`         | `selesai`             |
> | —              | `ditugaskan`          |
>
> Transisi **hanya searah**: `ditugaskan` → `in_progress` → `done`.
> Loncat atau mundur akan dibalas `422`.

> **`description` (opsional, maks 1000 char) = catatan pekerjaan.** Boleh dikirim bersama
> `in_progress` atau `done` (paling relevan saat `done` sebagai rangkuman hasil). Kosong/blank →
> di-omit (field `null`, tetap sukses). Tersimpan di `work_logs[].description`. App mengirimnya dari
> dialog "Selesaikan Tugas" di layar Working (field multiline). **Idempotent:** bila status sudah
> sama (tak ada transisi), server balas `200` tanpa membuat work log baru → `description` tidak
> tersimpan; **perlakukan sebagai sukses**, bukan error.

**Unggah Foto Bukti Pekerjaan** — semua kategori, boleh berkali-kali (append)
```
POST /api/tasks/{id}/photos
Content-Type: multipart/form-data
  photo   = <file gambar>   (WAJIB, jpg/png, maks 5 MB)
  caption = "Kondisi sesudah" (opsional, maks 255)

Response 201: { "message": "...", "data": { "id": 12, "url": "...", "caption": "..." } }
Response 422: bukan gambar / > 5 MB
Response 404: tugas bukan milik teknisi ini
```

**Unggah Foto Rumah Pelanggan** — HANYA kategori `pelanggan`
```
POST /api/tasks/{id}/house-photos
Content-Type: multipart/form-data
  photo   = <file gambar>   (WAJIB, maks 5 MB)
  caption = "Tampak depan"  (opsional, maks 255)

Response 201: bentuk sama dgn endpoint foto bukti
Response 422: "bukan laporan pelanggan, tidak ada rumah untuk difoto" (tugas non-pelanggan)
```

> **Kedua endpoint foto = `multipart/form-data`, bukan JSON. Nama part file WAJIB `photo`.**
> Hasil muncul kembali di detail: foto bukti → `repair_photos`; foto rumah → `house_photos`.
> - Tombol unggah: foto **bukti** di layar Detail **dan** Working (semua kategori); foto **rumah**
>   hanya di Detail saat `category == "pelanggan"`.
> - Sumber foto: **kamera** (ACTION_IMAGE_CAPTURE via `FileProvider`, otoritas
>   `${applicationId}.fileprovider`, tanpa permission `CAMERA`) **atau galeri** (Photo Picker, tanpa
>   izin). Komponen reusable: `ui/components/AddPhotoButton`.
> - **Pratinjau sebelum kirim:** setelah ambil/pilih foto, `AddPhotoButton` menampilkan dialog
>   pratinjau (thumbnail + kolom **"Keterangan (opsional)"**, maks 255) — upload baru jalan saat
>   "Kirim", "Batal" membuang foto tanpa mengunggah. Callback `onImagePicked: (Uri, String?) ->`
>   mengoper caption (null bila kosong) hingga ke `@Part("caption")`. Ini mencegah salah foto
>   terlanjur terunggah (belum ada endpoint hapus foto di backend — upload bersifat append).
> - Foto **dikompres dulu** ke JPEG < 5 MB (down-sample + koreksi EXIF) di `util/ImageCompressor`
>   sebelum dikirim — jangan kirim file mentah (bisa > 5 MB → 422).
> - Tangani `422`/`404` terpisah dari error jaringan (pakai `message` dari backend via `safeApiCall`).

---

### GPS

**Kirim Koordinat Lokasi**
```
POST /api/location
Body: { "report_id": 5, "latitude": -6.2631, "longitude": 107.0059, "recorded_at": "2026-06-10T14:05:30+07:00" }

Response 200: { "message": "Lokasi dicatat" }
Response 403: { "message": "Tidak dapat mengirim lokasi — laporan tidak aktif atau bukan tugas Anda" }
```

Backend hanya menerima koordinat jika status laporan `sedang_memperbaiki` dan `report_id`
memang milik teknisi yang sedang login. Gunakan `report_id` (bukan `task id`) saat kirim.

> **`recorded_at` (opsional, ISO-8601 + offset zona).** Diisi dari **waktu fix GPS sebenarnya**
> (`location.time`), bukan waktu kirim — agar akurat walau sinyal teknisi lemah/tertunda. App
> memformatnya via `SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")` (kompatibel minSdk 24, tanpa
> desugaring). Backward-compatible: bila tidak dikirim, server pakai waktu terima.

---

### Notifikasi

**Daftar Notifikasi**
```
GET /api/notifications

Response 200:
{
  "data": [
    {
      "id": 1,
      "title": "Tugas Baru",
      "body": "Anda ditugaskan ke laporan Pak Ahmad di Jl. Mawar No.3",
      "is_read": false,
      "created_at": "2025-06-01T08:00:00+07:00"
    }
  ]
}
```

**Tandai Sudah Dibaca**
```
PUT /api/notifications/{id}/read

Response 200: { "message": "Notifikasi ditandai sudah dibaca" }
```

> Tidak ada endpoint "jumlah belum dibaca". Hitung badge sendiri di client dari list:
> `data.count { !it.isRead }`.

---

## Layar & Alur Navigasi

```
[Login]
   ↓
[Daftar Tugas] ←——— Bottom Nav ———→ [Notifikasi]
      ↓                                   |
[Detail Tugas]           [Profil] ←———————
      ↓
[Sedang Memperbaiki]  ← GPS Foreground Service aktif di sini
```

| # | Layar | Route | Keterangan |
|---|---|---|---|
| 1 | Login | `login` | Form email + password. Simpan token ke DataStore. Kirim FCM token setelah login berhasil. |
| 2 | Daftar Tugas | `tasks` | List tugas aktif. Tab utama bottom navigation. Pull-to-refresh + auto-refresh tiap `ON_RESUME` (agar tugas baru muncul tanpa restart app). |
| 3 | Detail Tugas | `tasks/{id}` | Info lengkap (judul = `headline`), badge kategori, timeline work logs, galeri `repair_photos` + tombol unggah foto bukti (semua kategori), tombol "Mulai Memperbaiki" / "Selesai". **Adaptif**: kategori `pelanggan` → kartu kontak (telepon/WhatsApp) + galeri `house_photos` + tombol unggah foto rumah; non-pelanggan → tanpa kartu/galeri rumah. |
| 4 | Sedang Memperbaiki | `tasks/{id}/working` | Layar aktif saat GPS berjalan. Tampilkan nama pelanggan, alamat, timer durasi, status live, tombol unggah foto bukti. Tombol "Tandai Selesai" → dialog dgn field catatan pekerjaan opsional. |
| 5 | Notifikasi | `notifications` | Daftar notifikasi terbaru, indikator belum-baca. Tap → tandai dibaca. |
| 6 | Profil | `profile` | Nama + email user. Tombol logout. |

### Logika Tombol di Detail Tugas

- Status `ditugaskan` → tampilkan tombol **"Mulai Memperbaiki"** → kirim `in_progress` → start `LocationService` → navigasi ke `working`
- Status `sedang_memperbaiki` → tampilkan tombol **"Tandai Selesai"** di layar Working → buka dialog (catatan pekerjaan opsional) → kirim `done` (+ `description`) → stop `LocationService` → kembali ke Daftar Tugas

## GPS Foreground Service

`LocationService` berjalan sepanjang teknisi berstatus `sedang_memperbaiki`:

- **Start**: `LocationService.start()` dipanggil dari `WorkingScreen` (di `LaunchedEffect`) setelah
  task termuat & izin lokasi diberikan. Status sudah `in_progress` — diubah di
  `TaskDetailViewModel.startRepair` sebelum navigasi ke layar Working.
- **Interval**: kirim lokasi setiap **15 detik** via `FusedLocationProviderClient`
  (web admin polling peta tiap 10 detik — 15s adalah kompromi antara realtime & hemat baterai)
- **Akurasi & hemat baterai**: `LocationRequest` pakai `PRIORITY_BALANCED_POWER_ACCURACY`
  (~100 m, cukup untuk peta admin — JANGAN `HIGH_ACCURACY`, paling boros), interval 15 dtk /
  tercepat 10 dtk (`setMinUpdateIntervalMillis`), dan **displacement 15 m**
  (`setMinUpdateDistanceMeters`). Konsekuensi penting: **saat teknisi diam, lokasi TIDAK dikirim**
  (di bawah ambang 15 m → GMS menahan delivery) — ini disengaja untuk hemat baterai, bukan bug.
  Konstanta ada di `util/Constants.kt` (`LOCATION_INTERVAL_MS`, `LOCATION_FASTEST_INTERVAL_MS`,
  `LOCATION_MIN_DISPLACEMENT_M`).
- **Payload**: POST ke `/api/location` dengan `report_id` tugas aktif (+ `recorded_at` dari waktu fix GPS)
- **Stop**: `LocationService.stop()` dipanggil dari `WorkingScreen` setelah
  `WorkingViewModel.finishRepair()` berhasil mengubah status ke `done` (`finished = true`). Selain itu,
  bila server membalas **403** (laporan tidak aktif / bukan tugas ini), service **berhenti sendiri**
  (`stopSelf()` di callback) — pengiriman GPS dihentikan total tanpa menunggu aksi user.
- **Persistent notification** wajib ditampilkan selama service aktif (requirement Android 8+):
  contoh teks: "SkyNet — GPS aktif • Sedang memperbaiki"

Permission yang wajib dideklarasikan di `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Runtime permission `ACCESS_FINE_LOCATION` (semua versi) diminta sebelum memulai service —
yaitu di layar Working sebelum `LocationService.start()`.

> **`POST_NOTIFICATIONS` (Android 13+) TIDAK menunggu GPS.** Izin ini diminta lebih awal saat
> teknisi masuk **Daftar Tugas** (`TaskListScreen`), supaya notifikasi FCM tugas baru bisa
> tampil walau teknisi belum pernah membuka layar Working. Bila izin ini belum diberikan,
> `NotificationManager.notify()` menjadi no-op diam-diam di Android 13+ — notifikasi senyap.

## Firebase FCM

**Registrasi token:**
- `FirebaseMessaging.getInstance().token` untuk mendapatkan token perangkat
- Kirim token ke backend via `PUT /api/auth/fcm-token` **segera setelah login berhasil**
- Override `onNewToken` di `FirebaseMessagingService` → kirim ulang token ke backend jika sudah login

**Struktur pesan (baseline — yang dikirim backend saat ini):**

Backend (`NotificationObserver`) mengirim pesan **notification-only** (tanpa blok `data`):
```json
{ "notification": { "title": "Tugas Baru", "body": "Anda ditugaskan ke ..." } }
```

Perilaku `onMessageReceived` berbeda tergantung state app — ini bawaan FCM, tidak bisa diubah:

| State App | `onMessageReceived` dipanggil? | Yang terjadi |
|---|---|---|
| **Foreground** | ✅ Ya | App tampilkan notif sendiri via `NotificationManager` (baca `title`/`body` + `data["related_id"]` untuk deep-link) |
| **Background / killed** | ❌ Tidak | Sistem tampilkan notif otomatis dari blok `notification`; tap → `MainActivity` baca `intent.extras` untuk deep-link |

**Deep-link dari notifikasi (sudah diimplementasi):**
- Backend mengirim blok `data` (`type`, `related_id`) bersama blok `notification` di FCM payload.
- **Foreground**: `MonitoringFirebaseService` baca `remoteMessage.data["related_id"]`, bangun
  `PendingIntent` dengan extra `EXTRA_RELATED_ID`. Tap notifikasi → `MainActivity` → navigasi
  ke detail tugas.
- **Background/killed**: Sistem menampilkan notif otomatis dari blok `notification`. Tap →
  `MainActivity.onCreate` baca `intent.extras["fcm_related_id"]` → simpan di `deepLinkReportId`
  StateFlow → `AppNavGraph` consume dan navigasi ke detail tugas.
- **Dalam layar Notifikasi**: Klik notifikasi dengan `relatedId` → `NotificationViewModel.openNotification()`
  resolve `report_id` → `task_id` via `TaskRepository.getTasks()` → emit `NavigateToTask` event
  → navigasi ke `Tasks/{id}`.
- `related_id` dari backend adalah **report_id**, bukan **task assignment id**. Resolusi
  `report_id → task_id` dilakukan di klien via filter `tasks.firstOrNull { it.reportId == reportId }`.
- Daftar notifikasi lengkap tetap bisa dilihat kapan saja di layar Notifikasi (`GET /api/notifications`).

## DataStore — UserPreferences

Simpan hanya dua key:

```kotlin
val TOKEN_KEY = stringPreferencesKey("auth_token")
val USER_KEY  = stringPreferencesKey("user_json")  // JSON dari object User login
```

- `token == null` → arahkan ke layar Login
- Baca sebagai `Flow` agar reaktif; collect di ViewModel
- Hapus keduanya saat logout

## Room Database — Cache Lokal Offline

Aplikasi menyimpan cache lokal untuk tugas (`tasks`) dan notifikasi (`notifications`) di database SQLite lokal via Room (`monitoring_db`):

- **Entitas:** `TaskEntity` (termasuk TypeConverter Gson untuk `work_logs`, `house_photos`, `repair_photos`) dan `NotificationEntity`.
- **DAOs:** `TaskDao` (`syncTasks`, `syncTaskDetail`, `getTasks`, `getTaskById`) dan `NotificationDao` (`syncNotifications`, `getNotifications`, `markAsRead`).
- **Strategi Cache (Network-First with Local Fallback):**
  - Pemanggilan API sukses → simpan/sinkronkan ke Room secara otomatis.
  - Pemanggilan API gagal karena offline / jaringan terputus (`IOException` / status 5xx) → ambil data terakhir dari cache Room.
  - Error 4xx (mis. 401 Sesi habis, 404 Data tidak ditemukan) **tidak** mengambil cache agar state error bisnis backend tetap diteruskan ke UI.
- **Pembersihan Cache saat Logout:** `AppDatabase.clearAllTables()` dipanggil otomatis di `AuthRepositoryImpl.logout()`.

## OkHttp AuthInterceptor

File: `data/api/AuthInterceptor.kt` (kelas terpisah, di-inject ke OkHttp lewat `NetworkModule`).
Selalu menambahkan header `Accept`, dan menambahkan `Authorization` **hanya bila token ada** —
jangan kirim `Bearer null` saat belum login (endpoint `login` tak boleh membawa bearer basi):

```kotlin
class AuthInterceptor @Inject constructor(
    private val userPreferences: UserPreferences,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { userPreferences.getToken() }
        val builder = chain.request().newBuilder()
            .addHeader("Accept", "application/json")
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
```

## Penanganan Error API

Semua response error dari backend selalu memiliki field `message`. Tampilkan ke user via
Snackbar. HTTP status yang relevan:

| Status | Arti | Tindakan |
|---|---|---|
| `401` | Token tidak valid / kadaluarsa | Hapus token, paksa ke layar Login |
| `403` | Aksi tidak diizinkan (mis. kirim lokasi saat laporan tidak aktif) | Tampilkan `message` |
| `422` (bisnis) | Transisi status tidak valid | Tampilkan `message` dari response |
| `422` (validasi) | Field salah/kosong — ada objek `errors` | Tampilkan pesan dari `errors`, atau `message` |
| `404` | Resource tidak ada **ATAU** tugas bukan milik teknisi ini | Tampilkan pesan error / kembali ke list |
| Network error | Tidak ada koneksi | Tampilkan "Periksa koneksi internet Anda" |

> Catatan: endpoint task & notifikasi memakai `findOrFail` yang di-scope ke `technician_id`
> pemilik. Meminta tugas milik teknisi lain membalas **404** (bukan 403) — perlakukan sebagai
> "tugas tidak ditemukan".

## Konvensi Kode

- Satu ViewModel per layar — jangan share ViewModel antar layar kecuali data benar-benar sama
- UI state dimodelkan sebagai `sealed interface UiState<out T>` (`util/UiState.kt`):
  `Loading`, `Success(data: T)`, `Error(message: String)`
- Repository mengembalikan `Result<T>` — wrap semua network call dengan `try/catch`
- Network call di `Dispatchers.IO`, collect di ViewModel dengan `viewModelScope`
- Inject `Context` via `@ApplicationContext` dari Hilt — jangan pegang referensi Activity
- Naming: `*Screen.kt` (Composable root), `*ViewModel.kt`, `*Repository.kt`
- Hindari logic di Composable — semua logic di ViewModel
- **Event sekali-pakai** (navigasi, Snackbar) lewat `BaseViewModel<E>` (`ui/BaseViewModel.kt`) +
  `events: Flow<E>`; Composable mengonsumsi via `LaunchedEffect(Unit) { vm.events.collect { … } }`.
  Jangan pakai `MutableStateFlow<Boolean>` + `consumeX()` (rawan ter-trigger ganda saat recomposition).
- **Pemuatan data** ke `UiState` lewat helper `collectResult()` (`util/UiStateLoader.kt`) untuk pola
  `load()`/`refresh()` (termasuk "pertahankan data lama saat error") — jangan tulis ulang manual.
- Format ISO-8601 (`recorded_at` GPS & tampilan tanggal) terpusat di `util/DateUtils.kt`
  (`toIso8601`, `format`).

## Pengujian

Unit test (host JVM, di `app/src/test/`) memakai JUnit4 + `kotlinx-coroutines-test` + **mockk**
(mock repository/`ApiService`) + **turbine** (assert `Flow`/`events`). `MainDispatcherRule`
(`util/MainDispatcherRule.kt`) mengganti `Dispatchers.Main` dengan `TestDispatcher`.

- Jalankan: `.\gradlew.bat :app:testEmulatorDebugUnitTest --console=plain`.
- Uji ViewModel: `runTest(mainDispatcherRule.dispatcher) { … }` agar scheduler dibagi dengan
  `viewModelScope`; uji `events` dengan turbine; panggil `vm.viewModelScope.cancel()` di `finally`
  bila VM punya coroutine menetap (timer / `stateIn`), supaya `runTest` selesai bersih.
- Cakupan saat ini: semua repository (termasuk cache offline Room, catatan `description`, & unggah foto bukti/rumah) +
  `safeApiCall` + `DateUtils` + `WorkDuration` + semua ViewModel (68 test).
- **Titik buta:** `ImageCompressor` TIDAK punya unit test — bergantung pada `BitmapFactory`/`ExifInterface`
  framework Android yang tak tersedia di JVM, jadi tak bisa diuji di `src/test/`. Test repository yang
  menyentuh upload foto **me-mock** `ImageCompressor`, sehingga logika decode/kompres asli tak pernah
  dijalankan unit test. Verifikasi perubahan di sini lewat perangkat/instrumented test — jangan anggap
  "hijau di unit test" berarti kompresi foto benar (bug dekode pernah lolos justru di sini).
