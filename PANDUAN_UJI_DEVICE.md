# Panduan Uji Aplikasi di HP Fisik (Uji Akurasi GPS)

Panduan menjalankan aplikasi Monitoring Teknisi di **HP fisik** (bukan emulator) agar GPS
teruji dengan pergerakan nyata.

## Konsep: Build Variant

Aplikasi punya 2 versi dari kode yang sama, beda alamat backend saja:

| Variant | BASE_URL | Untuk |
|---|---|---|
| **emulatorDebug** | `http://10.0.2.2:8000/api/` | Emulator Android Studio |
| **deviceDebug** | `http://192.168.0.105:8000/api/` (dari `local.properties`) | HP fisik via WiFi |

`10.0.2.2` hanya dimengerti emulator. HP fisik butuh IP asli laptop. Tinggal **pilih** variant,
tak perlu edit kode.

---

## Tahap 1 — Siapkan backend agar bisa diakses HP

1. Jalankan Laravel agar mendengar dari semua alamat:
   ```
   php artisan serve --host=0.0.0.0 --port=8000
   ```
2. Tes dari **browser HP** dulu:
   ```
   http://192.168.0.105:8000
   ```
   - Halaman Laravel muncul → jaringan beres, lanjut.
   - Gagal/loading terus → biasanya **Windows Firewall** memblokir. Izinkan port 8000
     (atau matikan sementara firewall jaringan "Private").

   > Syarat: HP dan laptop **WiFi yang sama**.

---

## Tahap 2 — Sambungkan HP ke Android Studio

1. HP: **Settings → About phone → tap "Build number" 7×** untuk buka Developer Options.
2. **Developer Options → aktifkan "USB Debugging"**.
3. Colok HP ke laptop via USB.
4. Di HP muncul popup **"Allow USB debugging?"** → centang "Always allow" → **OK**.
5. Cek di Android Studio pojok kanan atas: nama HP muncul di dropdown perangkat.
   - Cek via terminal: `adb devices` (HP harus terdaftar, status `device`).

---

## Tahap 3 — Pilih variant `deviceDebug`

1. Android Studio, **pojok kiri bawah**, klik tab **"Build Variants"**
   (atau menu **View → Tool Windows → Build Variants**).
2. Di baris modul **`:app`**, ubah "Active Build Variant" dari `emulatorDebug` → **`deviceDebug`**.
3. Tunggu Gradle sync selesai.

---

## Tahap 4 — Jalankan ke HP

**Cara A — Android Studio (gampang):**
- Dropdown perangkat (atas) menunjuk ke **HP Anda**.
- Tekan **Run ▶** (hijau). Otomatis build + pasang + buka app.

**Cara B — Terminal:**
```powershell
.\gradlew.bat :app:installDeviceDebug
```
Lalu buka app manual di HP.

---

## Tahap 5 — Uji GPS

1. Saat app dibuka pertama kali, **beri izin Lokasi dan Notifikasi**.
2. HP: pastikan **GPS menyala**, mode akurasi tinggi (Settings → Location).
3. Login → buka tugas → tekan **"Mulai Memperbaiki"**. Layar Working terbuka,
   muncul notifikasi persisten "SkyNet — GPS aktif".
4. **Penting:** app sengaja **hanya kirim lokasi kalau bergerak ≥ 15 meter** (hemat baterai —
   bukan bug). Supaya data masuk:
   - **Jalan kaki** beberapa meter sambil layar Working terbuka, atau
   - Pantau peta web admin — titik teknisi bergerak mengikuti Anda.
5. Tekan **"Tandai Selesai"** untuk hentikan pengiriman GPS.

---

## Saat pindah WiFi (IP laptop berubah)

1. Cek IP laptop baru:
   ```powershell
   Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -like '192.168.*' }
   ```
2. Buka **`local.properties`** (root proyek), ganti:
   ```
   deviceBaseUrl=http://<IP-BARU>:8000/api/
   ```
3. Sync Gradle / build ulang. **Tanpa sentuh kode.**

---

## Kembali ke emulator

Di panel **Build Variants**, ubah `:app` kembali ke **`emulatorDebug`**. Selesai.
