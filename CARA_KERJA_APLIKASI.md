# Cara Kerja Aplikasi Monitoring Teknisi

Dokumen ini menjelaskan **bagaimana aplikasi ini bekerja dari awal sampai akhir**, ditulis
dengan bahasa sederhana. Tujuannya supaya siapa pun (termasuk yang bukan programmer) bisa
memahami alur aplikasi — cocok untuk bahan presentasi/sidang skripsi.

> Dokumen lain: `CLAUDE.md` = kontrak teknis (API, aturan), `RENCANA_IMPLEMENTASI.md` =
> catatan progres pengerjaan. Dokumen **ini** fokus pada *alur & cara kerja*.

---

## 1. Aplikasi ini untuk apa?

Bayangkan SkyNet RT/RW Net punya banyak pelanggan internet. Kadang ada gangguan (kabel putus,
sinyal hilang, dll). Admin di kantor menerima laporan, lalu **menugaskan seorang teknisi** untuk
datang memperbaiki.

Aplikasi ini adalah **alat kerja si teknisi di lapangan**. Dengan aplikasi ini, teknisi bisa:

1. **Melihat daftar tugas** yang diberikan admin (siapa pelanggannya, di mana, kerusakan apa).
2. **Menandai progres** pekerjaan: "mulai memperbaiki" lalu "selesai".
3. **Mengirim lokasi GPS** secara otomatis selama bekerja, supaya admin bisa memantau posisi
   teknisi di peta secara realtime.
4. **Menerima notifikasi** saat ada tugas baru.

Jadi aplikasi ini adalah **jembatan antara teknisi di lapangan dengan admin di kantor**.

---

## 2. Aplikasi ini tidak bekerja sendirian

Ini bagian yang penting dipahami. Aplikasi Android ini **bukan aplikasi yang berdiri sendiri** —
ia hanya satu dari dua bagian:

```
┌─────────────────────┐         internet          ┌──────────────────────────┐
│  APLIKASI ANDROID    │ ◄──────────────────────► │   BACKEND (server) di      │
│  (yang ini)          │     kirim & ambil data    │   kantor — Laravel         │
│  dipegang TEKNISI    │                           │   + database + web admin   │
└─────────────────────┘                           └──────────────────────────┘
```

- **Aplikasi Android (proyek ini)** = aplikasi di HP teknisi. Ia *tidak menyimpan data utama*.
  Ia hanya **menampilkan** data dan **mengirim** aksi.
- **Backend (server Laravel)** = "otak" dan "gudang data". Semua data tugas, pelanggan, akun,
  riwayat lokasi disimpan di sini (di database). Admin mengelola semua lewat web admin.

Analogi: Android = **kasir di toko** (melayani, menampilkan, mencatat). Backend = **gudang +
pembukuan pusat**. Kasir tidak menyimpan stok sendiri; ia selalu tanya/lapor ke pusat.

Cara keduanya "ngobrol" disebut **API**. Aplikasi mengirim permintaan (request) seperti
"tolong kasih daftar tugas saya" atau "ubah status tugas ini jadi selesai", dan backend
menjawab. Semua aturan komunikasi ini ada di `CLAUDE.md` bagian "Kontrak API Backend".

---

## 3. Bagaimana data mengalir di dalam aplikasi (arsitektur)

Di dalam aplikasi Android-nya sendiri, kode tidak ditumpuk jadi satu. Ia dibagi jadi
**beberapa lapisan**, masing-masing punya tugas berbeda. Ini disebut pola **MVVM**.

Bayangkan alurnya seperti rantai dari atas (yang dilihat user) ke bawah (yang bicara ke server):

```
   Yang DILIHAT & DISENTUH teknisi
   ┌──────────────────────────────────────┐
   │  1. SCREEN (tampilan/Composable)      │   contoh: LoginScreen, TaskListScreen
   │     tombol, teks, daftar              │
   └───────────────┬──────────────────────┘
                   │  user menekan tombol → memanggil...
                   ▼
   ┌──────────────────────────────────────┐
   │  2. VIEWMODEL (otak tiap layar)       │   contoh: LoginViewModel
   │     mengatur logika & menyimpan       │   memutuskan: loading? sukses? error?
   │     "keadaan" layar                   │
   └───────────────┬──────────────────────┘
                   │  minta data ke...
                   ▼
   ┌──────────────────────────────────────┐
   │  3. REPOSITORY (perantara data)       │   contoh: TaskRepository
   │     "kalau butuh data tugas,          │   menyederhanakan & merapikan
   │      tanya ke sini"                   │
   └───────────────┬──────────────────────┘
                   │  panggil...
                   ▼
   ┌──────────────────────────────────────┐
   │  4. API SERVICE (penghubung server)   │   ApiService.kt
   │     benar-benar mengirim request      │   ◄──► berbicara ke backend Laravel
   │     lewat internet                    │
   └──────────────────────────────────────┘
```

**Kenapa dipisah-pisah begini?** Supaya rapi dan mudah diperbaiki. Kalau ada masalah di
tampilan, kita cek Screen. Kalau masalah logika, cek ViewModel. Kalau masalah ambil data,
cek Repository. Tiap bagian punya tanggung jawab jelas — tidak campur aduk.

### Istilah penting yang sering muncul

| Istilah | Arti sederhana |
|---|---|
| **Composable / Screen** | Kode yang menggambar tampilan di layar (tombol, teks, daftar). |
| **ViewModel** | "Otak" satu layar. Menyimpan kondisi layar & mengatur logika. |
| **Repository** | Perantara yang tahu cara mengambil/mengirim data. |
| **State / UiState** | "Keadaan layar saat ini": sedang `Loading`, `Success` (berhasil, ada data), atau `Error`. |
| **Token** | Semacam "tiket masuk" yang dipakai setelah login untuk membuktikan identitas ke server. |
| **DataStore** | Tempat penyimpanan kecil di HP. Di sini hanya disimpan token + data user. |
| **Service** | Proses yang berjalan di latar belakang (di sini: pengirim GPS). |
| **FCM** | Firebase Cloud Messaging — layanan Google untuk mengirim notifikasi push. |

---

## 4. Alur lengkap penggunaan aplikasi (langkah demi langkah)

Inilah perjalanan teknisi memakai aplikasi, dari buka aplikasi sampai menyelesaikan tugas.

### Peta seluruh layar

```
                          [ Buka aplikasi ]
                                 │
                  Sudah pernah login?  ──► token tersimpan?
                       │ belum                    │ sudah
                       ▼                          ▼
                  ┌─────────┐              ┌───────────────┐
                  │  LOGIN  │ ───────────► │  DAFTAR TUGAS  │ ◄── halaman utama
                  └─────────┘   berhasil   └───────┬───────┘
                                                   │ tap salah satu tugas
                                                   ▼
                                          ┌──────────────────┐
                                          │   DETAIL TUGAS   │
                                          └────────┬─────────┘
                                    tekan "Mulai Memperbaiki"
                                                   ▼
                                          ┌──────────────────┐
                                          │ SEDANG MEMPERBAIKI│ ◄── GPS aktif di sini
                                          │     (Working)     │
                                          └────────┬─────────┘
                                       tekan "Tandai Selesai"
                                                   ▼
                                       kembali ke DAFTAR TUGAS

   Menu bawah (selalu ada di halaman utama):
   [ Tugas ]   [ Notifikasi ]   [ Profil ]
```

### Langkah 1 — Membuka aplikasi & menentukan halaman awal

Saat aplikasi dibuka, ia mengecek: **"Apakah teknisi sudah pernah login?"** Caranya, ia
melihat apakah ada **token** tersimpan di HP (DataStore).

- Belum ada token → tampilkan layar **Login**.
- Sudah ada token → langsung ke **Daftar Tugas** (tidak perlu login ulang).

*(Kode: `MainViewModel` membaca token, lalu `MainActivity` menampilkan layar yang sesuai.)*

### Langkah 2 — Login

Teknisi memasukkan **email & password**, lalu menekan tombol Masuk.

Apa yang terjadi di belakang layar:

1. Aplikasi mengirim email & password ke backend (`POST /api/auth/login`).
2. Kalau benar, backend membalas dengan **token** + **data user** (nama, email).
3. Aplikasi **menyimpan token & data user** di HP (DataStore). Token ini akan dipakai untuk
   semua permintaan berikutnya sebagai bukti identitas.
4. Aplikasi juga langsung mengambil **FCM token** (alamat unik HP ini untuk notifikasi) dan
   mengirimkannya ke server, supaya server tahu ke mana harus mengirim notifikasi tugas baru.
5. Pindah ke **Daftar Tugas**.

> Catatan: Hanya akun ber-role **teknisi** yang bisa login. Tidak ada fitur daftar/sign-up —
> akun dibuat admin lewat web.

*(Kode: `LoginViewModel.login()` → `AuthRepository.login()`.)*

### Langkah 3 — Daftar Tugas (halaman utama)

Ini halaman utama. Aplikasi mengambil daftar tugas milik teknisi dari server
(`GET /api/tasks`) dan menampilkannya sebagai kartu-kartu.

Hanya tugas yang **masih aktif** yang muncul: status `ditugaskan` (belum dikerjakan) atau
`sedang_memperbaiki` (sedang dikerjakan). Tugas yang sudah `selesai` tidak ditampilkan.

Fitur:
- **Tarik ke bawah untuk refresh** (pull-to-refresh) — memuat ulang daftar.
- **Refresh otomatis** setiap kali teknisi kembali ke halaman ini — supaya tugas baru muncul
  tanpa perlu menutup aplikasi.
- Di halaman ini juga aplikasi **meminta izin notifikasi** (Android 13+), supaya notifikasi
  tugas baru bisa muncul nanti.

Di bawah ada **menu navigasi** (bottom navigation): Tugas, Notifikasi, Profil.

*(Kode: `TaskListViewModel` → `TaskRepository.getTasks()`.)*

### Langkah 4 — Detail Tugas

Teknisi menekan salah satu tugas → masuk ke halaman detail
(`GET /api/tasks/{id}`). Di sini ditampilkan info lengkap:

- Nama pelanggan, alamat, jenis kerusakan, catatan.
- **Timeline / riwayat** (work logs) — jejak perubahan status tugas (kapan ditugaskan, dll).
- **Tombol aksi** yang berubah sesuai status:
  - Kalau status `ditugaskan` → tombol **"Mulai Memperbaiki"**.

Ketika teknisi menekan **"Mulai Memperbaiki"**:
1. Aplikasi memberitahu server "saya mulai mengerjakan" (`POST /api/tasks/{id}/status` dengan
   nilai `in_progress`).
2. Server mengubah status tugas jadi `sedang_memperbaiki`.
3. Aplikasi pindah ke layar **Sedang Memperbaiki** (Working).

> **Penting soal alamat:** alamat pelanggan hanya berupa **teks** (mis. "Jl. Mawar No.3").
> Tidak ada titik koordinat pelanggan, jadi aplikasi **tidak bisa** menunjukkan pin pelanggan
> di peta atau navigasi ke sana. GPS yang dikirim aplikasi adalah **posisi teknisi**, bukan
> posisi pelanggan.

*(Kode: `TaskDetailViewModel.startRepair()`.)*

### Langkah 5 — Sedang Memperbaiki (Working) + GPS

Ini layar paling "hidup". Selama teknisi berada di status ini, aplikasi **mengirim lokasi GPS-nya
ke server secara berkala**, supaya admin bisa memantau di peta.

Yang terjadi saat masuk layar ini:
1. Aplikasi **meminta izin lokasi** (kalau belum diberi). Tanpa izin ini, GPS tidak bisa jalan.
2. Setelah izin diberi & data tugas termuat, aplikasi **menyalakan GPS Service** (`LocationService`).
3. Muncul **timer durasi** pengerjaan dan **notifikasi permanen** "SkyNet — GPS aktif" (ini
   wajib oleh aturan Android: kalau aplikasi mengirim lokasi di latar belakang, harus ada
   notifikasi yang menandakannya).

Cara kerja pengiriman GPS (detail di bagian 5):
- Setiap **±15 detik**, lokasi teknisi dikirim ke server (`POST /api/location`).
- Server hanya menerima kalau status tugas memang `sedang_memperbaiki` & tugas itu milik
  teknisi tersebut.

Ada dua hal yang bisa dilakukan teknisi di sini:

- **"Tandai Selesai"** → aplikasi memberitahu server pekerjaan selesai (`done` →
  status jadi `selesai`), **mematikan GPS Service**, lalu kembali ke Daftar Tugas. Tugas ini
  hilang dari daftar karena sudah selesai.
- **"Kecilkan" (tombol ⌄ atau tombol back)** → kembali ke Daftar Tugas **tanpa mematikan GPS**.
  GPS tetap berjalan di latar belakang. Teknisi bisa cek tugas/notifikasi/profil sambil tetap
  terpantau. Untuk kembali ke layar ini, teknisi masuk lagi lewat Detail Tugas.

*(Kode: `WorkingScreen` mengatur izin & menyalakan/mematikan service; `WorkingViewModel`
mengatur timer & tombol selesai.)*

### Langkah 6 — Notifikasi

Tab **Notifikasi** menampilkan daftar pemberitahuan (`GET /api/notifications`), misalnya
"Tugas Baru: Anda ditugaskan ke...". Notifikasi yang belum dibaca ditandai. Saat ditap,
ditandai sebagai sudah dibaca (`PUT /api/notifications/{id}/read`).

Selain daftar ini, notifikasi juga muncul sebagai **push notification** (lihat bagian 6).

### Langkah 7 — Profil & Logout

Tab **Profil** menampilkan nama & email teknisi, dan tombol **Logout**.

Saat logout:
1. Aplikasi memberitahu server untuk menghapus token (`POST /api/auth/logout`).
2. **Apa pun hasilnya** (bahkan kalau sedang offline), aplikasi **selalu** menghapus token &
   data user dari HP.
3. Kembali ke layar Login.

*(Kode: `ProfileViewModel` → `AuthRepository.logout()`.)*

---

## 5. Bagaimana GPS bekerja (lebih dalam)

Ini fitur inti, jadi dijelaskan terpisah.

**Apa itu "Foreground Service"?** Itu cara Android menjalankan tugas di latar belakang secara
sah, dengan syarat ada notifikasi yang terlihat. Aplikasi memakainya untuk terus mengirim GPS
walaupun teknisi pindah ke layar lain atau mengunci HP.

**Kapan menyala?** Saat teknisi masuk layar "Sedang Memperbaiki" dan izin lokasi sudah diberi.

**Kapan mati?**
1. Teknisi menekan "Tandai Selesai", **atau**
2. Server menolak lokasi dengan kode **403** (artinya tugas sudah tidak aktif / bukan tugas dia)
   — service langsung berhenti sendiri.

**Seberapa sering & seberapa akurat?**
- Kirim tiap **±15 detik** (web admin memuat ulang peta tiap 10 detik; 15 detik adalah
  kompromi antara realtime dan hemat baterai).
- Akurasi pakai mode **hemat baterai** (`BALANCED`, ~100 m) — bukan akurasi tertinggi yang
  paling boros baterai. Untuk peta pemantauan admin, ini sudah cukup.
- Ada ambang **jarak 15 meter**: kalau teknisi **diam di tempat**, lokasi **tidak dikirim**.
  Ini **disengaja** untuk hemat baterai & data — bukan bug. Lokasi baru dikirim lagi saat
  teknisi bergerak cukup jauh.

**Apa yang dikirim?** Titik koordinat (latitude, longitude), `report_id` tugas yang sedang
dikerjakan, dan **waktu sebenarnya GPS terbaca** (`recorded_at`) — bukan waktu kirim. Jadi
kalau sinyal lemah dan pengiriman tertunda, waktunya tetap akurat.

*(Kode: `LocationService.kt`, konstanta di `util/Constants.kt`.)*

---

## 6. Bagaimana notifikasi push bekerja (FCM)

Aplikasi memakai **Firebase Cloud Messaging (FCM)** dari Google untuk mengirim notifikasi
"tugas baru" ke HP teknisi, bahkan saat aplikasi sedang tertutup.

Alurnya:
1. Setiap HP punya **FCM token** (semacam alamat unik). Saat login, aplikasi mengambil token ini
   dan mengirimkannya ke server. Server menyimpan: "teknisi X ada di HP dengan alamat token ini".
2. Saat admin menugaskan tugas baru, backend menyuruh Firebase mengirim notifikasi ke alamat
   token teknisi tersebut.
3. Notifikasi muncul di HP teknisi.

Perilaku notifikasi berbeda tergantung kondisi aplikasi (ini bawaan Firebase, tidak bisa diubah):

| Kondisi aplikasi | Yang menampilkan notifikasi |
|---|---|
| **Terbuka (foreground)** | Aplikasi sendiri yang menampilkannya (kode `onMessageReceived`). |
| **Tertutup / di latar** | Sistem Android otomatis menampilkannya. |

Saat notifikasi ditap, aplikasi terbuka di halaman awal (Daftar Tugas). **Tidak langsung
membuka tugas tertentu** — ini sengaja disederhanakan agar tidak menambah risiko bug.

*(Kode: `MonitoringFirebaseService.kt`.)*

---

## 7. Bagaimana keamanan/identitas dijaga (token)

Setelah login, semua permintaan ke server harus menyertakan **token** sebagai bukti "ini benar
teknisi yang sudah login". Aplikasi melakukannya **otomatis**: ada komponen bernama
**AuthInterceptor** yang menyelipkan token ke setiap permintaan, jadi programmer tidak perlu
menambahkannya satu per satu.

Kalau token kedaluwarsa / tidak valid, server membalas kode **401**. Saat itu aplikasi akan
menghapus token dan memaksa teknisi login ulang.

*(Kode: `AuthInterceptor.kt`.)*

---

## 8. Ringkasan satu paragraf (untuk dihafal cepat)

> Aplikasi ini adalah **klien Android untuk teknisi** SkyNet. Teknisi **login** (token disimpan
> di HP), lalu melihat **daftar tugas** yang diambil dari **server Laravel** lewat **API**. Saat
> mengerjakan tugas, teknisi menekan **"Mulai Memperbaiki"** (status → `sedang_memperbaiki`),
> dan aplikasi menyalakan **GPS Foreground Service** yang mengirim lokasi tiap ±15 detik ke
> server agar admin bisa memantau di peta. Setelah selesai, teknisi menekan **"Tandai Selesai"**
> (status → `selesai`), GPS dimatikan. Notifikasi tugas baru dikirim lewat **Firebase (FCM)**.
> Kode disusun rapi dengan pola **MVVM** (Screen → ViewModel → Repository → API) agar setiap
> bagian punya tugas yang jelas.

---

## 9. Daftar istilah singkat

- **API**: aturan cara aplikasi & server bertukar data lewat internet.
- **Backend**: server pusat (Laravel) yang menyimpan semua data & dipakai admin.
- **Token**: tiket bukti login.
- **MVVM**: pola penyusunan kode menjadi lapisan UI–ViewModel–Repository.
- **State**: keadaan layar saat ini (Loading / Success / Error).
- **Foreground Service**: proses latar belakang yang sah (dengan notifikasi) — di sini pengirim GPS.
- **FCM**: layanan Google untuk notifikasi push.
- **DataStore**: penyimpanan kecil di HP (di sini: token & data user).
- **`report_id` vs `task id`**: saat kirim GPS pakai `report_id` (id laporan), bukan id tugas.
