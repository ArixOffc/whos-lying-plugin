# 🎮 Who's Lying? - Minecraft Plugin

**Who's Lying?** adalah plugin Minecraft game yang terinspirasi dari game Roblox. Dalam game ini, pemain harus menemukan **Impostor** di antara mereka yaaa mbutt

---

## 🎭 PERAN PEMAIN

### 🕵️ INVESTIGATOR (Mayoritas Player)
- ✅ Mengetahui **kategori** dan **kata rahasia**
- ✅ Harus memberikan deskripsi/clue tentang kata tersebut
- ❌ **TIDAK boleh** menyebutkan kata secara langsung
- 🎯 **Tujuan:** Identifikasi Impostor melalui voting

### 🤥 IMPOSTOR (1 Player)
- ✅ Hanya tahu **kategori** saja
- ❌ **TIDAK tahu** kata rahasianya
- ✅ Harus berpura-pura tahu kata berdasarkan clue dari investigator
- 🎯 **Tujuan:** Tidak ketahuan saat voting
- 🏆 **Bonus:** Jika berhasil menebak kata rahasia setelah ketahuan, Impostor tetap menang!

---

## 🎮 CARA BERMAIN LENGKAP

### 📋 FASE 1: REGISTRASI (Moderator)

**Sistem:** Moderator mendaftarkan player secara manual (seperti SambungKata)

```bash
# Moderator daftar player satu per satu
/regis Steve
/regis Alex
/regis Herobrine

# Cek daftar player
/listplayer

# Hapus player jika salah
/unregis Steve

# Minimum 3 player, maksimum 8 player
```

**Output:**
```
Cora berhasil didaftarkan! (1/8)
Amrik berhasil didaftarkan! (2/8)
Gatw berhasil didaftarkan! (3/8)
```

---

### 🏗️ FASE 2: START GAME

```bash
# Moderator berdiri di lokasi yang diinginkan
# Arena akan dibangun otomatis di posisi moderator
/start
```

**Yang Terjadi:**
1. ⚙️ **Arena dibangun otomatis** (meja bundar + kursi sesuai jumlah player)
2. 🎰 **Gacha urutan** - Animasi acak urutan duduk player (1 detik)
3. 🪑 **Mannequin spawned** - Avatar player duduk di kursi menghadap meja
4. 🎭 **Role assigned** - 1 player jadi Impostor (rahasia!)
5. 📝 **Kata dipilih** - Random kategori + kata
6. 📨 **Info dikirim private** ke masing-masing player

**Contoh Private Message:**
- **Investigator:** `🕵️ INVESTIGATOR | Kategori: Animals | Kata: Dog`
- **Impostor:** `🤥 IMPOSTOR | Kategori: Animals | Kata: ???`

---

### 💬 FASE 3: DESCRIPTION PHASE (Giliran Bicara)

**Sistem:** Semua player berbicara 1x per ronde, urutan sudah ditentukan dari gacha.

#### Saat Giliran Player:
- 📹 **Kamera semua player** otomatis fokus ke player yang giliran (animasi smooth)
- ⏰ **Timer 60 detik** per player
- 💬 **Player ketik deskripsi** di chat
- 🚫 **Player lain tidak bisa chat** (blocked)
- 📢 **Deskripsi di-broadcast** ke semua player
- 📝 **Text display** di atas mannequin menampilkan kata yang diketik

#### Contoh Giliran:

**Giliran Steve:**
```
System: "Giliran Steve untuk Menjawab"
🎬 [Camera animate ke Steve]
⏰ [Timer: 60 detik]

Steve (chat): "It barks"
✅ [Word filter: pass]
📢 [Steve] It barks
📝 [Text display above Steve's mannequin: "It barks"]
```

**Giliran Herobrine (Impostor):**
```
System: "Giliran Herobrine untuk Menjawab"
🎬 [Camera animate ke Herobrine]

Herobrine (chat): "It has four legs"
📢 [Herobrine] It has four legs
💭 (Herobrine tidak tahu kata "Dog", jadi kasih clue umum)
```

**Giliran Alex:**
```
System: "Giliran Alex untuk Menjawab"
🎬 [Camera animate ke Alex]

Alex (chat): "Man's best friend"
📢 [Alex] Man's best friend
```

#### Aturan Description:
- ✅ Boleh kasih clue/deskripsi apa saja
- ❌ **TIDAK BOLEH** sebut kata rahasia langsung (word filter aktif)
- ⏰ Jika timer habis → giliran di-skip
- 🔄 Setelah semua player bicara → lanjut Discussion

---

### 🗣️ FASE 4: DISCUSSION PHASE (Diskusi Bebas)

**Sistem:** Free chat untuk semua player, diskusikan siapa yang mencurigakan!

```
System: "═══ FASE DISKUSI =══"
System: "Diskusikan siapa yang mencurigakan!"
System: "Waktu: 60 detik"

🎬 [Camera ke center/tetap di last player]
⏰ [Timer 60 detik]
💬 [Free chat enabled]
```

**Contoh Chat:**
```
[DISKUSI] Steve: Herobrine sus, too generic
[DISKUSI] Alex: Yeah I agree
[DISKUSI] Herobrine: No way! It's accurate!
[DISKUSI] Steve: Let's vote him
```

**Action Bar:** `💬 Diskusi: 30 detik tersisa`

---

### ✅ FASE 5: VOTE CHECK (Mau Vote Sekarang?)

**Sistem:** Player pilih apakah mau voting sekarang atau lanjut ronde baru.

```
System: "═══ VOTE CHECK =══"
System: "Mulai voting sekarang?"
System: "Ketik Y (Yes) atau N (No) di chat!"
```

**Player Response:**
```
Steve: Y
System: "Respon dicatat: Y"

Alex: Y
System: "Respon dicatat: Y"

Herobrine: N
System: "Respon dicatat: N"

System: "2 YES, 1 NO"
System: "Mayoritas setuju! Voting dimulai!"
```

**Hasil:**
- **Majority YES** → Lanjut ke Voting Phase
- **Majority NO** → Kembali ke Description Phase (Ronde 2)
- ⚠️ **Force Vote:** Setelah 5 ronde tanpa vote, voting dipaksa otomatis

---

### 🗳️ FASE 6: VOTING PHASE (Vote Impostor)

**Sistem:** Vote siapa yang menurut kamu Impostor!

```
System: "═══ FASE VOTING =══"
System: "Vote siapa yang menurut kamu IMPOSTOR!"
System: "Gunakan: /vote <nama>"
System: "Waktu: 30 detik"
```

**Player Vote:**
```
Steve: /vote Herobrine
System: "Kamu vote: Herobrine"
System: "Steve telah vote. (1/3)"

Alex: /vote Herobrine
System: "Kamu vote: Herobrine"
System: "Alex telah vote. (2/3)"

Herobrine: /vote Steve
System: "Kamu vote: Steve"
System: "Herobrine telah vote. (3/3)"
```

**Aturan Voting:**
- ✅ Setiap player vote **1 kali**
- ❌ Tidak bisa vote diri sendiri
- ⏰ Timer 30 detik
- 🔄 Jika seri (tie) → random selection dari yang seri

---

### 🎉 FASE 7: REVEAL (Hasil & Pemenang)

**Hasil Voting:**
```
System: "═══ HASIL VOTING =══"
System: "Hasil:"
System: "  Herobrine: 2 suara"
System: "  Steve: 1 suara"
System: ""
System: "Herobrine telah dikick dari game!"
System: "Apakah dia Impostor...?"
```

**Scenario 1: Impostor Ketahuan**
```
System: "✓ YA! Dia adalah IMPOSTOR!"
System: "INVESTIGATOR MENANG!"
🎊 +50 XP untuk Cora
🎊 +50 XP untuk amrik
```

**Scenario 2: Salah Vote (Bukan Impostor)**
```
System: "✗ BUKAN! Dia adalah Investigator!"
System: "Impostor mendapat kesempatan menebak kata!"
System: "Impostor, ketik tebakanmu di chat dalam 15 detik!"

Impostor: Dog

System: "Impostor menebak: Dog"
System: "✓ BENAR!"
System: "IMPOSTOR MENANG!"
🎊 +150 XP untuk Impostor
```

**Final Reveal:**
```
System: "Impostor: Herobrine"
System: "Kategori: Animals"
System: "Kata: Dog"
```

---

### 🔄 FASE 8: NEXT ROUND atau END GAME

```
# Lanjut ronde baru (arena tetap, new roles, new word)
/nextround

# Atau akhiri game total
/endgame
```

**Jika `/nextround`:**
- ✅ Arena tetap di tempat yang sama
- ✅ Player yang sama (masih terdaftar)
- 🔄 Gacha urutan baru
- 🎭 Role baru (Impostor beda)
- 📝 Kata baru
- 🎬 Ulangi dari Description Phase

**Jika `/endgame`:**
- 🏆 Tampilkan leaderboard final
- 🧹 Restore semua block ke aslinya
- 🗑️ Remove semua mannequin & entities
- 👤 Restore player state (gamemode, inventory, dll)
- 🔙 Kembali ke IDLE (siap `/start` lagi)

---

## 📋 COMMANDS LENGKAP

### 🎮 Player Commands (Tidak Ada - Registrasi via Moderator)

Plugin ini menggunakan sistem registrasi moderator (seperti SambungKata). **Player TIDAK self-join**, melainkan didaftarkan oleh moderator.

### 👨‍💼 Moderator Commands

| Command | Permission | Deskripsi |
|---------|-----------|-----------|
| `/regis <nama>` | `whoislying.admin` | Daftarkan player ke game (max 8) |
| `/unregis <nama>` | `whoislying.admin` | Hapus player dari daftar |
| `/listplayer` | `whoislying.admin` | Lihat daftar player terdaftar |
| `/listscore` | `whoislying.admin` | Lihat leaderboard sementara |
| `/start` | `whoislying.admin` | Mulai game (build arena + gacha) |
| `/nextround` | `whoislying.admin` | Mulai ronde baru setelah ronde selesai |
| `/endgame` | `whoislying.admin` | Akhiri game dan restore semua |
| `/resetgame` | `whoislying.admin` | Reset game ke awal (emergency) |
| `/skip` | `whoislying.admin` | Skip giliran player saat ini |

### 💬 In-Game Commands (Player)

| Command | Kapan | Deskripsi |
|---------|-------|-----------|
| `(chat)` | Description Phase | Ketik deskripsi saat giliran kamu |
| `(chat)` | Discussion Phase | Chat bebas dengan prefix [DISKUSI] |
| `Y` atau `N` | Vote Check Phase | Pilih mau voting atau tidak |
| `/vote <nama>` | Voting Phase | Vote player yang kamu curigai |
| `(chat)` | Impostor Guess | Impostor ketik tebakan kata |

---

## 📖 CONTOH SESSION LENGKAP

```
═══════════════════════════════════════
         REGISTRASI PHASE
═══════════════════════════════════════
Moderator: /regis Steve
System: Steve berhasil didaftarkan! (1/8)

Moderator: /regis Alex
System: Alex berhasil didaftarkan! (2/8)

Moderator: /regis Herobrine
System: Herobrine berhasil didaftarkan! (3/8)

Moderator: /listplayer
System: ══ Daftar Player ══
System:   1. Steve
System:   2. Alex
System:   3. Herobrine
System: Total: 3 player

═══════════════════════════════════════
         START GAME
═══════════════════════════════════════
Moderator: /start (at X=100, Y=64, Z=200)

[🏗️ Arena building...]
- Floor: 9x9 grass blocks
- Table: Spruce planks + stairs
- Chairs: 3x spruce slabs

[🎰 Gacha animation - 1 second]
Steve:     "Kamu berada pada urutan ke: 2" (random)
Alex:      "Kamu berada pada urutan ke: 1" (random)
Herobrine: "Kamu berada pada urutan ke: 3" (random)

[Final Result]
Steve:     "Kamu berada pada urutan ke: 1"
Alex:      "Kamu berada pada urutan ke: 3"
Herobrine: "Kamu berada pada urutan ke: 2"

[🪑 Spawning mannequins...]
- Steve's mannequin at chair #1
- Herobrine's mannequin at chair #2
- Alex's mannequin at chair #3

[🎭 Role Assignment]
Random pick: Herobrine = IMPOSTOR

[📨 Private messages]
To Steve:     "🕵️ INVESTIGATOR | Kategori: Animals | Kata: Dog"
To Alex:      "🕵️ INVESTIGATOR | Kategori: Animals | Kata: Dog"
To Herobrine: "🤥 IMPOSTOR | Kategori: Animals | Kata: ???"

═══════════════════════════════════════
         DESCRIPTION PHASE - ROUND 1
═══════════════════════════════════════
[📹 Camera snaps to Steve]
System: "Giliran Steve untuk Menjawab"
⏰ Timer: 60s

Steve: "It barks"
System: [Steve] It barks
[Text display above Steve: "It barks"]

[📹 Camera animates to Herobrine - smooth transition]
System: "Giliran Herobrine untuk Menjawab"

Herobrine: "It has four legs"
System: [Herobrine] It has four legs
[Text display above Herobrine: "It has four legs"]

[📹 Camera animates to Alex]
System: "Giliran Alex untuk Menjawab"

Alex: "Man's best friend"
System: [Alex] Man's best friend
[Text display above Alex: "Man's best friend"]

═══════════════════════════════════════
         DISCUSSION PHASE
═══════════════════════════════════════
System: "═══ FASE DISKUSI =══"
System: "Diskusikan siapa yang mencurigakan!"
System: "Waktu: 60 detik"

Steve: "Herobrine sus, too generic"
[DISKUSI] Steve: Herobrine sus, too generic

Alex: "Yeah I agree"
[DISKUSI] Alex: Yeah I agree

Herobrine: "No way! It's accurate!"
[DISKUSI] Herobrine: No way! It's accurate!

Action Bar: "💬 Diskusi: 30 detik tersisa"

═══════════════════════════════════════
         VOTE CHECK PHASE
═══════════════════════════════════════
System: "═══ VOTE CHECK =══"
System: "Mulai voting sekarang?"
System: "Ketik Y (Yes) atau N (No) di chat!"

Steve: Y
System: "Respon dicatat: Y"

Alex: Y
System: "Respon dicatat: Y"

Herobrine: N
System: "Respon dicatat: N"

System: "Mayoritas setuju! Voting dimulai!"

═══════════════════════════════════════
         VOTING PHASE
═══════════════════════════════════════
System: "═══ FASE VOTING =══"
System: "Vote siapa yang menurut kamu IMPOSTOR!"
System: "Gunakan: /vote <nama>"

Steve: /vote Herobrine
System: "Kamu vote: Herobrine"

Alex: /vote Herobrine
System: "Kamu vote: Herobrine"

Herobrine: /vote Steve
System: "Kamu vote: Steve"

═══════════════════════════════════════
         REVEAL PHASE
═══════════════════════════════════════
System: "═══ HASIL VOTING =══"
System: "Hasil:"
System: "  Herobrine: 2 suara"
System: "  Steve: 1 suara"
System: ""
System: "Herobrine telah dikick dari game!"
System: "Apakah dia Impostor...?"

[3 detik pause untuk drama...]

System: "✓ YA! Dia adalah IMPOSTOR!"
System: "INVESTIGATOR MENANG!"

System: ""
System: "Impostor: Herobrine"
System: "Kategori: Animals"
System: "Kata: Dog"

[Rewards]
+50 XP to Steve
+50 XP to Alex

System: "══════════════════"
System: "Gunakan /nextround untuk ronde berikutnya atau /endgame untuk mengakhiri."

═══════════════════════════════════════
         NEXT ROUND atau END GAME
═══════════════════════════════════════
Moderator: /nextround
[New gacha, new roles, new word, repeat!]

Moderator: /endgame
System: "══════════════════"
System: "  PERMAINAN BERAKHIR"
System: "══ LEADERBOARD FINAL ══"
System: "1. Steve - 5 poin"
System: "2. Alex - 3 poin"
System: "3. Herobrine - 1 poin"
System: "══════════════════"

[🧹 Cleanup]
- Restore all blocks
- Remove all mannequins
- Restore player states
```

---

## ⚙️ KONFIGURASI

### config.yml

```yaml
min-players: 3              # Minimum pemain untuk mulai
max-players: 8              # Maksimum pemain (3-8)
description-time-seconds: 60  # Waktu per giliran bicara
discussion-time-seconds: 60   # Waktu diskusi
voting-time-seconds: 30       # Waktu voting
impostor-guess-time: 15       # Waktu impostor menebak kata
max-rounds-before-force-vote: 5  # Ronde maksimal sebelum vote dipaksa

rewards:
  investigator-win-xp: 50     # XP untuk investigator menang
  impostor-guess-xp: 150      # XP untuk impostor menebak benar

word-filter:
  enabled: true
  warn-message: "&cKamu tidak boleh menyebutkan kata rahasia secara langsung!"
```

**Note:** Arena dibangun otomatis di posisi moderator saat `/start`, tidak perlu setup manual.

### words.yml

File ini berisi database kata-kata yang digunakan dalam game. Format:

```yaml
categories:
  Animals:
    - Dog
    - Cat
    - Elephant
    # ... dst
  Food:
    - Pizza
    - Sushi
    # ... dst
```

Plugin sudah include **10+ kategori** dengan **15+ kata per kategori**.

---

## 🎨 FITUR UTAMA

### ✅ Adopsi Sistem dari SambungKata
- 👥 **Registrasi Moderator** - `/regis` untuk daftar player
- 🏗️ **Auto Arena Building** - Meja bundar + kursi otomatis
- 🎰 **Gacha Urutan** - Animasi acak urutan duduk player
- 🪑 **Mannequin System** - Avatar player dengan skin asli
- 📹 **Camera System** - Snap & animate smooth antar player
- 📝 **Text Display** - Live update deskripsi di atas kepala
- 👻 **Player Invisibility** - Player invisible, hanya mannequin terlihat
- 🧹 **Auto Cleanup** - Restore blocks & entities setelah game

### ✅ Game Mechanics Who's Lying
- 🎭 **Role System** - 1 Impostor vs Investigator
- 🔤 **Word Database** - 10+ kategori, 15+ kata per kategori
- 📨 **Private Info** - Investigator tahu kata, Impostor tidak
- 💬 **4 Fase Gameplay** - Description, Discussion, Vote Check, Voting
- 🗳️ **Democratic Voting** - Vote sistem dengan tie-breaker
- 🎯 **Impostor Guess** - Kesempatan menebak jika ketahuan
- 🚫 **Word Filter** - Cegah sebut kata langsung
- 🔄 **Multi-round Support** - `/nextround` untuk ronde baru
- 🏆 **Score System** - Track kemenangan per player
- 🎵 **Sound Effects** - Suara untuk setiap fase

---

## 🔧 BUILD & INSTALL

### Requirements
- ☕ Java 17+
- 📦 Maven
- 🎮 Spigot/Paper 1.20.1+

### Build dari Source

**Windows:**
```bash
cd WhoIsLying
build.bat
```

**Linux/Mac:**
```bash
cd WhoIsLying
mvn clean package
```

Output: `target/WhoIsLying-1.0.0.jar`

### Install ke Server

```bash
# 1. Copy JAR ke folder plugins
cp target/WhoIsLying-1.0.0.jar /path/to/server/plugins/

# 2. Restart server
# Atau reload: /reload confirm

# 3. Plugin siap digunakan!
```

**Config & words.yml** akan auto-generate saat plugin pertama kali load.

---

## 🎯 TIPS BERMAIN

### Untuk Investigator 🕵️
- ✅ Berikan clue yang **cukup jelas** untuk investigator lain
- ✅ **Jangan terlalu spesifik** agar Impostor tidak bisa menebak
- ✅ Perhatikan siapa yang memberikan clue **aneh/tidak relevan**
- ✅ **Koordinasi** dengan investigator lain di fase diskusi
- ✅ **Analisis pola** - Impostor biasanya kasih clue umum

### Untuk Impostor 🤥
- ✅ **Dengarkan baik-baik** clue dari investigator pertama
- ✅ Berikan clue yang **masih dalam kategori** tapi tidak terlalu spesifik
- ✅ **Jangan terlalu menonjol** atau terlalu diam
- ✅ **Ikuti pola** clue dari investigator lain
- ✅ Jika ketahuan, **usahakan tebak kata dengan benar!**

### Untuk Moderator �‍💼
- ✅ Daftar player sebelum `/start`
- ✅ Berdiri di **area kosong** saat `/start` (arena auto-build)
- ✅ Gunakan `/skip` jika player offline/AFK saat giliran
- ✅ Gunakan `/resetgame` jika terjadi error/bug
- ✅ Gunakan `/nextround` untuk lanjut ronde baru
- ✅ Gunakan `/endgame` saat mau selesai total

---

## 📚 DOKUMENTASI LENGKAP

- 📖 **README.md** (file ini) - Overview & cara main
- 🎬 **PROJECT_FLOW.md** - Flow lengkap dengan sistem SambungKata
- 🎮 **GAME_MECHANICS.md** - Mekanik game detail
- 🔧 **TROUBLESHOOTING.md** - Problem solving
- ✅ **IMPLEMENTATION_CHECKLIST.md** - Checklist fitur
- 🚀 **QUICKSTART.md** - Quick start guide

---

## 🐛 TROUBLESHOOTING CEPAT

### Plugin tidak load?
```bash
# Check Java version (harus 17+)
java -version

# Check server version (harus 1.20.1+)
/version
```

### Arena tidak terbuild?
- ✅ Pastikan moderator di area **kosong** (tidak ada block penting)
- ✅ Arena membutuhkan area **9x9** block
- ✅ Check console untuk error

### Mannequin tidak spawn?
- ✅ Pastikan player yang didaftar **online**
- ✅ Check permission `whoislying.admin` untuk moderator
- ✅ Restart server jika masih error

### Camera tidak pindah?
- ✅ Check TPS server (`/tps`) - harus >15
- ✅ Pastikan player tidak di gamemode spectator
- ✅ Gunakan `/resetgame` untuk reset

Lihat **TROUBLESHOOTING.md** untuk solusi lengkap.

---

## 📝 CHANGELOG

### v1.0.0 (Initial Release)
- ✅ Adopsi sistem SambungKata (registrasi, arena, mannequin, kamera)
- ✅ Implementasi lengkap mekanik Who's Lying
- ✅ 10+ kategori dengan 15+ kata per kategori
- ✅ Sistem voting dengan vote check
- ✅ Sound effects & visual feedback
- ✅ Protection system (no PVP, no block break)
- ✅ Full configurable
- ✅ Multi-round support

---

## 📄 LICENSE

MIT License - Feel free to use and modify!

---

## 👨‍💻 CREDITS

**Developer:** ArixOffc  
**GitHub:** [github.com/ArixOffc](https://github.com/ArixOffc)

**Inspirasi:**
- Game "Who's Lying?" di Roblox by @reversed_R (170M+ plays)
- Sistem arena & kamera dari plugin SambungKata

**Adaptasi untuk Minecraft Java Edition dengan mekanik yang disesuaikan.**

---

## 🎉 SELAMAT BERMAIN!

Ajak teman-temanmu dan cari tahu **siapa yang berbohong!** 🕵️‍♂️🤥

**Need help?** Baca dokumentasi atau contact developer.  
**Enjoy!** 🎮✨
