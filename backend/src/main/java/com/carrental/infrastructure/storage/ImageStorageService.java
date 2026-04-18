package com.carrental.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 图片存储服务接口
 *
 * 设计原则：
 * - 接口抽象，支持未来扩展到 OSS 等云存储
 * - 本地存储实现：LocalImageStorageService
 * - 云存储实现（未来）：OssImageStorageService
 */
public interface ImageStorageService {

    /**
     * 存储图片
     *
     * @param file      上传的文件
     * @param vehicleId 关联车辆 ID (可选，用于目录组织，可为 null 或 0)
     * @return 存储结果 (包含 URL)
     * @throws StorageException 存储失败
     */
    StorageResult store(MultipartFile file, Long vehicleId) throws StorageException;

    /**
     * 删除图片
     *
     * @param url 图片 URL
     * @return 是否删除成功
     * @throws StorageException 删除失败
     */
    boolean delete(String url) throws StorageException;

    /**
     * 检查图片是否存在
     *
     * @param url 图片 URL
     * @return 是否存在
     */
    boolean exists(String url);
}
