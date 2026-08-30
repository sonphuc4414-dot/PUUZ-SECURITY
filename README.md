<div align="center">

<img src="src/main/resources/assets/puuz_map_shield/icon.png" width="180" alt="PUUZ SECURITY logo">

# 🛡️ PUUZ SECURITY

### PUUZ Map Shield

**Control your Map Art. Protect your privacy. Own your client experience.**

A client-side Minecraft Fabric utility focused on Map Art control, login privacy, customization, and long-term stability.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-DBD0B8?style=for-the-badge)](https://fabricmc.net/)
[![Version](https://img.shields.io/badge/Version-1.2.0-8B5CF6?style=for-the-badge)](https://github.com/sonphuc4414-dot/PUUZ-SECURITY/releases)
[![Side](https://img.shields.io/badge/Side-Client--Side-111827?style=for-the-badge)](https://fabricmc.net/)

</div>

---

## 📖 Giới thiệu

**PUUZ SECURITY** là tên sản phẩm hiện tại của dự án trước đây mang tên **PUUZ Map Shield**.

Phiên bản `1.2.0` mở rộng project từ một mod ẩn Map Art thành một bộ công cụ client-side tập trung vào **quyền kiểm soát hiển thị, privacy khi đăng nhập, cấu hình cá nhân và khả năng mở rộng**.

Mọi tính năng đều hướng tới việc hoạt động ở phía client và hạn chế ảnh hưởng đến server, gameplay và hiệu năng.

> **1.2.0 là một Big Update, nhưng đây vẫn chưa phải điểm cuối của PUUZ SECURITY.**

---

# ✨ Tính năng

## 🗺️ Map Shield

Ẩn Map Art trên client khi **Hide Map Art** được bật.

Các Map Art được người dùng cho phép vẫn có thể hiển thị thông qua cơ chế allow-list của từng server.

### Điều khiển mặc định

| Phím | Chức năng |
|---|---|
| `F8` | Bật/tắt Map Shield |
| `P` | Cho phép Map Art đang ngắm |
| `U` | Chặn lại Map Art đang ngắm |
| `K` | Mở PUUZ SECURITY Settings |

> Tất cả keybind đều là keybind chuẩn của Minecraft/Fabric và có thể đổi tại **Options → Controls**.

---

## 🔐 Login Password Shield

Khi nhập các lệnh đăng nhập/đăng ký đã được cấu hình, mật khẩu sẽ được **che bằng `*` trong ô chat**.

Mặc định hỗ trợ:

```text
/l
/login
/reg
/register
/dn
/dk
```

Ví dụ:

```text
/login matkhau123
```

sẽ hiển thị:

```text
/login ***********
```

### Privacy model

- Chỉ thay đổi **phần hiển thị của ô chat**.
- Nội dung thật vẫn được giữ để gửi lệnh tới server.
- Không ghi mật khẩu vào log của mod.
- Không lưu password vào file config.
- Phù hợp cho livestream, quay video hoặc chia sẻ màn hình.

---

## ➕ Custom Password Commands

Không bị giới hạn ở các command mặc định.

Người dùng có thể thêm command tùy ý ngay trong GUI, ví dụ:

```text
/password
/auth
/verify
/unlock
```

Command được chuẩn hóa theo dạng `/command` và không phân biệt chữ hoa/chữ thường.

Không cần sửa source code để thêm command mới.

---

# ⚙️ PUUZ SECURITY Settings

`1.2.0` có một GUI cấu hình tập trung cho các tính năng của mod.

### Có thể chỉnh

- 🗺️ Hide Map Art
- 🔐 Login Password Shield
- 🔄 Update Checker
- ➕ Thêm custom password command
- ➖ Xóa command
- ♻️ Khôi phục command mặc định
- 🎮 Mở Minecraft Controls để chỉnh keybind
- 🔎 Kiểm tra update thủ công

GUI sử dụng widget vanilla của Minecraft để giảm dependency và giảm nguy cơ xung đột.

---

# 🧩 Mod Menu

Khi **Mod Menu** được cài đặt, PUUZ SECURITY sẽ xuất hiện với nút **Settings**.

Bấm Settings sẽ mở **cùng một PUUZ SECURITY Settings GUI** dùng trong game.

Mod Menu là dependency tùy chọn.

Không có Mod Menu, mod vẫn hoạt động bình thường.

---

# 🔄 Update Checker

PUUZ SECURITY kiểm tra release mới từ GitHub:

```text
https://api.github.com/repos/sonphuc4414-dot/PUUZ-SECURITY/releases/latest
```

Repository:

```text
https://github.com/sonphuc4414-dot/PUUZ-SECURITY
```

### Ví dụ

```text
Đang chạy:       1.1.0
GitHub latest:   1.2.0
                 ↓
        🔔 Có bản cập nhật mới
```

Checker hỗ trợ:

- `1.0.0 → 1.2.0`
- `1.1.0 → 1.2.0`
- `1.2.0 → 1.3.0`
- Không báo nếu bản hiện tại bằng hoặc mới hơn.

### An toàn

- HTTP chạy ở background.
- Không block render thread.
- Không block client thread bằng network I/O.
- Timeout cho connection/request.
- Cache thời gian kiểm tra.
- Không tạo request storm.
- Không cần GitHub PAT/API key.
- GitHub/network lỗi không làm crash mod.

---

# 🚀 v1.2.0 — Big Update

## Đổi tên sản phẩm

Tên hiển thị của mod hiện tại là:

```text
PUUZ SECURITY
```

Internal Mod ID vẫn giữ:

```text
puuz_map_shield
```

để hạn chế phá compatibility/config cũ.

## Privacy-first controls

`1.2.0` bổ sung hệ thống privacy cho command login/register và cho phép người dùng tự quyết định command nào cần che.

## GUI-first configuration

Các tính năng quan trọng được gom vào một settings screen thay vì bắt người dùng sửa file thủ công.

## Developer handoff

Project có `AI_DEVELOPMENT_NOTES.md` để developer hoặc AI khác có thể đọc nhanh kiến trúc, version history, các nguyên tắc compatibility và hướng phát triển.

---

# 📦 Yêu cầu

| Thành phần | Yêu cầu |
|---|---|
| Minecraft | `1.21.11` |
| Fabric Loader | `0.18.1+` |
| Fabric API | `0.141.6+1.21.11` |
| Java | `21+` |
| Mod Menu | `17.0.0+` — tùy chọn |

---

# 📥 Cài đặt

1. Cài Minecraft `1.21.11`.
2. Cài Fabric Loader tương thích.
3. Cài Fabric API tương thích.
4. Tải `.jar` mới nhất từ **GitHub Releases**.
5. Đưa `.jar` vào:

```text
.minecraft/mods/
```

6. Khởi động Minecraft bằng Fabric.

Mod Menu không bắt buộc.

---

# 🎨 Avatar / Branding

Logo chính thức của PUUZ SECURITY được lưu tại:

```text
src/main/resources/assets/puuz_map_shield/icon.png
```

Logo này được dùng cho:

- Mod icon
- GitHub README
- Branding của project

---

# ⚡ Performance & Stability

PUUZ SECURITY được phát triển theo nguyên tắc:

- Không block render thread.
- Không block client thread bằng network request.
- Hạn chế allocation không cần thiết.
- Hạn chế workload khi tính năng bị tắt.
- Không tạo request storm.
- Fail-safe khi network/API không khả dụng.
- Không để state của server này ảnh hưởng server khác.
- Không log password hoặc credential.
- Ưu tiên tương thích với hệ sinh thái Fabric.

---

# 🧩 Compatibility

Dự án hướng tới khả năng hoạt động cùng các mod Fabric phổ biến.

Đặc biệt cần kiểm thử cùng:

- Fabric API
- Sodium
- Lithium
- FerriteCore
- Entity Culling
- ImmediatelyFast
- Mod Menu

Không có guarantee tuyệt đối cho mọi tổ hợp mod/phiên bản.

---

# 🛠️ Development

## Clone

```bash
git clone https://github.com/sonphuc4414-dot/PUUZ-SECURITY.git
cd PUUZ-SECURITY
```

## Build

Linux / Termux:

```bash
chmod +x gradlew
./gradlew clean build
```

Hoặc:

```bash
gradle clean build
```

JAR được tạo trong:

```text
build/libs/
```

---

# 📁 Project Structure

```text
PUUZ-SECURITY/
│
├── src/main/java/com/puuz/mapshield/
│   ├── PuuzMapShieldClient.java
│   ├── access/
│   │   └── MapRenderStateAccess.java
│   ├── config/
│   │   └── MapShieldConfig.java
│   ├── gui/
│   │   └── PuuzSecuritySettingsScreen.java
│   ├── integration/
│   │   └── ModMenuIntegration.java
│   ├── keybind/
│   │   └── MapShieldKeybind.java
│   ├── mixin/
│   │   ├── ChatScreenMixin.java
│   │   ├── MapRenderStateMixin.java
│   │   └── MapRendererMixin.java
│   └── update/
│       ├── UpdateChecker.java
│       └── UpdateInfo.java
│
├── src/main/resources/
│   ├── assets/puuz_map_shield/
│   ├── fabric.mod.json
│   └── puuz_map_shield.mixins.json
│
├── AI_DEVELOPMENT_NOTES.md
├── build.gradle
├── gradle.properties
├── LICENSE
└── README.md
```

---

# 🧪 Testing Checklist

Trước khi release:

### Map Shield

- [ ] Hide Map Art bật/tắt đúng
- [ ] P allow-map hoạt động
- [ ] U block-map hoạt động
- [ ] Server A không ảnh hưởng server B
- [ ] Disconnect/reconnect không crash

### Password Shield

- [ ] `/l`
- [ ] `/login`
- [ ] `/reg`
- [ ] `/register`
- [ ] `/dn`
- [ ] `/dk`
- [ ] Command custom
- [ ] Toggle on/off
- [ ] Nội dung thật vẫn gửi đúng
- [ ] Password không xuất hiện trong log

### GUI

- [ ] Mở bằng K
- [ ] Mở từ Mod Menu
- [ ] Toggle hoạt động
- [ ] Thêm command
- [ ] Xóa command
- [ ] Reset command
- [ ] Mở Controls
- [ ] Kiểm tra update

### Update Checker

- [ ] Latest = current → không báo
- [ ] Latest > current → báo
- [ ] GitHub offline → không crash
- [ ] Timeout → không crash
- [ ] Không request storm

---

# 🐛 Bug Report

Khi báo lỗi, cung cấp:

```text
Minecraft:
Fabric Loader:
Fabric API:
PUUZ SECURITY:
Mod Menu:

Mô tả lỗi:

Các bước tái hiện:
1.
2.
3.

Danh sách mod:

Crash report / latest.log:
```

**Không đăng password, PAT, token, private key hoặc credential.**

---

# 💡 Feature Request

PUUZ SECURITY được thiết kế để tiếp tục phát triển.

Bạn có thể đề xuất:

- Map Art features
- Privacy features
- GUI improvements
- Performance improvements
- Keybind improvements
- Server-specific profiles
- Automation improvements

Roadmap có thể thay đổi theo quá trình phát triển.

---

# 🔮 Roadmap

```text
v1.0.0  ── ✅ Initial release
   │
   ▼
v1.1.0  ── ✅ Selective Map Art + improvements
   │
   ▼
v1.2.0  ── 🔥 Big Update
   │
   ├── PUUZ SECURITY branding
   ├── Login Password Shield
   ├── Custom password commands
   ├── Advanced Settings GUI
   ├── Mod Menu integration
   └── Update Checker improvements
   │
   ▼
Future   ── 🚀 More privacy, controls & performance
```

---

# 📜 Versioning

Dự án sử dụng version dạng:

```text
MAJOR.MINOR.PATCH
```

Ví dụ:

```text
1.0.0
1.1.0
1.2.0
1.2.1
```

- **MAJOR** — thay đổi lớn/breaking changes.
- **MINOR** — feature update.
- **PATCH** — bug/stability fixes.

---

# 🤝 Contributing

Pull Request và đóng góp được hoan nghênh.

Ưu tiên:

- Code rõ ràng.
- Không block render/client thread.
- Không tạo request storm.
- Không log sensitive data.
- Không phá compatibility.
- Test trước khi release.

---

# ⭐ Support PUUZ SECURITY

Nếu project hữu ích:

⭐ Star repository  
🐛 Báo bug  
💡 Đề xuất feature  
🧪 Giúp testing  
📢 Chia sẻ project

---

<div align="center">

# 🛡️ PUUZ SECURITY

**Hide it. Control it. Protect it.**

### v1.2.0 — Big Update

**And this is only the beginning. 🚀**

</div>

---

## 💰 Money History — v1.2.5

Optional client-side HUD for local payment history.

- Records `/pay` transactions.
- Records supported incoming payment confirmations.
- Optional balance display.
- Position, size and scale are configurable.
- Sent/received visibility can be toggled independently.
- Local history is unlimited and persists until the user clears it.
- No transaction history is uploaded anywhere.

## 💰 Money History — v1.2.5

An optional, local-only HUD for tracking `/pay` activity.

- Enable/disable the widget.
- Enable/disable Balance independently.
- Toggle sent and received payments independently.
- Choose HUD position presets.
- Choose compact/balanced/large sizing.
- Keep an unlimited local history until manually cleared.
- No payment history is sent to any remote service.

Data is stored under `config/puuz-security/` on the client.
