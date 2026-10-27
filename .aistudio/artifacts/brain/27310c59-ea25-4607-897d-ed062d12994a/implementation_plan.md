# BacaDulu - WordPress Database Backup (.db.gz) Offline Reader

Aplikasi Android modern berbasis Jetpack Compose dan SQLite lokal (Room) untuk membaca arsip backup database WordPress (`.db.gz` dari UpdraftPlus) secara luring (offline) tanpa media. Menyajikan pengalaman membaca artikel yang cepat, nyaman di mata, dan bebas gangguan dengan dukungan multi-situs, pencarian, bookmark, dan ekspor data.

## User Review & Critical Decisions

> [!IMPORTANT]
> Ringkasan keputusan berdasarkan preferensi yang telah dikonfirmasi:

- **Contoh Data Siap Pakai**: Disertakan 1 database situs contoh ("Kuliner Nusantara" dengan puluhan artikel informatif) yang dapat dimuat langsung dengan 1 ketukan pada layar pembuka, sehingga aplikasi langsung dapat dicoba tanpa harus memindahkan file `.db.gz` terlebih dahulu.
- **Deteksi Prefix Tabel Fleksibel**: Parser membaca tabel SQL secara cerdas untuk prefix apapun yang berakhiran `_posts` dan `_options` (seperti `wp_posts`, `wp_abcd_posts`), mengekstrak `siteurl` dan postingan bertipe `post` berstatus `publish`.
- **Tema & Tampilan Baca**: Menyediakan toggle instan Light & Dark mode, serta palet Sepia kertas klasik, kontrol ukuran font slider/+/- (14sp - 28sp), jenis font (Sans, Serif, Monospace), serta tombol reset pengaturan.
- **Pembersihan Konten Visual**: Tag `<img>` dan `<iframe>` disaring dari tampilan baca untuk menjaga kenyamanan tanpa media, sedangkan data asli di database tetap tersimpan 100% utuh dengan opsi "Lihat Asli".

---

## 1. Overview & Core Concept

**BacaDulu** adalah pembaca arsip WordPress offline yang mengubah file backup database `.db.gz` berukuran besar menjadi perpustakaan artikel lokal yang ringan dan terindeks. Pengguna dapat mengimpor banyak file backup website sekaligus, membaca koleksi artikel secara acak (discovery mode), mencari pos berdasarkan judul secara alfabetis (A-Z), menandai pos favorit, serta membaca artikel dengan kontrol tipografi penuh.

### Persona & Nilai Utama
- **Target Pengguna**: Pemilik blog, arsiparis konten, jurnalis, atau pembaca setia yang ingin menyelamatkan, membaca ulang, atau mengkaji artikel website lawas tanpa perlu menyalakan web server (Apache/Nginx/MySQL/PHP).
- **Keuntungan Utama**:
  1. *Zero Server Dependency*: Tidak butuh hosting atau localhost XAMPP.
  2. *Low Resource*: Ekstraksi streaming GZIP langsung ke SQLite dalam batch transaction tanpa boros RAM.
  3. *Distraction-Free Reading*: Artikel disajikan rapi tanpa iklan, gambar rusak (broken image), atau skrip pelacak.

---

## 2. User Experience & Visual Design

### Alur Antarmuka Pengguna (Key User Flows)

```
┌────────────────────────────────────────────────────────────────────────┐
│                              BACADULU                                  │
│                                                                        │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────────────┐  │
│  │  Home (Acak)   │  │   Cari & Simpan│  │    Kelola Situs          │  │
│  │ 15 Pos Acak    │  │ • A-Z Search   │  │ • Impor File .db.gz      │  │
│  │ • Acak Ulang 🔀│  │ • Bookmark 🔖  │  │ • Muat Demo Website      │  │
│  │ • Buka Baca 📖 │  │ • Filter Cepat │  │ • Ekspor Database .db 💾 │  │
│  └───────┬────────┘  └───────┬────────┘  └─────────────┬────────────┘  │
│          │                   │                         │               │
│          └─────────────┬─────┴─────────────────────────┘               │
│                        ▼                                               │
│             ┌────────────────────────┐                                 │
│             │  Halaman Baca (Reader) │                                 │
│             │ • Next / Prev Navigasi │                                 │
│             │ • Pengaturan Font & Bg │                                 │
│             │ • Toggle Light / Dark  │                                 │
│             │ • Tombol "Lihat Asli"  │                                 │
│             └────────────────────────┘                                 │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Halaman Home (Beranda)**:
   - Menampilkan 15 kartu artikel pilihan acak dari seluruh situs yang telah diimpor.
   - Tombol "Acak Ulang" (Floating Action Button / Top bar action) untuk mengacak kembali 15 pos baru.
   - Setiap kartu menampilkan: Judul pos tebal, chip nama domain situs sumber, tanggal publikasi, dan tombol bookmark sekali-ketuk.

2. **Halaman Cari & Bookmark**:
   - Tab ganda: **Pencarian A-Z** dan **Bookmark**.
   - Input pencarian langsung menyaring pos berdasarkan judul dengan urutan alfabetis.
   - Tab bookmark menampilkan pos tersimpan dengan urutan penambahan terbaru di atas (`bookmarked_at DESC`).

3. **Halaman Kelola Data & Impor (Website Manager)**:
   - Ringkasan total situs dan total pos tersimpan.
   - Tombol **"Impor File .db.gz"** membuka pemilih dokumen Android (SAF).
   - Dialog deteksi duplikasi: jika URL situs sudah ada, menampilkan perbandingan:
     - Jumlah pos lokal saat ini vs jumlah pos di file backup baru.
     - Pilihan: **[Lewati / Batal]** atau **[Timpa / Perbarui]**.
   - Tombol **"Muat Contoh Data"**: Langsung mengisi database dengan demo blog kuliner untuk uji coba cepat.
   - Tombol **"Ekspor Database (.db)"**: Menyimpan salinan database lokal ke folder Download HP.
   - Daftar situs terdaftar: nama domain, total pos, tanggal impor, serta tombol hapus per website.

4. **Halaman Baca (Single Post View)**:
   - Bilah atas bersih dengan tombol kembali, bookmark, dan ikon Pengaturan (⚙️).
   - **Dialog Pengaturan Teks**:
     - Pilihan Font: Sans (Modern), Serif (Buku/Editorial), Monospace (Koding).
     - Ukuran Teks: Tombol **A-** dan **A+** serta slider (14sp s.d. 28sp).
     - Tema Warna: Toggle Terang, Gelap, Sepia (kertas krem), dan Custom.
     - Tombol "Reset" untuk mengembalikan ke konfigurasi bawaan.
   - Navigasi artikel: Bar navigasi bawah menampilkan tombol **"Sebelumnya"** dan **"Berikutnya"** berdasarkan urutan artikel dalam situs yang sama.
   - Bagian bawah konten: Tombol **"Lihat Konten Asli (Raw HTML)"** untuk beralih antara format baca bersih dan kode HTML mentah.

---

## 3. Key Product Decisions & Trade-Offs

- **Streaming Gzip Parser vs In-Memory Extraction**:
  - *Pendekatan*: Menggunakan `GZIPInputStream` berpasangan dengan `BufferedReader` untuk memproses baris-per-baris perintah SQL `INSERT INTO ...`.
  - *Alasan*: File backup UpdraftPlus dapat berukuran puluhan megabyte (atau lebih). Mengekstrak seluruh teks ke memori dapat menyebabkan OutOfMemoryError (OOM) pada Android.
  - *Batch Transaction*: Menyimpan pos dalam potongan batch (100 baris per batch) di dalam satu transaksi SQLite untuk kecepatan impor maksimal.
- **Deteksi Otomatis Prefix Tabel**:
  - *Pendekatan*: Regex memeriksa tabel yang berakhiran `_options` (untuk mengambil `siteurl`) dan `_posts` (untuk mengambil baris pos publikasi).
  - *Alasan*: Banyak instalasi WordPress menggunakan prefix kustom (seperti `wp9x_posts` atau `wp_custom_posts`) demi keamanan.
- **Pembersihan HTML Visual**:
  - *Pendekatan*: Konten mentah disimpan persis tanpa diubah di kolom `post_content`. Parser tampilan membersihkan tag `<img>`, `<picture>`, dan `<iframe>`, serta mengonversi tag teks seperti `<h1>`-`<h6>`, `<p>`, `<blockquote>`, `<ul>`, `<ol>`, `<li>`, `<a>`, `<strong>`, `<em>` menjadi visual yang estetik.
  - *Alasan*: Pengguna tetap memiliki akses 100% ke data mentah (lewat tombol "Lihat Asli"), sementara tampilan baca tetap nyaman dan bebas error gambar rusak offline.

---

## 4. Technical Architecture & Data Strategy

### Diagram Arsitektur & Komponen

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI                              │
│                                                                        │
│   HomeScreen       SearchBookmarkScreen      ManageSitesScreen         │
│   (Random 15)      (A-Z & Bookmarks)         (Import & Export)         │
│        ▲                   ▲                         ▲                 │
│        │                   │                         │                 │
│        └───────────────────┼─────────────────────────┘                 │
│                            │                                           │
│                            ▼                                           │
│                     PostReaderScreen                                   │
│                  (Custom Reader Theme)                                 │
└────────────────────────────┬───────────────────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       BacaDuluViewModel                                │
│   • Multi-site query state       • Post navigation state (Next/Prev)   │
│   • Gzip Import Progress Flow    • Reader Preferences (DataStore/Prefs)│
└────────────────────────────┬───────────────────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     WordPressArchiveRepository                         │
│   • Stream parse .db.gz / .sql.gz (GZIPInputStream)                   │
│   • Extract siteurl from wp_options                                    │
│   • Batch insert posts inside SQLite Transaction                       │
│   • Export App SQLite file to Download directory                       │
└────────────────────────────┬───────────────────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────────────────┐
│                 Room Database (bacadulu_database.db)                   │
│                                                                        │
│   TABLE `sites`                                                        │
│   - id (PK AUTOINCREMENT)                                              │
│   - site_url (TEXT)                                                    │
│   - total_posts (INTEGER)                                              │
│   - imported_at (INTEGER / TIMESTAMP)                                  │
│                                                                        │
│   TABLE `posts`                                                        │
│   - id (PK AUTOINCREMENT)                                              │
│   - site_id (INTEGER FK -> sites.id ON DELETE CASCADE)                │
│   - unique_key (TEXT UNIQUE)                                           │
│   - original_id (INTEGER)                                              │
│   - post_title (TEXT)                                                  │
│   - post_content (TEXT)                                                │
│   - post_date (TEXT)                                                   │
│   - is_bookmarked (INTEGER DEFAULT 0)                                  │
│   - bookmarked_at (INTEGER NULL)                                       │
└────────────────────────────────────────────────────────────────────────┘
```

### Rencana File & Modul:
1. **Model & Entitas Database**:
   - `SiteEntity.kt`: Model tabel `sites`.
   - `PostEntity.kt`: Model tabel `posts` beserta foreign key index ke `sites`.
   - `BacaDuluDao.kt`: Query SQL untuk 15 pos acak, pencarian A-Z, bookmark, pos per situs, prev/next pos, dan batch insert.
   - `BacaDuluDatabase.kt`: Abstract Room database.
2. **Parser & Importer Engine**:
   - `WordPressDumpParser.kt`: Streaming parser membaca stream gzipped/SQL, mengekstrak option `siteurl`, dan mengekstrak entri `wp_posts` (`post_type='post'`, `post_status='publish'`).
   - `DemoDataProvider.kt`: Data sample website blog kuliner nusantara untuk pengujian instan.
3. **Repository & Export Service**:
   - `ArchiveRepository.kt`: Mengelola transaksi impor, pengecekan duplikasi, hapus data situs, dan utilitas ekspor database `.db` ke folder Download.
4. **UI & Komponen Baca**:
   - `ReaderPreferences.kt`: Pengaturan font, ukuran teks, dan skema warna baca.
   - `HtmlContentRenderer.kt`: Pemformat HTML bersih tanpa gambar/iframe.
   - `MainScreens.kt`: Halaman Beranda (15 Acak), Cari & Bookmark, Kelola Situs (Import/Export/List).
   - `PostReaderView.kt`: Halaman pembaca artikel lengkap dengan bar navigasi, modal pengaturan tipografi, dan toggle "Lihat Asli".
5. **App Icon & Branding**:
   - Adaptive icon buku terbuka berwarna-warni sesuai spesifikasi.
   - Sinkronisasi metadata `BacaDulu` di `metadata.json` dan `strings.xml`.
