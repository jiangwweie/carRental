package com.carrental.infrastructure.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 存储结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageResult {

    /**
     * 相对 URL (如: /uploads/vehicles/2026/04/xxx.jpg)
     */
    private String url;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件大小 (字节)
     */
    private long size;

    /**
     * MIME 类型
     */
    private String contentType;
}
