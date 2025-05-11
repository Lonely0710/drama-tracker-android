<div align="center">
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111738271.png" width="200" alt="应用图标">
</div>

<p align="center">
  <img alt="license" src="https://img.shields.io/github/license/Lonely0710/drama-tracker-Android" />
  <img alt="stars" src="https://img.shields.io/github/stars/Lonely0710/drama-tracker-Android" />
  <img alt="forks" src="https://img.shields.io/github/forks/Lonely0710/drama-tracker-Android" />
  <img alt="issues" src="https://img.shields.io/github/issues/Lonely0710/drama-tracker-Android" />
  <!-- <img alt="release" src="https://img.shields.io/github/v/release/Lonely0710/drama-tracker-Android" /> -->
  <!-- <img alt="downloads" src="https://img.shields.io/github/downloads/Lonely0710/drama-tracker-Android/total" /> -->
</p>

# 影迹 - DramaTracker

> 一站式影视追踪与发现平台，记录你的观影历程，发现高分好剧

## 🌟 功能全景

### 📱 核心功能
| 模块     | 特性                          | 技术亮点                 |
| -------- | ----------------------------- | ------------------------ |
| **搜索** | 豆瓣/TMDb/Bangumi多源聚合搜索 | Jsoup爬虫 + OkHttp       |
| **追踪** | 追剧进度/评分/点评/自定义标签 | Appwrite云SDK + 云数据库 |
| **推荐** | 每周放送/近期热门/高分榜单    | Crawler爬虫 + 自定义分析 |
| **发现** | 个性化内容推荐/热度榜单       | 自定义算法 + ViewPager2  |

## 🖼 界面展示

### 🔐 身份验证流程
<div align="center">
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111738944.png" width="30%" alt="品牌启动页">
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111738979.png" width="30%" alt="邮箱登录界面"> 
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111738985.png" width="30%" alt="用户注册界面">
  
  应用启动与身份验证流程
</div>

---

### 🏠 主页与搜索
<div align="center">
  <div>
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111738481.png" width="30%" alt="主页界面">
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111738510.png" width="30%" alt="豆瓣榜单">
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111738544.png" width="30%" alt="TMDb榜单">
  </div>
  
  <div style="margin-top:20px">
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111738677.png" width="30%" alt="Bangumi榜单">
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739801.png" width="30%" alt="搜索界面">
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739850.png" width="30%" alt="搜索结果">
  </div>
  
  主页浏览与搜索功能
</div>

---

### 📺 影音库
<div align="center">
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739532.png" width="30%" alt="影音库总览">
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739467.png" width="30%" alt="观看记录列表">
  
  个人影音库与观看记录
</div>

---

### ➕ 添加记录
<div align="center">
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739999.png" width="30%" alt="添加记录">
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739012.png" width="30%" alt="搜索添加结果">
  
  添加观看记录流程
</div>

---

### 🔍 推荐发现
<div align="center">
  <div>
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739213.png" width="30%" alt="近期热门">
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739265.png" width="30%" alt="每周放送">
  </div>
  
  <div style="margin-top:20px">
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739362.png" width="30%" alt="高分榜单">
    <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739455.png" width="30%" alt="电视剧榜单">
  </div>
  
  推荐与发现页面
</div>

---

### ⚙️ 设置管理
<div align="center">
  <img src="https://lonelynotes-images.oss-cn-beijing.aliyuncs.com/202505111739647.png" width="30%" alt="设置界面">
  
  应用设置与用户管理
</div>

## 🏗️ 技术架构

### 系统设计
```text
架构模式: 单Activity多Fragment
开发语言: Java + Kotlin混合
核心组件:
  ├── 数据层: Appwrite云数据库 + 爬虫数据源
  ├── 展示层: ViewBinding + RecyclerView
  ├── 业务层: 模块化Fragment设计
  └── 工具层: Glide图片处理 + OkHttp + Jsoup爬虫
```

## ⚙️ 配置指南
### 环境要求
- Android Studio Flamingo+

- Java 8+

- Appwrite服务实例

### 云服务配置
1. 复制配置文件模板：
   ```bash
   cp app/src/main/assets/secrets.properties.template app/src/main/assets/secrets.properties
   ```
2. 配置API密钥和Appwrite参数：
   ```properties
   # AppWrite项目配置
   APPWRITE_PROJECT_ID=your_project_id_here
   APPWRITE_DATABASE_ID=your_database_id_here
   
   # AppWrite集合ID配置
   APPWRITE_COLLECTION_USERS_ID=your_users_collection_id_here 
   APPWRITE_COLLECTION_COLLECTIONS_ID=your_collections_collection_id_here
   APPWRITE_COLLECTION_MEDIA_ID=your_media_collection_id_here
   
   # TMDB API配置
   TMDB_API_TOKEN=your_api_token_here
   TMDB_API_KEY=your_api_key_here
   ```
3. 初始化云资源：
   ```bash
   # 创建存储桶
   appwrite storage createBucket --name drama-posters --permission read
   ```

## 📦 安装与使用

### APK安装
```bash
adb install app/release/dramatracker-v1.0.0.apk
```

### 开发构建
```bash
# 调试版本
./gradlew assembleDebug

# 发布版本
./gradlew assembleRelease
```

## 🌱 贡献指引
欢迎通过以下方式参与项目：
- 在Issues报告问题或建议
- 提交Pull Request时请：
  - 遵循现有代码风格
  - 更新相关文档
  - 添加必要的单元测试

## 📜 许可协议
本项目基于 [MIT License](LICENSE) 开源，允许自由使用和修改，但需保留原始版权声明。

---

<details>
<summary>📮 联系维护者</summary>

**核心开发者**：Lonely团队  
**电子邮箱**：lingsou43@gmail.com  
**技术栈咨询**：欢迎提交Issue讨论  
**路线图**：  
- [x] 基础功能实现 (2025 Q2)  
- [ ] 观影数据分析 (2025 Q4)  
- [ ] 社交分享功能 (2026 Q1)  
</details> 