package com.example.data.demo

import com.example.data.parser.ParsedPost

data class DemoSiteData(
    val siteUrl: String,
    val posts: List<ParsedPost>
)

object DemoDataProvider {

    const val DEMO_SITE_URL = "kuliner-nusantara.net"

    fun getDemoBackupData(): DemoSiteData {
        val posts = listOf(
            ParsedPost(
                originalId = 101,
                postTitle = "Rahasia Kelezatan Rendang Daging Sapi Khas Minangkabau",
                postContent = """
                    <p>Rendang telah diakui sebagai salah satu hidangan terenak di dunia oleh berbagai survei kuliner internasional. Namun, membuat rendang yang autentik membutuhkan kesabaran luar biasa dan pemahaman filosofis tentang perpaduan rempah.</p>
                    <img src="https://kuliner-nusantara.net/wp-content/uploads/rendang-kuali.jpg" alt="Rendang di Kuali" />
                    <h2>Filosofi Tiga Tahap Memasak Rendang</h2>
                    <p>Masyarakat Minangkabau membagi proses memasak hidangan ini ke dalam tiga fase penting:</p>
                    <ul>
                        <li><strong>Gulai:</strong> Fase pertama saat santan masih encer dan berkuah kuning kecokelatan.</li>
                        <li><strong>Kalio:</strong> Fase kedua ketika santan mulai mengental, berminyak, dan berwarna cokelat kemerahan.</li>
                        <li><strong>Rendang:</strong> Fase akhir saat cairan telah mengering sempurna, bumbu terkaramelisasi menjadi hitam pekat, dan minyak terserap kembali ke serat daging.</li>
                    </ul>
                    <iframe src="https://www.youtube.com/embed/demo-rendang" width="560" height="315"></iframe>
                    <blockquote>"Kunci utama rendang hitam yang tahan berbulan-bulan tanpa bahan pengawet adalah api kecil dan adukan konsisten selama minimal 6 hingga 8 jam."</blockquote>
                    <h2>Komposisi Rempah Wajib</h2>
                    <p>Gunakan kelapa tua parut asli yang diperas tanpa air berlebih. Bumbu halus meliputi bawang merah, bawang putih, jahe, lengkuas, cabai merah keriting, serai, daun jeruk purut, daun kunyit, serta asam kandis.</p>
                """.trimIndent(),
                postDate = "2025-02-14 10:15:00"
            ),
            ParsedPost(
                originalId = 102,
                postTitle = "Menelusuri Riwayat Kuah Hitam Rawon Khas Jawa Timur",
                postContent = """
                    <p>Rawon adalah sup daging berkuah hitam legam yang memiliki cita rasa gurih khas berkat bumbu kluwek (buah kepayang). Sajian tertua di Pulau Jawa ini bahkan telah tercatat dalam prasasti Taji peninggalan era Mataram Kuno abad ke-10.</p>
                    <h2>Memilih Kluwek Berkualitas</h2>
                    <p>Tidak semua kluwek siap digunakan untuk rawon. Berikut tips memilih kluwek yang baik:</p>
                    <ol>
                        <li>Kocok buah kluwek; jika isinya terasa terlepas dan berbunyi, biasanya kualitasnya bagus.</li>
                        <li>Cicipi sedikit daging kluwek sebelum dihaluskan. Jika terasa pahit atau asam tajam, jangan dipakai.</li>
                        <li>Rendam daging kluwek dalam air hangat agar lunak sebelum diulek bersama bumbu dapur lainnya.</li>
                    </ol>
                    <img src="https://kuliner-nusantara.net/wp-content/uploads/rawon-surabaya.jpg" alt="Rawon Daging Sapi" />
                    <h2>Pelengkap Wajib Rawon</h2>
                    <p>Semangkuk rawon panas belum lengkap tanpa kehadiran kecambah pendek mentah, telur asin masir, kerupuk udang, dan sambal terasi pedas manis.</p>
                """.trimIndent(),
                postDate = "2025-02-18 14:30:00"
            ),
            ParsedPost(
                originalId = 103,
                postTitle = "Gudeg Jogja: Manisnya Warisan Sri Sultan Hamengkubuwono",
                postContent = """
                    <p>Gudeg merupakan olahan nangka muda (gori) yang dimasak perlahan bersama santan, gula aren kelapa asli, dan daun jati untuk menghasilkan warna cokelat kemerahan yang khas.</p>
                    <blockquote>"Proses mengungkep gudeg dalam periuk tanah liat membutuhkan waktu semalam suntuk di atas tungku kayu bakar agar rasa manis gurihnya meresap hingga ke tulang ayam kampung."</blockquote>
                    <h2>Gudeg Basah vs Gudeg Kering</h2>
                    <p>Terdapat dua varian utama gudeg di Yogyakarta:</p>
                    <ul>
                        <li><strong>Gudeg Basah:</strong> Disajikan dengan kuah areh santan kental yang gurih melimpah, sering dinikmati sebagai sarapan pagi di daerah Wijilan.</li>
                        <li><strong>Gudeg Kering:</strong> Dimasak lebih lama hingga kadar airnya minim, tahan disimpan beberapa hari dan cocok untuk oleh-oleh perjalanan.</li>
                    </ul>
                    <p>Lauk pendamping yang tak boleh tertinggal meliputi sambal krecek cabai rawit pedas, telur pindang gurih, dan tahu tempe bacem.</p>
                """.trimIndent(),
                postDate = "2025-02-22 09:00:00"
            ),
            ParsedPost(
                originalId = 104,
                postTitle = "Sensasi Cuko Pedas Gurih Pempek Palembang Asli",
                postContent = """
                    <p>Pempek adalah bukti kepiawaian masyarakat Bumi Sriwijaya dalam mengolah ikan sungai seperti tenggiri, belida, dan gabus menjadi hidangan tepung sagu bertekstur kenyal nan gurih.</p>
                    <img src="https://kuliner-nusantara.net/wp-content/uploads/pempek-kapal-selam.jpg" alt="Pempek Kapal Selam" />
                    <h2>Ragam Bentuk dan Karakter Pempek</h2>
                    <p>Dari satu adonan dasar, lahirlah puluhan variasi pempek:</p>
                    <ul>
                        <li><strong>Kapal Selam:</strong> Diisi satu butir telur bebek atau ayam utuh mentah sebelum direbus.</li>
                        <li><strong>Lenjer:</strong> Berbentuk silinder panjang dengan aroma ikan yang sangat kuat.</li>
                        <li><strong>Adaan:</strong> Dicampur santan dan irisan bawang merah lalu langsung digoreng bulat.</li>
                        <li><strong>Kulit:</strong> Dibuat dari campuran kulit ikan cincang dengan tekstur renyah di luar kenyal di dalam.</li>
                    </ul>
                    <h2>Kunci Kuah Cuko yang Mantap</h2>
                    <p>Cuko autentik Palembang dibuat dari gula batok Linggau yang hitam pekat, cabai rawit hijau tumbuk, bawang putih segar, dan asam jawa. Jangan pernah menambahkan cuka kimia sintetis jika ingin cita rasa yang legit dan ramah di lambung.</p>
                """.trimIndent(),
                postDate = "2025-02-25 16:45:00"
            ),
            ParsedPost(
                originalId = 105,
                postTitle = "Kehangatan Coto Makassar dan Ketupat Daun Pandan",
                postContent = """
                    <p>Coto Makassar bukan sekadar soto biasa. Kuahnya yang kaya rempah dan kental menggunakan kaldu daging sapi serta jeroan yang direbus bersama kacang tanah sangrai tumbuk halus.</p>
                    <h2>Rempah Rampah Tradisional</h2>
                    <p>Resep coto menggunakan perpaduan unik 40 jenis rempah (disebut <em>ampah sangka 40</em>) termasuk pala, cengkih, ketumbar, jintan, serai, daun salam, dan lengkuas.</p>
                    <blockquote>"Menyantap Coto Makassar paling pas ditemani ketupat anyaman daun pandan wangi serta sambal tauco dan perasan jeruk nipis segar."</blockquote>
                """.trimIndent(),
                postDate = "2025-03-01 11:20:00"
            ),
            ParsedPost(
                originalId = 106,
                postTitle = "Aroma Harum Sate Lilit Ikan Khas Pulau Dewata",
                postContent = """
                    <p>Berbeda dengan sate pada umumnya yang dagingnya dipotong dadu dan ditusuk, sate lilit dibuat dari daging cincang halus yang dicampur kelapa parut dan bumbu base genep, lalu dililitkan pada batang serai atau bilah bambu pipih.</p>
                    <h2>Mengenal Bumbu Base Genep</h2>
                    <p>Base genep adalah fondasi kuliner Bali yang memadukan empat kelompok rempah:</p>
                    <ul>
                        <li>Rempah hangat: jahe, lengkuas</li>
                        <li>Rempah pedas: cabai rawit, merica hitam, merica putih</li>
                        <li>Rempah manis: pala, cengkih, kayu manis</li>
                        <li>Rempah asam: asam jawa, jeruk limau</li>
                    </ul>
                    <p>Batang serai sebagai tusukan sate memberikan aroma harum segar alami saat dibakar di atas bara arang batok kelapa.</p>
                """.trimIndent(),
                postDate = "2025-03-05 13:10:00"
            ),
            ParsedPost(
                originalId = 107,
                postTitle = "Mie Aceh: Perpaduan Rempah Kari Timur Tengah dan India",
                postContent = """
                    <p>Terletak di ujung barat Nusantara dan menjadi pintu gerbang jalur perdagangan rempah dunia zaman dahulu, Aceh melahirkan hidangan mie kuning tebal berbalut bumbu kari pekat nan menggoda selera.</p>
                    <h2>Pilihan Penyajian Mie Aceh</h2>
                    <p>Pengunjung warung mie di Banda Aceh biasanya memesan dengan tiga cara penyajian:</p>
                    <ol>
                        <li><strong>Mie Goreng Kering:</strong> Ditumis tanpa kuah hingga bumbu meresap kering.</li>
                        <li><strong>Mie Goreng Basah (Nyemek):</strong> Memiliki sedikit kuah kental pekat yang sangat gurih.</li>
                        <li><strong>Mie Kuah:</strong> Disajikan dengan kuah kaldu rempah kari yang melimpah dan hangat.</li>
                    </ol>
                    <p>Tambahan kepiting laut segar, daging sapi empuk, atau udang windu menjadikan sajian ini semakin istimewa dinikmati bersama emping melinjo dan acar bawang merah.</p>
                """.trimIndent(),
                postDate = "2025-03-10 18:00:00"
            ),
            ParsedPost(
                originalId = 108,
                postTitle = "Gurihnya Soto Betawi Kuah Santan dan Susu Segar",
                postContent = """
                    <p>Soto Betawi adalah kuliner legendaris Jakarta yang memadukan potongan daging sapi, babat, dan paru goreng garing dengan kuah creamy yang dibuat dari kombinasi santan kelapa murni dan susu sapi murni.</p>
                    <p>Penggunaan minyak samin saat menumis bumbu memberikan keharuman Timur Tengah yang khas pada kuah soto. Sajian ini ditaburi emping goreng renyah, potongan tomat segar, daun bawang, serta acar mentimun wortel asam segar.</p>
                """.trimIndent(),
                postDate = "2025-03-15 12:00:00"
            ),
            ParsedPost(
                originalId = 109,
                postTitle = "Kearifan Lokal Papeda dan Ikan Kuah Kuning Papua",
                postContent = """
                    <p>Papeda adalah makanan pokok khas masyarakat Papua dan Maluku yang terbuat dari tepung sagu murni yang diseduh dengan air mendidih hingga mengental dan bening elastis.</p>
                    <blockquote>"Menyantap papeda menggunakan sepasang sumpit bambu khusus bernama gata-gata dengan cara digulung lincah, lalu dicelupkan ke dalam mangkuk kuah kuning ikan tongkol atau gabus yang kaya kunyit dan kemangi."</blockquote>
                    <p>Teksturnya yang lembut dingin berpadu kontras dengan kuah ikan yang hangat, asam, segar, dan pedas gurih di lidah.</p>
                """.trimIndent(),
                postDate = "2025-03-20 15:40:00"
            ),
            ParsedPost(
                originalId = 110,
                postTitle = "Ayam Betutu Gilimanuk: Rempah Pedas yang Meresap Sempurna",
                postContent = """
                    <p>Ayam Betutu adalah masakan tradisional Bali berupa ayam atau bebek utuh yang diisi bumbu base genep dan daun singkong, lalu dibungkus pelepah pinang atau daun pisang rapat-rapat.</p>
                    <p>Dahulu, masakan ini dipanggang di dalam lubang tanah dengan sekam padi yang membara lambat selama belasan jam. Hasilnya adalah daging yang luar biasa empuk hingga mudah terlepas dari tulangnya dengan cita rasa pedas rempah yang merasuk hingga ke sumsum.</p>
                """.trimIndent(),
                postDate = "2025-03-24 17:25:00"
            ),
            ParsedPost(
                originalId = 111,
                postTitle = "Nasi Liwet Solo: Kelembutan Nasi Gurih Beraroma Pandan",
                postContent = """
                    <p>Nasi liwet adalah sajian khas Solo yang dimasak dengan santan kelapa, kaldu ayam kampung, daun salam, dan serai. Keunikannya terletak pada topping areh santan kental yang disiramkan di atas nasi hangat beralaskan daun pisang pincuk.</p>
                    <p>Lauk pendamping liwet Solo terdiri dari sayur labu siam pedas, suwiran ayam opor lembut, dan telur pindang berbumbu kecap manis gurih.</p>
                """.trimIndent(),
                postDate = "2025-03-28 08:30:00"
            ),
            ParsedPost(
                originalId = 112,
                postTitle = "Bika Ambon Medan: Tekstur Bersarang yang Memikat",
                postContent = """
                    <p>Meskipun menyandang nama Ambon, kue basah berwarna kuning keemasan ini merupakan oleh-oleh ikonik dari Kota Medan, Sumatera Utara.</p>
                    <p>Rahasia tekstur berongga sarang tawon yang kenyal dan berserat terletak pada fermentasi adonan menggunakan tuak nira aren atau air kelapa segar selama berjam-jam sebelum dipanggang perlahan dengan api bawah.</p>
                """.trimIndent(),
                postDate = "2025-04-02 10:50:00"
            ),
            ParsedPost(
                originalId = 113,
                postTitle = "Kerak Telor: Gurih Renyah Warisan Budaya Batavia",
                postContent = """
                    <p>Kerak telor adalah camilan kuno khas Betawi yang dimasak di atas wajan cekung kecil tanpa minyak menggunakan arang batok kelapa. Terbuat dari beras ketan putih, telur bebek atau ayam, ebi sangrai, dan serundeng kelapa pedas gurih.</p>
                    <p>Ketika adonan setengah matang, wajan sengaja dibalik menghadap bara api arang secara langsung untuk menghasilkan kerak yang garing renyah dan beraroma harum asap alami.</p>
                """.trimIndent(),
                postDate = "2025-04-08 16:15:00"
            ),
            ParsedPost(
                originalId = 114,
                postTitle = "Segarnya Es Pisang Ijo Khas Pesisir Makassar",
                postContent = """
                    <p>Es Pisang Ijo memadukan pisang raja matang yang dibalut kulit adonan tepung beras pandan hijau cerah. Disajikan dalam mangkuk es serut dingin dengan siraman bubur sumsum lembut, sirup merah DHT aroma pisang ambon, dan susu kental manis gurih.</p>
                    <p>Sangat populer sebagai menu berbuka puasa di bulan Ramadhan dan pelepas dahaga di terik siang hari.</p>
                """.trimIndent(),
                postDate = "2025-04-12 13:40:00"
            ),
            ParsedPost(
                originalId = 115,
                postTitle = "Klappertaart Manado: Sentuhan Manis Akulturasi Hindia Belanda",
                postContent = """
                    <p>Klappertaart adalah kue puding panggang khas Manado yang kaya akan kelapa muda, susu, mentega, dan taburan kacang kenari serta kismis dengan taburan kayu manis bubuk.</p>
                    <p>Dapat dinikmati dalam kondisi dingin langsung dari lemari es dengan tekstur lembut meleleh layaknya custard mewah.</p>
                """.trimIndent(),
                postDate = "2025-04-16 11:05:00"
            )
        )

        return DemoSiteData(
            siteUrl = DEMO_SITE_URL,
            posts = posts
        )
    }
}
