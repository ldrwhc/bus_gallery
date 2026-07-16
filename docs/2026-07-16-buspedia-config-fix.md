# Buspedia 配置同步修复报告

**日期**: 2026-07-16  
**分支**: `codex/upload-and-cleanup`  
**操作人**: u010882320

---

## 问题概述

用户反馈 Desktop 上传工具的"爬取参数"功能抓取 buspedia 配置与 buspedia.top 网站实际配置不一致，且抓取结果与数据库已有数据相同（而非 buspedia 最新数据）。

## 根因分析

### Bug #1: Desktop 异步竞态条件（主因）

**文件**: `busGallery-desktop/ui/MainWindow.cpp:84-91`

Desktop 在 `setupUi()` 中注册了一个 model 字段变更信号：
```cpp
connect(m_modelField, &AutocompleteField::valueChanged, this, [this]() {
    if (m_suppressModelAutoFill) return;  // ← 新增保护
    qint64 modelId = m_modelField->selectedId();
    if (modelId > 0) {
        m_catalog->fetchModelVehicles(modelId);  // 异步 HTTP 请求
    }
});
```

当用户点击"爬取参数"时：
1. `fetchBuspediaDetail()` 从 buspedia API 获取数据
2. `setAcText(m_modelField, "BJ6160C6CCD")` 触发 `valueChanged` 信号
3. `fetchModelVehicles(modelId)` 发起**异步** HTTP 请求
4. buspedia 数据正确填入所有配置字段
5. **异步回调到达** → `modelVehiclesReady` lambda 用数据库旧数据**覆盖** buspedia 数据

**时序图**:
```
爬取参数 → setModel("BJ6160C6CCD") → fetchModelVehicles (async)
         → setEngine("菲亚特...")     ← buspedia 正确值
         → setTrans("ZF EcoLife...")  ← buspedia 正确值
         → ...所有字段已填好...
         → ⏰ modelVehiclesReady 回调到达
         → OVERWRITE engine ← 数据库旧值 "康明斯..."
         → OVERWRITE trans  ← 数据库旧值 "Allison..."
```

### Bug #2: Python Scraper 搜索参数错误（已修正）

**文件**: `busGallery-desktop/buspedia_scraper.py:34`

原代码使用 `?q=` 参数搜索，但 buspedia API 实际使用 `?name=`。
```python
# 修正前
result = fetch_api(f'/search?q={urllib.parse.quote(plate)}')

# 修正后
result = fetch_api(f'/search?name={urllib.parse.quote(plate)}')
```

同时修正了响应解析：API 返回 `{"v": [...]}` 对象格式，而非数组。

---

## 修复内容

### 1. Desktop 竞态条件修复

**文件**: `busGallery-desktop/ui/MainWindow.h:115`, `busGallery-desktop/ui/MainWindow.cpp`

- 新增 `m_suppressModelAutoFill` 标志位
- `fetchFromBuspedia()` 中设置 `m_suppressModelAutoFill = true`
- `fetchBuspediaDetail()` 回调中重置 `m_suppressModelAutoFill = false`
- 所有错误路径均正确重置标志位
- Model 自动填充回调检查标志位，抑制时跳过

### 2. Python Scraper 修复

**文件**: `busGallery-desktop/buspedia_scraper.py:31-49`

- 搜索参数从 `?q=` 改为 `?name=`
- 响应解析适配新的 `{"v": [...]}` 格式

### 3. 全量数据库配置回填

**文件**: `scripts/rescrape_buspedia_config.py` (新增)

- 连接 MySQL 查询全部 190 辆车
- 逐辆搜索 buspedia API（`?name=` 参数，保留车牌空格）
- 获取最新配置并对比差异
- 更新 `vehicle_config` 表的 engine/motor/fuel_type/step_type/transmission_system/suspension/axle
- 更新 `vehicle` 表的 fleet_number/factory_date/launch_date
- 内置燃料类型标准化（buspedia "电" → 内部 "纯电" 等）

---

## 数据库更新统计

| 指标 | 数值 |
|------|------|
| 数据库车辆总数 | 190 |
| buspedia 搜索命中 | **190 (100%)** |
| 配置有变化 | **43 辆** |
| 配置无变化 | 147 辆 |
| API 错误 | 0 |

### 重点修复案例

| 车牌 | 自编号 | 变更内容 |
|------|--------|----------|
| 京A R1809 | 24441 | 发动机: 康明斯→菲亚特, 变速箱: Allison→ZF |
| 京A R1855 | 24443 | 发动机: 康明斯→菲亚特, 变速箱: Allison→ZF |
| 京A X2327 | 26025 | 发动机: 康明斯→菲亚特, 变速箱: Allison→ZF |
| 京A W6923 | 24469 | 发动机: 康明斯→玉柴, 变速箱: Allison T325R→T350R |
| 京A 05697F | 40798 | 燃料: 柴油+电→压缩天然气+电 |
| 京A 23457D | 222129 | 悬挂: 钢板→气囊 |
| 京A 27215D | 241019 | 踏步: 一踏→二踏, 悬挂: 气囊→钢板 |

### 批量修正类型

- **变速箱格式**: 全角括号→半角括号（`纯电动（直驱）` → `纯电动 (直驱)`），约 10 辆
- **踏步描述**: wheelchair 符号空格统一（`♿` → ` ♿`），约 15 辆
- **车桥品牌**: 德纳→ZF、创捷→方盛 等，约 6 辆
- **踏步类型错误**: 一踏/二踏/三踏修正，约 5 辆

---

## Git 备份记录

| 时机 | 位置 | Commit |
|------|------|--------|
| 修复前 | 本地 | `b92be0c` backup: before desktop race-condition fix and db re-scrape |
| 修复前 | 服务器 | `8f372ef` backup: before config re-scrape from buspedia |
| 修复后 | 服务器 | `f916392` backup: after config re-scrape - 43 vehicles updated |
| 修复后 | 本地 | `c3b9410` backup: after desktop fix and re-scrape |

---

## 涉及文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `busGallery-desktop/ui/MainWindow.h` | 修改 | 新增 `m_suppressModelAutoFill` 字段 |
| `busGallery-desktop/ui/MainWindow.cpp` | 修改 | 竞态条件修复 + 搜索参数保留 `?name=` |
| `busGallery-desktop/buspedia_scraper.py` | 修改 | 搜索参数 `?q=` → `?name=`，响应格式适配 |
| `scripts/rescrape_buspedia_config.py` | 新增 | 全量配置回填脚本 |
| `memory/buspedia-scraping-api.md` | 修改 | API 参数文档修正 |

---

## 后续建议

1. **Desktop 重新编译**: 修复后的 `MainWindow.cpp` 需要重新编译 Qt 桌面应用
2. **定期同步**: 可设置 cron 定时执行 `rescrape_buspedia_config.py` 保持配置同步
3. **添加 buspedia slug 字段**: 在 `vehicle` 表增加 `buspedia_slug` 列，避免重复搜索
