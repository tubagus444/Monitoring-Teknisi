# Panduan Uji Aplikasi di HP Fisik (Uji Akurasi GPS)

Panduan menjalankan aplikasi Monitoring Teknisi di **HP fisik** (bukan emulator) agar GPS teruji dengan pergerakan nyata.

---

## Tahap 1 — Siapkan Backend agar Bisa Diakses HP

1. Pastikan laptop dan HP Anda terhubung ke **jaringan Wi-Fi yang sama**.
2. Cek IP lokal laptop Anda. Buka terminal (PowerShell) dan ketik:
   ```powershell
   ipconfig
   ```
   *Catat IPv4 Address Anda (biasanya berawalan `192.168.x.x` atau `10.x.x.x`, contoh: `192.168.0.105`).*
3. Di terminal proyek Laravel (Aplikasi Monitoring), jalankan perintah:
   ```bash
   composer dev:mobile
   ```
   *(Perintah ini akan menjalankan server di `--host=0.0.0.0` sekaligus menyalakan queue worker).*
4. **Wajib Tes via Browser HP**: Buka browser di HP Anda (Chrome/Safari) lalu akses:
   ```
   http://192.168.0.105:8000
   ```
   - Halaman Laravel muncul → **Jaringan beres, lanjut!**
   - Loading lama / Connection Refused → Biasanya diblokir **Windows Firewall**. Anda perlu mengizinkan port 8000 atau mematikan sementara firewall untuk jaringan "Private".

---

## Tahap 2 — Sambungkan HP ke Android Studio

1. HP: Buka **Settings → About phone → tap "Build number" 7×** untuk mengaktifkan Developer Options.
2. Buka **Developer Options → aktifkan "USB Debugging"**.
3. Colokkan HP ke laptop via kabel USB.
4. Di HP muncul popup **"Allow USB debugging?"** → centang "Always allow" → **OK**.
5. Pastikan nama HP Anda muncul di dropdown perangkat di bagian atas Android Studio.

---

## Tahap 3 — Jalankan Aplikasi

Anda bisa menggunakan *Build Variant* mana saja (`emulatorDebug` ataupun `deviceDebug`), karena aplikasi ini sudah dilengkapi fitur penggantian URL API secara dinamis!

1. Klik tombol **Run ▶** (hijau) di Android Studio.
2. Aplikasi akan ter-install dan terbuka di HP Anda.

---

## Tahap 4 — Konfigurasi API di Halaman Login

Agar aplikasi Android bisa tersambung ke backend Laravel di laptop:
1. Saat aplikasi terbuka (berada di Halaman Login), ketuk ikon **Pengaturan (Konfigurasi API)**.
2. Masukkan URL backend beserta port dan path `/api/`, menggunakan IP laptop Anda tadi.
   Contoh: `http://192.168.0.105:8000/api/`
3. Simpan pengaturan. 
4. Coba login menggunakan akun teknisi. Jika berhasil masuk, berarti koneksi sukses!

---

## Tahap 5 — Uji GPS

1. Pastikan **GPS menyala** di HP Anda (Settings → Location → Mode Akurasi Tinggi).
2. Berikan izin **Lokasi (Allow all the time / Allow only while using the app)** dan izin **Notifikasi** ketika diminta.
3. Buka salah satu tugas laporan, lalu tekan **"Mulai Memperbaiki"**. 
4. Layar "Working" (Sedang Memperbaiki) akan terbuka, dan akan muncul notifikasi persisten di panel atas layar bahwa *GPS Aktif*.
5. **Penting:** Aplikasi ini dirancang agar **hanya mengirimkan lokasi ke server jika teknisi berpindah sejauh ≥ 15 meter** (untuk menghemat baterai). 
   - Untuk melihat perubahan titik di peta web admin, silakan **berjalan kaki/naik motor** menjauhi posisi awal.
6. Tekan **"Tandai Selesai"** untuk menghentikan pengiriman GPS.

---

## 💡 Tips: Saat Pindah Wi-Fi (IP Laptop Berubah)

Jika Anda berpindah tempat (misal dari kampus ke rumah), IP laptop Anda otomatis akan berubah. 
**Anda TIDAK perlu build ulang APK-nya dari Android Studio.**
Cukup ikuti langkah ini:
1. Cek IP baru laptop Anda.
2. Pastikan Laravel berjalan (`composer dev:mobile`).
3. Buka aplikasi di HP, pergi ke halaman **Pengaturan API** di layar Login.
4. Masukkan IP yang baru. Selesai!
