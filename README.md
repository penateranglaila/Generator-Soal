# Generator Soal F4 — proyek APK

Pembungkus Android (WebView) untuk `generator-soal-f4.html`. Tidak memakai pustaka pihak ketiga,
jadi proses build hanya butuh Android Gradle Plugin + Android SDK.

- Android 5.0+ (minSdk 21) — cukup untuk Oppo A5s, Samsung A16, dan MatePad 11.5
- Berjalan offline penuh; semua pengaturan tersimpan otomatis di perangkat
- **Simpan DOCX / proyek (.json)**: muncul pemilih “Simpan sebagai”, tanpa izin penyimpanan
- **PDF**: memakai layanan cetak Android → pilih “Simpan sebagai PDF”; ukuran kertas mengikuti dokumen (F4)
- Pilih logo, font, dan proyek lewat pemilih berkas Android

## Cara 1 — Bangun otomatis di GitHub (tanpa Android Studio)

1. Buat repositori baru di GitHub (boleh privat).
2. Unggah **seluruh isi folder ini** (termasuk folder `.github`) ke repositori itu.
3. Buka tab **Actions → Build APK → Run workflow** (atau tunggu otomatis setelah unggah).
4. Setelah hijau, buka hasil run → bagian **Artifacts** → unduh `Generator-Soal-F4-apk`, ekstrak, lalu pasang `app-debug.apk`.

Pertama kali memasang, izinkan “Pasang dari sumber tidak dikenal” untuk aplikasi yang dipakai membuka berkas.
APK ini bertanda tangan debug — cukup untuk dipakai sendiri; tidak untuk Play Store.

## Cara 2 — Android Studio (di komputer)

1. Buka folder ini lewat **File → Open**.
2. Tunggu sinkronisasi Gradle, lalu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. APK ada di `app/build/outputs/apk/debug/app-debug.apk`.

## Memperbarui isi aplikasi

Ganti `app/src/main/assets/index.html` dengan versi terbaru `generator-soal-f4.html`
(namanya harus tetap `index.html`), lalu bangun ulang. Naikkan `versionCode` di `app/build.gradle`
agar APK baru bisa menimpa yang lama; data pengguna tetap aman.

## Catatan

- Font yang tertanam (Sakkal Majalla, Times New Roman) berlisensi Microsoft — pakai untuk keperluan pribadi dan jangan sebarkan APK-nya.
- Proyek ini disusun tanpa bisa dibangun di lingkungan penyusunnya (tidak ada Android SDK). Jika build gagal,
  kirim pesan galatnya apa adanya.
