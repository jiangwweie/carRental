# 图片上传功能 - 详细设计文档

> **版本**: v1.0
> **日期**: 2026-04-18
> **作者**: 架构师

---

## 1. 问题分析

### 1.1 当前问题

| 问题 | 影响 | 严重程度 |
|------|------|----------|
| 图片以 base64 存入 MySQL JSON 列 | 数据库膨胀，单条记录可达数 MB | 高 |
| 管理端用 textarea 手动粘贴 base64 | 用户体验极差，易出错 | 高 |
| 后端无文件上传端点 | 无法实现正常的图片上传功能 | 高 |
| 无静态文件服务配置 | 上传后无法访问图片 | 高 |

### 1.2 影响范围

- **数据库**: `vehicles.images` 字段存储 base64，每条记录 ~100KB-1MB
- **前端**: `VehicleView.vue` 使用 `<el-input type="textarea">` 手动输入
- **API**: `AdminVehicleController` 接收 `List<String> images` 无文件上传支持

### 1.3 设计目标

1. **解决数据库膨胀**: 图片存储在文件系统，数据库只存 URL
2. **改善用户体验**: 提供拖拽上传、预览、删除功能
3. **支持开发环境**: Spring Boot 静态资源映射 + Vite proxy
4. **预留扩展点**: 接口抽象，未来可迁移到 OSS

---

## 2. 系统架构

### 2.1 架构图（文字描述）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              前端层 (Frontend)                               │
├─────────────────────────────────────────────────────────────────────────────┤
│  frontend-admin (Vue 3 + Element Plus)                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  VehicleView.vue                                                      │    │
│  │  ┌─────────────────────────────────────────────────────────────┐    │    │
│  │  │  ImageUploader 组件                                          │    │    │
│  │  │  - el-upload (拖拽上传)                                       │    │    │
│  │  │  - 图片预览 + 删除                                            │    │    │
│  │  │  - 上传进度显示                                               │    │    │
│  │  └─────────────────────────────────────────────────────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ HTTP POST /api/v1/admin/images/upload
                                      │ HTTP GET  /uploads/vehicles/xxx.jpg
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              后端层 (Backend)                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  Spring Boot 3.2.5 (Port 8081)                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Controllers                                                         │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │  AdminImageController                                           │  │    │
│  │  │  - POST /api/v1/admin/images/upload    → 上传图片               │  │    │
│  │  │  - DELETE /api/v1/admin/images/{id}   → 删除图片 (可选)         │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Services                                                            │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │  ImageStorageService (接口)                                     │  │    │
│  │  │  └── LocalImageStorageService (实现 - 本地文件系统)             │  │    │
│  │  │      └── OssImageStorageService (未来实现 - 阿里云 OSS)         │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Config                                                              │    │
│  │  ┌───────────────────────────────────────────────────────────────┐  │    │
│  │  │  WebMvcConfig                                                   │  │    │
│  │  │  - addResourceHandlers(): /uploads/** → file:backend/uploads/   │  │    │
│  │  └───────────────────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ 文件 I/O
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           存储层 (Storage)                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  本地文件系统                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  backend/uploads/                                                    │    │
│  │  ├── vehicles/                      # 车辆图片目录                   │    │
│  │  │   ├── 2026/                      # 按年份分目录                   │    │
│  │  │   │   ├── 04/                    # 按月份分目录                   │    │
│  │  │   │   │   ├── v1_1713456789012_abc123.jpg  # 命名: v{vehicleId}_{timestamp}_{random}.ext │    │
│  │  │   │   │   └── ...                                             │    │
│  │  │   └── ...                                                       │    │
│  │  └── temp/                          # 临时上传目录(可选)             │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流

```
上传流程:
┌──────────┐    1. 选择文件     ┌──────────────────┐
│  用户    │ ─────────────────> │  ImageUploader   │
└──────────┘                    │  (前端组件)       │
                                └────────┬─────────┘
                                          │
                                2. POST /api/v1/admin/images/upload
                                (multipart/form-data, vehicleId 可选)
                                          │
                                          ▼
                                ┌──────────────────┐
                                │ AdminImageController │
                                │   (后端控制器)      │
                                └────────┬─────────┘
                                          │
                                3. 校验 + 存储
                                          │
                                          ▼
                                ┌──────────────────┐
                                │ LocalImageStorageService │
                                │   (存储服务)            │
                                └────────┬─────────┘
                                          │
                                4. 生成 URL: /uploads/vehicles/2026/04/xxx.jpg
                                          │
                                          ▼
                                ┌──────────────────┐
                                │ 返回 { url, filename } │
                                └────────┬─────────┘
                                          │
                                5. 前端更新 images 数组
                                          │
                                          ▼
                                ┌──────────────────┐
                                │ POST/PUT /api/v1/admin/vehicles │
                                │ (images: ["/uploads/..."])      │
                                └──────────────────┘
```

---

## 3. 后端 API 设计

### 3.1 图片上传接口

```
POST /api/v1/admin/images/upload
```

**认证**: 需要（管理员权限 `admin`）

**请求格式**: `multipart/form-data`

| 字段 | 类型 | 必填 | 验证 | 说明 |
|------|------|------|------|------|
| `file` | File | 是 | 见文件验证规则 | 图片文件 |
| `vehicle_id` | Long | 否 | 正整数 | 关联车辆 ID（用于目录组织，可选） |

**文件验证规则**:

| 规则 | 值 | 说明 |
|------|-----|------|
| 最大文件大小 | 5 MB | 超过拒绝 |
| 允许格式 | jpg, jpeg, png, webp | 通过扩展名 + MIME 类型双重校验 |
| 图片最小尺寸 | 100 x 100 px | 防止上传过小图片 |
| 图片最大尺寸 | 4096 x 4096 px | 防止上传超大图片 |

**成功响应** (HTTP 200):

```json
{
  "code": 0,
  "data": {
    "url": "/uploads/vehicles/2026/04/v1_1713456789012_abc123.jpg",
    "filename": "v1_1713456789012_abc123.jpg",
    "size": 245678,
    "content_type": "image/jpeg"
  },
  "message": "success"
}
```

**错误响应**:

| 错误码 | HTTP 状态 | 说明 |
|--------|-----------|------|
| 4000 | 400 | 参数错误（缺少 file 字段） |
| 4005 | 400 | 文件类型不支持 |
| 4006 | 400 | 文件大小超过限制 |
| 4007 | 400 | 图片尺寸不符合要求 |
| 5000 | 500 | 服务器存储失败 |

### 3.2 图片删除接口（可选，延后实现）

```
DELETE /api/v1/admin/images
```

**认证**: 需要（管理员权限）

**请求体**:

```json
{
  "url": "/uploads/vehicles/2026/04/xxx.jpg"
}
```

**说明**: 仅在图片未被任何车辆引用时允许删除。v1.0 可暂不实现，通过后台脚本清理孤立文件。

### 3.3 新增错误码

在 `ErrorCode.java` 中新增:

```java
// 文件上传相关
FILE_TYPE_NOT_SUPPORTED(4005, "文件类型不支持"),
FILE_SIZE_EXCEEDED(4006, "文件大小超过限制"),
IMAGE_DIMENSION_INVALID(4007, "图片尺寸不符合要求"),
FILE_UPLOAD_FAILED(5001, "文件上传失败");
```

---

## 4. 文件存储策略

### 4.1 目录结构

```
backend/
├── uploads/                           # 上传根目录 (gitignore)
│   ├── vehicles/                      # 车辆图片
│   │   ├── 2026/                      # 按年份
│   │   │   ├── 01/                    # 按月份
│   │   │   ├── 02/
│   │   │   ├── ...
│   │   │   └── 12/
│   │   └── 2027/
│   └── temp/                          # 临时文件 (可选)
└── ...
```

### 4.2 文件命名规则

```
格式: v{vehicleId}_{timestamp}_{random6}.{ext}

示例:
- v1_1713456789012_abc123.jpg    (关联车辆 ID=1)
- v0_1713456789013_def456.png    (未关联车辆)

组成部分:
- v: 固定前缀，表示车辆图片
- vehicleId: 车辆 ID，未关联时为 0
- timestamp: 毫秒时间戳，保证唯一性
- random6: 6 位随机字符，防止冲突
- ext: 原始文件扩展名（统一小写）
```

### 4.3 存储配置

在 `application.yml` 中新增:

```yaml
upload:
  # 存储类型: local (本地) / oss (阿里云)
  type: local

  # 本地存储配置
  local:
    # 上传根目录（相对于项目根目录或绝对路径）
    base-path: ${UPLOAD_PATH:uploads}
    # URL 前缀
    url-prefix: /uploads

  # 文件限制
  limits:
    # 最大文件大小 (字节)
    max-size: 5242880  # 5MB
    # 允许的文件类型
    allowed-types: jpg,jpeg,png,webp
    # 最小图片尺寸
    min-dimension: 100
    # 最大图片尺寸
    max-dimension: 4096
```

### 4.4 URL 生成规则

```
相对 URL: /uploads/vehicles/2026/04/v1_1713456789012_abc123.jpg

完整 URL (开发环境):
http://localhost:8081/uploads/vehicles/2026/04/v1_1713456789012_abc123.jpg

完整 URL (生产环境 Nginx):
http://api.example.com/uploads/vehicles/2026/04/v1_1713456789012_abc123.jpg
```

---

## 5. 静态资源访问方案

### 5.1 开发环境

**后端配置** (`WebMvcConfig.java`):

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 静态资源映射: /uploads/** -> file:uploads/
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/");
}
```

**前端配置** (`vite.config.js`):

```javascript
server: {
  port: 3001,
  proxy: {
    '/api': {
      target: 'http://localhost:8081',
      changeOrigin: true
    },
    // 新增: 代理静态资源
    '/uploads': {
      target: 'http://localhost:8081',
      changeOrigin: true
    }
  }
}
```

**访问流程**:

```
前端请求: http://localhost:3001/uploads/vehicles/xxx.jpg
    ↓ Vite Proxy
后端响应: http://localhost:8081/uploads/vehicles/xxx.jpg
    ↓ Spring 静态资源映射
文件系统: uploads/vehicles/xxx.jpg
```

### 5.2 生产环境

**Nginx 配置**:

```nginx
server {
    listen 80;
    server_name api.example.com;

    # API 请求转发到 Spring Boot
    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 静态资源直接由 Nginx 提供 (更高性能)
    location /uploads/ {
        alias /path/to/backend/uploads/;
        expires 30d;
        add_header Cache-Control "public, immutable";

        # 防止执行脚本
        location ~* \.(php|jsp|cgi)$ {
            deny all;
        }
    }
}
```

**说明**: 代码层面无需修改，只需部署时配置 Nginx。

---

## 6. 扩展点设计（未来 OSS 迁移）

### 6.1 存储服务接口

```java
package com.carrental.infrastructure.storage;

public interface ImageStorageService {

    /**
     * 存储图片
     * @param file 上传的文件
     * @param vehicleId 关联车辆 ID (可选)
     * @return 存储结果 (包含 URL)
     */
    StorageResult store(MultipartFile file, Long vehicleId) throws StorageException;

    /**
     * 删除图片
     * @param url 图片 URL
     * @return 是否删除成功
     */
    boolean delete(String url) throws StorageException;

    /**
     * 检查图片是否存在
     */
    boolean exists(String url);
}

@Data
@AllArgsConstructor
public class StorageResult {
    private String url;           // 相对 URL
    private String filename;      // 文件名
    private long size;            // 文件大小
    private String contentType;   // MIME 类型
}

public class StorageException extends RuntimeException {
    private final int code;
    // ...
}
```

### 6.2 本地存储实现

```java
@Service
@ConditionalOnProperty(name = "upload.type", havingValue = "local")
public class LocalImageStorageService implements ImageStorageService {

    @Value("${upload.local.base-path}")
    private String basePath;

    @Override
    public StorageResult store(MultipartFile file, Long vehicleId) {
        // 1. 校验文件
        validateFile(file);

        // 2. 生成文件名和路径
        String filename = generateFilename(file, vehicleId);
        String relativePath = generatePath(filename);

        // 3. 创建目录
        Path targetDir = Paths.get(basePath, relativePath);
        Files.createDirectories(targetDir);

        // 4. 保存文件
        Path targetFile = targetDir.resolve(filename);
        file.transferTo(targetFile);

        // 5. 返回结果
        String url = "/uploads/" + relativePath + "/" + filename;
        return new StorageResult(url, filename, file.getSize(), file.getContentType());
    }

    // ... 其他方法
}
```

### 6.3 OSS 存储实现（未来）

```java
@Service
@ConditionalOnProperty(name = "upload.type", havingValue = "oss")
public class OssImageStorageService implements ImageStorageService {

    @Autowired
    private OSS ossClient;  // 阿里云 OSS SDK

    @Value("${upload.oss.bucket}")
    private String bucket;

    @Value("${upload.oss.endpoint}")
    private String endpoint;

    @Override
    public StorageResult store(MultipartFile file, Long vehicleId) {
        // 1. 校验文件
        validateFile(file);

        // 2. 生成 OSS key
        String key = "vehicles/" + generateFilename(file, vehicleId);

        // 3. 上传到 OSS
        ossClient.putObject(bucket, key, file.getInputStream());

        // 4. 返回 CDN URL
        String url = "https://cdn.example.com/" + key;
        return new StorageResult(url, ...);
    }

    // ... 其他方法
}
```

### 6.4 切换配置

只需修改 `application.yml`:

```yaml
upload:
  type: oss  # 从 local 切换到 oss

  oss:
    endpoint: oss-cn-beijing.aliyuncs.com
    bucket: car-rental-images
    access-key-id: ${OSS_ACCESS_KEY}
    access-key-secret: ${OSS_SECRET_KEY}
```

**无需修改任何业务代码。**

---

## 7. 前端组件设计

### 8.1 ImageUploader 组件

**文件**: `frontend-admin/src/components/ImageUploader.vue`

**Props**:

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `modelValue` | `string[]` | `[]` | 图片 URL 数组 (v-model) |
| `maxCount` | `number` | `5` | 最大图片数量 |
| `vehicleId` | `number` | `null` | 关联车辆 ID |
| `disabled` | `boolean` | `false` | 是否禁用 |

**Events**:

| 事件 | 参数 | 说明 |
|------|------|------|
| `update:modelValue` | `string[]` | 更新图片数组 |
| `upload-success` | `{ url, filename }` | 上传成功 |
| `upload-error` | `{ error }` | 上传失败 |

**使用示例**:

```vue
<template>
  <ImageUploader
    v-model="form.images"
    :vehicle-id="editingId"
    :max-count="5"
  />
</template>
```

### 8.2 组件功能

1. **拖拽上传**: 支持拖拽文件到上传区域
2. **点击上传**: 点击按钮选择文件
3. **图片预览**: 上传后显示缩略图
4. **删除图片**: 点击删除按钮移除图片
5. **上传进度**: 显示上传进度条
6. **错误提示**: 文件类型/大小不符合要求时提示

---

## 9. 安全考虑

### 9.1 文件上传安全

| 安全措施 | 实现方式 |
|----------|----------|
| 文件类型校验 | 扩展名白名单 + MIME 类型双重校验 |
| 文件大小限制 | Spring `multipart.max-file-size` + 自定义校验 |
| 文件名消毒 | 使用时间戳 + 随机字符，不使用原始文件名 |
| 路径遍历防护 | 不接受用户输入的路径参数 |
| 图片内容校验 | 读取图片尺寸，验证是否为有效图片 |
| 病毒扫描 | (可选) 集成 ClamAV |

### 9.2 访问控制

- 上传接口需要管理员权限
- 静态资源 `/uploads/**` 不需要认证（图片需要在小程序公开展示）
- 删除接口需要管理员权限 + 所有权校验

---

## 10. 测试策略

### 10.1 单元测试

```java
@SpringBootTest
class ImageStorageServiceTest {

    @Autowired
    private ImageStorageService storageService;

    @Test
    void testStoreImage() throws Exception {
        // 准备测试图片
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg",
            Files.readAllBytes(Paths.get("src/test/resources/test-image.jpg"))
        );

        // 执行存储
        StorageResult result = storageService.store(file, 1L);

        // 验证
        assertNotNull(result.getUrl());
        assertTrue(result.getUrl().startsWith("/uploads/vehicles/"));
        assertTrue(storageService.exists(result.getUrl()));
    }

    @Test
    void testRejectInvalidFileType() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.exe", "application/octet-stream",
            "invalid".getBytes()
        );

        assertThrows(StorageException.class, () -> {
            storageService.store(file, 1L);
        });
    }
}
```

### 10.2 集成测试

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
class AdminImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUploadImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg",
            Files.readAllBytes(Paths.get("src/test/resources/test-image.jpg"))
        );

        mockMvc.perform(multipart("/api/v1/admin/images/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.url").exists());
    }
}
```

---

## 11. 监控与日志

### 11.1 日志记录

```java
@Slf4j
public class LocalImageStorageService implements ImageStorageService {

    @Override
    public StorageResult store(MultipartFile file, Long vehicleId) {
        log.info("[IMAGE_UPLOAD] vehicleId={}, filename={}, size={}, type={}",
            vehicleId, file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            // ... 存储逻辑

            log.info("[IMAGE_UPLOAD_SUCCESS] url={}, duration={}ms",
                result.getUrl(), duration);
            return result;
        } catch (Exception e) {
            log.error("[IMAGE_UPLOAD_FAILED] vehicleId={}, error={}",
                vehicleId, e.getMessage(), e);
            throw new StorageException(ErrorCode.FILE_UPLOAD_FAILED, e);
        }
    }
}
```

### 11.2 监控指标

| 指标 | 说明 |
|------|------|
| `image_upload_total` | 上传总数 |
| `image_upload_success` | 上传成功数 |
| `image_upload_failed` | 上传失败数 |
| `image_upload_size_bytes` | 上传文件大小分布 |
| `image_storage_used_bytes` | 存储空间使用量 |

---

## 12. 部署检查清单

### 12.1 部署前

- [ ] 创建 `backend/uploads/vehicles/` 目录
- [ ] 设置目录权限 (应用可写)
- [ ] 配置 `application.yml` 中的 `upload.local.base-path`

### 12.2 部署后验证

- [ ] 测试图片上传功能
- [ ] 测试图片访问 URL
- [ ] 测试前端图片展示
- [ ] 检查小程序图片展示
- [ ] 监控磁盘空间

---

## 附录 A: 配置文件完整示例

### application.yml (新增部分)

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 5MB
      max-request-size: 10MB

upload:
  type: local
  local:
    base-path: ${UPLOAD_PATH:uploads}
    url-prefix: /uploads
  limits:
    max-size: 5242880
    allowed-types: jpg,jpeg,png,webp
    min-dimension: 100
    max-dimension: 4096
```

### WebMvcConfig.java (新增部分)

```java
@Value("${upload.local.base-path:uploads}")
private String uploadBasePath;

@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadBasePath + "/");
}
```

---

## 附录 B: 文件验证实现

```java
@Component
public class ImageValidator {

    @Value("${upload.limits.max-size}")
    private long maxSize;

    @Value("${upload.limits.allowed-types}")
    private String allowedTypes;

    @Value("${upload.limits.min-dimension}")
    private int minDimension;

    @Value("${upload.limits.max-dimension}")
    private int maxDimension;

    public void validate(MultipartFile file) {
        // 1. 检查文件是否为空
        if (file.isEmpty()) {
            throw new StorageException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }

        // 2. 检查文件大小
        if (file.getSize() > maxSize) {
            throw new StorageException(ErrorCode.FILE_SIZE_EXCEEDED,
                "文件大小超过限制: " + (maxSize / 1024 / 1024) + "MB");
        }

        // 3. 检查文件类型
        String extension = getExtension(file.getOriginalFilename());
        if (!allowedTypes.contains(extension.toLowerCase())) {
            throw new StorageException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                "不支持的文件类型: " + extension);
        }

        // 4. 检查 MIME 类型
        String contentType = file.getContentType();
        if (!isValidMimeType(contentType)) {
            throw new StorageException(ErrorCode.FILE_TYPE_NOT_SUPPORTED,
                "不支持的 MIME 类型: " + contentType);
        }

        // 5. 检查图片尺寸
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new StorageException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "无法读取图片");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (width < minDimension || height < minDimension ||
                width > maxDimension || height > maxDimension) {
                throw new StorageException(ErrorCode.IMAGE_DIMENSION_INVALID,
                    String.format("图片尺寸 %dx%d 不符合要求 (%d~%d)",
                        width, height, minDimension, maxDimension));
            }
        } catch (IOException e) {
            throw new StorageException(ErrorCode.FILE_UPLOAD_FAILED, "读取图片失败", e);
        }
    }
}
```

---

*文档版本: v1.0 | 创建日期: 2026-04-18 | 状态: 待评审*

---

# 图片上传功能 - 开发契约

> **版本**: v1.0
> **日期**: 2026-04-18
> **依据设计**: `docs/arch/image-upload-design.md`

---

## 1. 后端开发任务清单

### 任务 B-1: 新增错误码

**输入**:
- 设计文档中的错误码定义

**输出**:
- `ErrorCode.java` 新增枚举值

**验收标准**:
```java
// ErrorCode.java 包含以下新枚举
FILE_TYPE_NOT_SUPPORTED(4005, "文件类型不支持"),
FILE_SIZE_EXCEEDED(4006, "文件大小超过限制"),
IMAGE_DIMENSION_INVALID(4007, "图片尺寸不符合要求"),
FILE_UPLOAD_FAILED(5001, "文件上传失败");
```

**预估工时**: 0.5h

---

### 任务 B-2: 配置文件更新

**输入**:
- 设计文档中的配置示例

**输出**:
- `application.yml` 新增上传配置

**验收标准**:
```yaml
# application.yml 包含以下配置
spring.servlet.multipart.max-file-size: 5MB
upload.type: local
upload.local.base-path: uploads
upload.limits.max-size: 5242880
upload.limits.allowed-types: jpg,jpeg,png,webp
```

**预估工时**: 0.5h

---

### 任务 B-3: 创建存储服务接口

**输入**:
- 设计文档第 6.1 节接口定义

**输出**:
- `src/main/java/com/carrental/infrastructure/storage/ImageStorageService.java`
- `src/main/java/com/carrental/infrastructure/storage/StorageResult.java`
- `src/main/java/com/carrental/infrastructure/storage/StorageException.java`

**验收标准**:
- 接口包含 `store()`, `delete()`, `exists()` 方法
- `StorageResult` 包含 `url`, `filename`, `size`, `contentType` 字段
- `StorageException` 包含错误码

**预估工时**: 1h

---

### 任务 B-4: 实现本地存储服务

**输入**:
- `ImageStorageService` 接口
- 配置参数

**输出**:
- `src/main/java/com/carrental/infrastructure/storage/LocalImageStorageService.java`
- `src/main/java/com/carrental/infrastructure/storage/ImageValidator.java`

**验收标准**:
- 实现文件存储到 `uploads/vehicles/YYYY/MM/` 目录
- 文件命名符合 `v{vehicleId}_{timestamp}_{random6}.{ext}` 规则
- 校验文件类型、大小、图片尺寸
- 返回正确的 URL 格式: `/uploads/vehicles/...`

**预估工时**: 3h

---

### 任务 B-5: 创建图片上传控制器

**输入**:
- `ImageStorageService` 实现
- API 设计规范

**输出**:
- `src/main/java/com/carrental/controller/AdminImageController.java`

**验收标准**:
```java
@PostMapping("/api/v1/admin/images/upload")
public ApiResponse<UploadResult> upload(
    @RequestParam("file") MultipartFile file,
    @RequestParam(value = "vehicle_id", required = false) Long vehicleId
)
```
- 需要 `@PreAuthorize("hasRole('ADMIN')")` 权限
- 返回格式符合 API 响应规范
- 错误时返回正确的错误码

**预估工时**: 2h

---

### 任务 B-6: 配置静态资源映射

**输入**:
- 设计文档第 5.1 节

**输出**:
- `WebMvcConfig.java` 新增 `addResourceHandlers()` 方法

**验收标准**:
- 访问 `/uploads/**` 映射到 `file:uploads/` 目录
- 开发环境可直接访问静态资源

**预估工时**: 0.5h

---

### 任务 B-7: 后端单元测试

**输入**:
- 所有新增类

**输出**:
- `src/test/java/com/carrental/infrastructure/storage/ImageStorageServiceTest.java`
- `src/test/java/com/carrental/controller/AdminImageControllerTest.java`

**验收标准**:
- 测试覆盖正常上传流程
- 测试覆盖异常情况（类型错误、大小超限）
- 所有测试通过

**预估工时**: 2h

---

## 2. 前端开发任务清单

### 任务 F-1: 创建图片上传组件

**输入**:
- Element Plus el-upload 组件文档
- 设计文档第 8.1 节组件设计

**输出**:
- `frontend-admin/src/components/ImageUploader.vue`

**验收标准**:
- 支持拖拽上传
- 支持点击上传
- 显示上传进度
- 支持图片预览（缩略图）
- 支持删除图片
- 限制最大上传数量

**预估工时**: 3h

---

### 任务 F-2: 集成到车辆管理页面

**输入**:
- `ImageUploader.vue` 组件
- `VehicleView.vue` 现有代码

**输出**:
- 更新 `frontend-admin/src/views/vehicles/VehicleView.vue`
- 删除原有的 textarea 图片输入

**验收标准**:
- 新增/编辑车辆时显示 ImageUploader 组件
- `form.images` 正确绑定到 ImageUploader
- 提交时 `images` 数组包含上传后的 URL

**代码变更示意**:
```vue
<!-- 替换原来的 -->
<el-form-item label="图片" prop="images">
  <el-input v-model="imagesInput" type="textarea" ... />
</el-form-item>

<!-- 替换为 -->
<el-form-item label="图片" prop="images">
  <ImageUploader
    v-model="form.images"
    :vehicle-id="editingId"
    :max-count="5"
  />
</el-form-item>
```

**预估工时**: 1h

---

### 任务 F-3: 配置 Vite Proxy

**输入**:
- 设计文档第 5.1 节

**输出**:
- 更新 `frontend-admin/vite.config.js`

**验收标准**:
```javascript
proxy: {
  '/api': { target: 'http://localhost:8081', changeOrigin: true },
  '/uploads': { target: 'http://localhost:8081', changeOrigin: true }
}
```

**预估工时**: 0.5h

---

### 任务 F-4: 前端样式优化

**输入**:
- ImageUploader 组件

**输出**:
- 上传区域样式
- 预览图片样式
- 删除按钮样式

**验收标准**:
- 上传区域大小合适（建议 150x150 px）
- 预览图片清晰
- 删除按钮位置明显但不遮挡图片
- 整体风格与 Element Plus 一致

**预估工时**: 1h

---

### 任务 F-5: 前端错误处理

**输入**:
- 后端错误码定义

**输出**:
- 友好的错误提示

**验收标准**:
| 错误码 | 前端提示 |
|--------|----------|
| 4005 | "仅支持 jpg、png、webp 格式的图片" |
| 4006 | "图片大小不能超过 5MB" |
| 4007 | "图片尺寸需在 100x100 到 4096x4096 之间" |
| 5001 | "上传失败，请重试" |

**预估工时**: 0.5h

---

## 3. 任务依赖关系

### 3.1 依赖图

```
后端任务依赖:
B-1 (错误码) ─────┐
                   │
B-2 (配置) ────────┼──> B-3 (接口) ──> B-4 (本地存储) ──> B-5 (控制器) ──> B-7 (测试)
                   │                           │
                   └───────────────────────────┘

B-6 (静态资源映射) ──> 可独立进行


前端任务依赖:
F-1 (组件) ──> F-2 (集成) ──> F-4 (样式) ──> F-5 (错误处理)

F-3 (Vite配置) ──> 可独立进行
```

### 3.2 可并行的任务组

| 并行组 | 任务 | 说明 |
|--------|------|------|
| **并行组 1** | B-1, B-2, B-6, F-3 | 配置类任务，无依赖 |
| **并行组 2** | B-3 | 需等待 B-1 完成 |
| **并行组 3** | B-4, F-1 | 后端存储实现 + 前端组件开发可并行 |
| **并行组 4** | B-5 | 需等待 B-4 完成 |
| **并行组 5** | F-2, B-7 | 集成 + 后端测试可并行 |
| **并行组 6** | F-4, F-5 | 收尾任务 |

---

## 4. 开发顺序建议

### 推荐顺序（单人开发）

```
Phase 1: 基础配置 (0.5 天)
├── B-1: 错误码
├── B-2: 配置文件
├── B-6: 静态资源映射
└── F-3: Vite Proxy

Phase 2: 后端核心 (1 天)
├── B-3: 存储接口
├── B-4: 本地存储实现
└── B-5: 上传控制器

Phase 3: 前端开发 (0.5 天)
├── F-1: 上传组件
├── F-2: 页面集成
└── F-4: 样式优化

Phase 4: 测试与收尾 (0.5 天)
├── B-7: 后端测试
└── F-5: 错误处理
```

### 推荐顺序（前后端分离开发）

**后端工程师**:
```
B-1 → B-2 → B-6 → B-3 → B-4 → B-5 → B-7
```

**前端工程师**:
```
F-3 → (等待 B-5 完成) → F-1 → F-2 → F-4 → F-5
```

---

## 5. 总预估工时

| 类型 | 任务 | 工时 |
|------|------|------|
| **后端** | B-1 ~ B-7 | 9.5h |
| **前端** | F-1 ~ F-5 | 6h |
| **联调** | 集成测试 | 2h |
| **总计** | - | **17.5h ≈ 2.5 天** |

---

## 6. 验收检查清单

### 6.1 功能验收

- [ ] 管理端可上传图片（拖拽 + 点击）
- [ ] 上传后显示预览
- [ ] 可删除已上传图片
- [ ] 图片数量限制生效（默认 5 张）
- [ ] 文件类型限制生效（仅 jpg/png/webp）
- [ ] 文件大小限制生效（最大 5MB）
- [ ] 图片尺寸限制生效（100~4096 px）
- [ ] 保存车辆后图片正确显示

### 6.2 技术验收

- [ ] 图片存储在 `backend/uploads/vehicles/` 目录
- [ ] 数据库 `vehicles.images` 存储 URL 而非 base64
- [ ] 后端单元测试全部通过
- [ ] 静态资源可通过 `/uploads/` 访问
- [ ] 前端 proxy 配置正确

### 6.3 兼容性验收

- [ ] 小程序端图片正常展示（使用新 URL）

### 6.4 性能验收

- [ ] 单张图片上传时间 < 2s（5MB 以内）
- [ ] 车辆列表加载速度未明显下降
- [ ] 数据库查询性能未下降

---

## 7. 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 图片迁移失败 | 中 | 高 | 备份数据库，增量迁移，失败回滚 |
| 磁盘空间不足 | 低 | 高 | 监控磁盘使用，设置告警阈值 |
| 大文件上传超时 | 低 | 中 | 前端限制文件大小，后端校验 |
| XSS 攻击 | 低 | 高 | 不使用原始文件名，MIME 类型白名单 |

---

## 8. 后续迭代规划

### v1.6 - 图片优化
- 图片自动压缩（> 1MB 自动压缩）
- 图片格式自动转 webp
- 图片水印功能

### v1.7 - OSS 迁移
- 实现 `OssImageStorageService`
- 配置切换到阿里云 OSS
- CDN 加速

### v1.8 - 图片管理
- 图片删除接口
- 孤立文件清理脚本
- 图片使用统计

---

*契约版本: v1.0 | 创建日期: 2026-04-18 | 状态: 待确认*
