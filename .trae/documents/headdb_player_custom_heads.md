# NBT-Editor HeadDB：新增玩家头查询 + 本地自定义头颅库

## Context

新版 HeadDB（HeadsDB-master）能获取的头比 NBT-Editor 移植的旧版多两块：**任意玩家名查询头颅**（Mojang API）与**自定义头颅库**（服务端本地存储）。用户希望 mod 也能覆盖这两块。官方 10 万+ 头颅目录两边同源（minecraft-heads.com / headsdb.com），无需改动；本次只补足增量能力，并把旧版收藏逻辑中 category 为 null 时可能 NPE 的隐患一并修掉。

纯客户端功能，不涉及服务端权限。不移植新版分片协议 / 服务端自定义头管理 / 经济系统。

## 改动清单

### 1. `src/main/java/tsp/headdb/ported/HeadAPI.java`（主要门面）

新增 import：`ByteArray...`（实际 `java.nio.charset.StandardCharsets`）、`java.util.Base64`、`java.util.function.Consumer`、`com.google.gson.JsonArray/JsonElement/JsonObject/JsonParser`。

**工具方法：**
- `public static String parseHeadName(String value)`：base64 解码纹理 value 的 profile JSON，取 `name`（回退 `profileName`），失败返回 null。
- `public static Head buildCustomHead(String value)`：先 `getHeadByValue(value)` 命中官方库则直接用；否则构造 `new Head(-1).withName(parseHeadName(value) ?? "Custom Head").withUniqueId(UUID.nameUUIDFromBytes(value.getBytes(UTF_8))).withValue(value)`（category 保持 null）。
- `public static String getCategoryName(Head head)`：`category != null ? category.getName() : "custom"`，供界面层安全取分类名。

**玩家头查询（异步，daemon 线程 + 回主线程）：**
- `public static void getPlayerHeadAsync(String name, Consumer<Head> success, Consumer<String> error)`：
  - 线程内 `getDatabase().fetch("https://api.mojang.com/users/profiles/minecraft/" + name)`（复用 protected `HeadDatabase.fetch`，同包可直接调，UA 已有）→ 解析 `id`（32 位无连字符 UUID，补连字符）与 `name`；无 `id` 字段 → 回主线程 `error.accept("not_found")`。
  - `fetch("https://sessionserver.mojang.com/session/minecraft/profile/" + dashedUuid)` → 找 `properties` 中 `name=="textures"` 的 `value` → 构造 `new Head(-1).withName(...).withUniqueId(...).withValue(...)`。
  - 成功/失败回调均用 `MainUtil.client.execute(...)` 切回主线程；`IOException|JsonParseException` → `error.accept("error")`（离线/盗版环境友好提示）。

**自定义头颅存储（镜像 favorites 结构，文件 `SETTINGS_FOLDER/headdb_custom.txt`，格式 `v1\n[...]` JSON 数组存 value）：**
- `public static void addCustomHead(String value)` / `removeCustomHead(String value)`：内存 list + `saveCustomHeads()`。
- `public static void loadCustomHeads() throws IOException`：非 `v1\n` 开头忽略。
- `private static void saveCustomHeads()`。
- `public static List<Head> getCustomHeads()`：逐个 `buildCustomHead(value)`。

**顺带修复：** `getFavoriteHeads()` 中 `getHeadByValue(favorite)` 可能返回 null（收藏了自定义头）→ 改为 null 时回退 `buildCustomHead(favorite)`，避免 null 进列表后续 NPE。

### 2. `src/main/java/tsp/headdb/ported/inventory/InventoryUtils.java`

- 新增 `public static void openCustomMenu()`：`PagedPane(4,6,"&c&lHeadDB &8- &6Custom Heads")`，每头手写 `Button`（仿 `openLocalMenu`，**不要用 genButton**）：左键购买 1 / 左 shift 64 / 右键删除（`HeadAPI.removeCustomHead(value)` + `openCustomMenu()` 重开 + 消息）。
- `openDatabase()`：
  - 槽 42 增加 custom 按钮（`getUILocation("custom", 42)`，图标 `new ItemStack(Items.FILLED_MAP)`，名字 `&6Custom`，lore 提示保存与浏览）。
  - `slotClicked` 中 `name.equalsIgnoreCase("custom")` 分支 → `openCustomMenu()`（加在 favorites/local 分支旁、`Category.getByName` 之前）。
- **NPE 修复：** `genButton`（L127）与 `openFavoritesMenu`（L77）中 `head.getCategory().getName()` 改为 `HeadAPI.getCategoryName(head)`。

### 3. `src/main/java/com/luneruniverse/minecraft/mod/nbteditor/commands/get/GetHdbCommand.java`

- 新增 `player` 子命令：`/get hdb player <name>`，`StringArgumentType.word()` 取参；先用 `net.minecraft.util.StringUtil.isValidPlayerName` 校验（GetSkullCommand 已有同款用法），不合法 → `nbteditor.hdb.player.invalid_name`；合法 → 发 `nbteditor.hdb.player.searching` 反馈，调 `HeadAPI.getPlayerHeadAsync(name, head -> InventoryUtils.purchaseHead(head, 1, "player", head.getName()), error -> 发 nbteditor.hdb.player.<error>)`。
- 新增 `save` 子命令：`/get hdb save [name]`（可选 name 参数），`saveHeldHead(FabricClientCommandSource, String)`：
  - `ItemTagReferences.PROFILE.get(MainUtil.client.player.getMainHandItem())` → `Optional<GameProfile>`；`profile.properties().get("textures")` 取 `Property.value()`（UnbindSkullCommand 已有 `properties()` 用法）。
  - 无纹理 → 反馈 `nbteditor.hdb.custom.not_player_head`；有 → `HeadAPI.addCustomHead(value)` + 反馈 `nbteditor.hdb.custom.saved`（名字用自定义名或 `parseHeadName`）。
- 新增 import：`java.util.Optional`、`com.mojang.authlib.GameProfile`、`com.mojang.authlib.properties.Property`、`net.minecraft.util.StringUtil`。

### 4. `src/main/java/com/luneruniverse/minecraft/mod/nbteditor/NBTEditorClient.java`

`onInitializeClient` 中 `HeadAPI.loadFavorites()` 旁加 `try { HeadAPI.loadCustomHeads(); } catch (IOException e) { NBTEditor.LOGGER.error("Error while loading HeadDB custom heads", e); }`。

### 5. 语言文件 `src/main/resources/assets/nbteditor/lang/zh_cn.json` + `en_us.json`

`nbteditor.hdb.*` 键体系下新增：
- `custom`：自定义头颅 / Custom Heads（按钮名，无需 § 码）
- `custom.saved`：已保存到自定义库
- `custom.not_player_head`：主手需手持带纹理的玩家头
- `player.searching` / `player.not_found` / `player.no_textures` / `player.error` / `player.invalid_name`

（中文沿用现有键的 § 着色风格，与 `nbteditor.hdb.search` 等一致。）

## 风险与规避

| 风险 | 规避 |
|---|---|
| 离线/盗版模式 Mojang API 不可用（204/空/异常） | `profile.has("id")` 守卫 + 异常 catch → 友好错误键，全部异步不卡主线程 |
| `genButton`/收藏界面 `getCategory()` 对无 category 头 NPE | 统一走 `HeadAPI.getCategoryName`，purchaseHead 本就忽略 category 字符串 |
| `Head.getItemStack` 的 `Validate`（name/uuid/value 非 null） | `buildCustomHead` 与玩家路径都显式设置三项，仅 category 为 null（`Head` L41 已处理 null 分支）|
| `save` 手持非玩家头/无纹理 | `PROFILE.get` 为空或 properties 无 textures → 友好报错 |
| 文本解密损坏的 value | `parseHeadName` 内 try/catch，回退 "Custom Head" |
| `buildCustomHead` 内部 `getHeadByValue` 在库过期时触发一次网络 update | 自定义菜单入口均经 `openDatabase`（已有 checkUpdated 门控），可接受 |

## 验证

1. `.\gradlew.bat compileJava`（Windows PowerShell，工作目录 `d:\NBT-Editor26.1.2-26.1.x-1`）通过。
2. 游戏内在线模式：
   - `/get hdb player notch` → 查询反馈后获得 Notch 头颅。
   - `/get hdb player Notch` → 大小写不敏感正常工作。
   - `/get hdb player 不存在的玩家xyz` → friendly not_found。
   - 手持含纹理玩家头执行 `/get hdb save` → 保存成功；检查 `nbteditor/headdb_custom.txt` 为 `v1\n["...base64..."]`。
   - `/get hdb` 主菜单出现 Custom 按钮（槽 42）→ 打开自定义菜单：左键获取、右键删除并刷新。
   - 把自定义头加入收藏后再开收藏菜单 → 无 NPE（回归收藏修复）。
   - 离线模式 `/get hdb player x` → 友好离线错误，无崩溃/卡死。