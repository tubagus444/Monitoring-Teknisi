# Pengembangan Lanjutan — Aplikasi Monitoring Teknisi (SkyNet)

> 📓 **Dokumen hidup (living document).** Catatan semua penyesuaian/fitur **setelah** aplikasi
> inti (fase 0–9) selesai. Setiap penambahan dicatat sebagai satu **Iterasi** bernomor.
>
> - Fase inti (0–9) → `RENCANA_IMPLEMENTASI.md` (arsip, read-only).
> - Kontrak aktif (API, struktur package, konvensi) → `CLAUDE.md`.
> - Dokumen ini → riwayat & rencana pengembangan berkelanjutan (metode RAD, inkremental).

> Klien Android untuk backend Laravel `Aplikasi-Monitoring`. Studi kasus skripsi.

## Daftar Iterasi

| # | Iterasi | Tanggal | Status |
|---|---|---|---|
| 1 | [Penyesuaian Modul Pelanggan](#iterasi-1--penyesuaian-modul-pelanggan) | 2026-06-27 | ✅ Selesai (kode) |
| 2 | [Catatan & Bukti Pekerjaan + Foto Rumah](#iterasi-2--catatan--bukti-pekerjaan--foto-rumah) | 2026-06-28 | ✅ Selesai (kode) |

> Tambahkan baris baru di tabel ini setiap memulai iterasi, lalu tulis detailnya memakai
> **template** di bawah.

---

## Template Iterasi (salin untuk fitur baru)

```markdown
## Iterasi N — <Judul Singkat>

> Status: 🚧 Berjalan / ✅ Selesai / 📦 Ditunda — <ringkasan satu kalimat>.

### Konteks
Apa yang memicu perubahan ini (mis. fitur baru di backend, kebutuhan baru, perbaikan).

### Keputusan yang Dikonfirmasi
| Topik | Keputusan | Alasan |
|---|---|---|

### Perubahan Kontrak API (bila ada)
Endpoint/field yang berubah — tabel field (nama, tipe, nullability, keterangan).

### Perubahan Kode
| Aksi | File |
|---|---|
| Diubah | ... |
| Baru | ... |

Penjelasan ringkas per langkah.

### Gotcha / Risiko & Mitigasi
1. ...

### BELUM Termasuk (jangan dibangun dulu)
- ...

### Verifikasi
- compile / unit test / uji e2e — hasilnya.

### Status
- [ ] Langkah ...
```

---

## Iterasi 1 — Penyesuaian Modul Pelanggan

> Status: ✅ Selesai (kode) — `compileEmulatorDebugKotlin` hijau, 51 unit test lulus.
> Sisa = uji end-to-end dengan backend Laravel berjalan.

### Konteks

Backend SkyNet menyelesaikan **Modul Pelanggan**: pelanggan kini entitas tersendiri, dan
setiap laporan punya **kategori** (`pelanggan` / `jaringan` / `pemeliharaan`). Endpoint tugas
(`GET /api/tasks` & `GET /api/tasks/{id}`) menambah beberapa field, dan kini ada **tugas tanpa
pelanggan**. Sisi Android harus adaptif per kategori dan aman terhadap field `null` — tanpa
menyentuh endpoint login, update status, maupun GPS.

### Keputusan yang Dikonfirmasi

| Topik | Keputusan | Alasan |
|---|---|---|
| **Judul tugas** | Selalu pakai `headline` | `customer` kini bisa `null` (tugas non-pelanggan); `headline` selalu terisi. |
| **Field baru ditaruh di akhir model** | Default value, bukan sisip di tengah | Konstruksi `Task(...)` posisional di unit test tetap valid; Gson memetakan via `@SerializedName` sehingga urutan tak berpengaruh ke parsing. |
| **Aksi kontak pelanggan** | Telepon (`tel:`) + WhatsApp (`wa.me`) | Sesuai permintaan; nomor dinormalkan `0…` → `62…`. |
| **Galeri foto rumah** | Coil, muat URL apa adanya | URL `house_photos` absolut & ikut host API; tak boleh disusun manual. |
| **Cleartext HTTP** | Sudah aktif (tak ada perubahan) | `usesCleartextTraffic="true"` ada sejak fase 2 → foto HTTP `10.0.2.2`/IP-LAN langsung termuat. |
| **Upload foto & bukti pekerjaan** | Ditunda | Endpoint backend belum ada — iterasi berikutnya. |

### Perubahan Kontrak API

`GET /api/tasks` (list) & `GET /api/tasks/{id}` (detail) menambah field:

| Field | Tipe | Keterangan |
|---|---|---|
| `category` | `String` | `"pelanggan"` \| `"jaringan"` \| `"pemeliharaan"` |
| `headline` | `String` | Judul tampilan — PAKAI ini apa pun kategorinya |
| `customer` | `String?` | Nama pelanggan; `null` untuk non-pelanggan |
| `phone` | `String?` | No. HP pelanggan; `null` untuk non-pelanggan |
| `ip_address` | `String?` | IP pelanggan; `null` untuk non-pelanggan |
| `subscription_package` | `String?` | Paket internet; `null` untuk non-pelanggan |
| `house_photos` | `List<String>` | **Hanya di DETAIL.** URL absolut foto rumah; `[]` bila kosong |

`address` tetap ada (pelanggan = alamat; jaringan/pemeliharaan = lokasi/area terdampak),
tetap teks biasa — BUKAN koordinat.

### Perubahan Kode

| Aksi | File |
|---|---|
| Diubah | `data/api/model/TaskModels.kt`, `ui/components/TaskCard.kt`, `ui/screens/tasks/TaskListScreen.kt`, `ui/screens/tasks/TaskDetailScreen.kt`, `ui/screens/working/WorkingScreen.kt` |
| Baru | `data/api/model/TaskCategory.kt`, `ui/components/CategoryBadge.kt` |

1. **Model** — `Task` menambah `category, headline, phone, ipAddress, subscriptionPackage,
   housePhotos`; `customer/phone/ipAddress/subscriptionPackage` jadi nullable; `housePhotos`
   default `emptyList()`. Field baru ditaruh **setelah** `workLogs` dengan default. Tambah
   properti turunan `displayTitle` (`headline` → `customer` → `"Tugas #<reportId>"`) sebagai
   judul aman-null terpusat.
2. **`TaskCategory`** (enum) — `PELANGGAN` → "Gangguan Pelanggan", `JARINGAN` → "Gangguan
   Jaringan/Infrastruktur", `PEMELIHARAAN` → "Pemeliharaan"; `from()` `null`-safe.
3. **`CategoryBadge`** — Surface `secondaryContainer`, tak tampil bila kategori tak dikenal.
4. **Daftar tugas** — `TaskCard` & banner pakai `displayTitle` + `CategoryBadge`.
5. **Detail adaptif** — header `displayTitle` + badge; label alamat adaptif ("Alamat" vs
   "Lokasi/Area"); **hanya kategori pelanggan**: `CustomerContactCard` (nama, IP, paket, No. HP
   dengan tombol Telepon + WhatsApp, tiap baris null-check) & `HousePhotoGallery` (`LazyRow`
   thumbnail Coil, tap → `Dialog` perbesar; array kosong → section disembunyikan).
6. **Working screen** — `customer` → `displayTitle`.

Helper `toWhatsAppNumber(phone)`: buang non-digit, `0…` → `62…`.

### Gotcha / Risiko & Mitigasi

1. **Semua field pelanggan bisa `null`** — mitigasi: `displayTitle`, kartu/galeri hanya untuk
   kategori pelanggan, tiap baris kontak dibungkus null-check.
2. **URL `house_photos` absolut & ikut host API** — muat apa adanya; asalkan `BASE_URL` benar,
   URL foto otomatis benar.
3. **Cleartext HTTP** untuk foto storage — sudah dijamin `usesCleartextTraffic="true"`.
4. **Kompatibilitas unit test** — field baru di akhir + default → konstruksi `Task(...)`
   posisional di 4 test tetap kompilasi & lulus.

### BELUM Termasuk (jangan dibangun dulu)

- Upload foto rumah dari Android (`POST /api/customers/{id}/photos`) — endpoint backend belum ada.
- Fitur "catatan & bukti pekerjaan" (foto hasil perbaikan + deskripsi work log).

### Verifikasi

- `.\gradlew.bat :app:compileEmulatorDebugKotlin --console=plain` → hijau.
- `.\gradlew.bat :app:testEmulatorDebugUnitTest --console=plain` → 51 test lulus.

### Status

- [x] Model/DTO (`Task` + `TaskCategory`)
- [x] `CategoryBadge`
- [x] Daftar tugas (`TaskCard`, banner)
- [x] Detail adaptif (kontak + galeri)
- [x] Working screen
- [x] Verifikasi: compile hijau + 51 test lulus
- [ ] Uji end-to-end: tugas pelanggan (kontak + galeri) & tugas jaringan/pemeliharaan (tanpa
      crash, field `null`, galeri kosong) dengan backend Laravel berjalan

---

## Iterasi 2 — Catatan & Bukti Pekerjaan + Foto Rumah

> Status: ✅ Selesai (kode) — `assembleEmulatorDebug` hijau, 56 unit test lulus.
> Sisa = uji end-to-end dengan backend Laravel berjalan.

### Konteks

Backend menambah 3 kemampuan (semua **backward-compatible**, hanya menambah): (A) catatan
pekerjaan opsional saat update status, (B) unggah foto bukti hasil kerja, (C) unggah foto rumah
pelanggan. Detail tugas kini juga mengembalikan `repair_photos`. Endpoint lama tetap jalan.

### Keputusan yang Dikonfirmasi

| Topik | Keputusan | Alasan |
|---|---|---|
| **Sumber foto** | Kamera **+** Galeri | Teknisi lapangan butuh foto langsung; galeri untuk foto lama. |
| **Tempat unggah foto bukti** | Detail **dan** Working | Fleksibel — bisa saat bekerja maupun saat melihat detail. |
| **Permission kamera** | TIDAK deklarasi `CAMERA` | ACTION_IMAGE_CAPTURE via `FileProvider` tak butuh izin → tanpa prompt. |
| **Kompresi** | JPEG <5 MB di klien (down-sample + EXIF) | Lolos batas 5 MB backend; hemat kuota; perbaiki rotasi. |
| **Refresh setelah unggah** | Muat ulang detail (`getTaskDetail`) | Galeri `repair_photos`/`house_photos` selalu konsisten dgn server. |
| **Catatan saat selesai** | Dialog konfirmasi di Working dgn field multiline | Paling relevan saat `done` (rangkuman hasil). |
| **Idempotent 200 status** | Diperlakukan sukses | Bila tak ada transisi, server balas 200 tanpa work log baru — bukan error. |

### Perubahan Kontrak API

| Endpoint | Perubahan |
|---|---|
| `POST /tasks/{id}/status` | + field opsional `description` (string, maks 1000) |
| `GET /tasks/{id}` | + field `repair_photos` (objek `{url, caption, technician, uploaded_at}`) |
| `POST /tasks/{id}/photos` | **BARU** — multipart `photo` (wajib, ≤5 MB) + `caption`, semua kategori, 201 |
| `POST /tasks/{id}/house-photos` | **BARU** — multipart, hanya `pelanggan` (non-pelanggan → 422) |

### Perubahan Kode

| Aksi | File |
|---|---|
| Diubah | `data/api/ApiService.kt`, `data/api/model/TaskModels.kt`, `data/repository/TaskRepository.kt`, `ui/screens/working/WorkingViewModel.kt`, `ui/screens/working/WorkingScreen.kt`, `ui/screens/tasks/TaskDetailViewModel.kt`, `ui/screens/tasks/TaskDetailScreen.kt`, `AndroidManifest.xml`, `app/src/test/.../TaskRepositoryTest.kt` |
| Baru | `util/ImageCompressor.kt`, `ui/components/AddPhotoButton.kt`, `res/xml/file_paths.xml` |

1. **Model** — `UpdateStatusRequest` + `description: String?`; `RepairPhoto`,
   `PhotoUploadResponse`; `Task` + `repairPhotos: List<RepairPhoto> = emptyList()` (di akhir).
2. **API** — dua endpoint `@Multipart` (`uploadRepairPhoto`, `uploadHousePhoto`); part file
   bernama persis `photo`, `caption` opsional.
3. **`ImageCompressor`** — `content://` Uri → JPEG <5 MB: down-sample saat decode (≤1600 px) +
   koreksi rotasi EXIF + turunkan kualitas bertahap; `@ApplicationContext`, di `Dispatchers.IO`.
4. **Repository** — `updateStatus(..., description)`; `uploadRepairPhoto`/`uploadHousePhoto`
   (kompres → multipart → `safeApiCall` → muat ulang detail di ViewModel).
5. **`AddPhotoButton`** — komponen reusable: dialog pilih Kamera/Galeri; kamera via
   `TakePicture` + `FileProvider` (cache `images/`), galeri via Photo Picker.
6. **UI** — Working: tombol unggah bukti + dialog "Selesaikan Tugas" (field catatan). Detail:
   galeri `repair_photos` (caption di bawah thumbnail) + tombol unggah bukti (semua kategori);
   tombol unggah foto rumah hanya saat `category == "pelanggan"`. `HousePhotoGallery` di-refaktor
   jadi `PhotoGallery`/`RepairPhotoGallery`/`EnlargedPhotoDialog` reusable.
7. **Manifest** — `FileProvider` (`${applicationId}.fileprovider`) + `uses-feature` kamera opsional.

### Gotcha / Risiko & Mitigasi

1. **Multipart, bukan JSON; nama part wajib `photo`** — diset di `ApiService`.
2. **File mentah bisa >5 MB → 422** — selalu lewat `ImageCompressor` dulu.
3. **Foto rumah di tugas non-pelanggan → 422** — tombol hanya tampil saat `pelanggan`; error
   tetap ditangani via `message` backend.
4. **Rotasi EXIF** — foto kamera bisa miring; dikoreksi via `ExifInterface` sebelum kirim.
5. **Kompatibilitas unit test** — `repairPhotos` di akhir + default → konstruksi `Task(...)`
   posisional di test lama tetap valid; `TaskRepositoryImpl` kini butuh `ImageCompressor`
   (test pakai `mockk`).
6. **Idempotent 200** — bila status tak berubah, `description` tak tersimpan; bukan error.

### BELUM Termasuk (jangan dibangun dulu)

- Pendaftaran pelanggan baru oleh teknisi (`POST /api/customers`) — Rencana #4, menunggu
  persetujuan dosen; endpoint belum ada.
- Hapus/edit caption foto yang sudah diunggah.

### Verifikasi

- `.\gradlew.bat :app:compileEmulatorDebugKotlin --console=plain` → hijau.
- `.\gradlew.bat :app:testEmulatorDebugUnitTest --console=plain` → 56 test lulus.
- `.\gradlew.bat :app:assembleEmulatorDebug --console=plain` → APK terbentuk.

### Status

- [x] Model/DTO (`description`, `RepairPhoto`, `PhotoUploadResponse`, `Task.repairPhotos`)
- [x] API multipart (`uploadRepairPhoto`, `uploadHousePhoto`)
- [x] `ImageCompressor` + `AddPhotoButton` + `FileProvider`
- [x] Repository + ViewModel (catatan + unggah + refresh)
- [x] UI: Working (dialog catatan + unggah bukti), Detail (galeri + unggah bukti/rumah)
- [x] Verifikasi: compile + 56 test + assemble hijau
- [ ] Uji end-to-end: catatan tersimpan di work log; foto bukti muncul di `repair_photos`; foto
      rumah muncul di `house_photos`; 422 foto rumah pada tugas non-pelanggan; kamera & galeri
      di perangkat fisik dengan backend Laravel berjalan
