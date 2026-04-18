package com.carrental.infrastructure.storage;

import com.carrental.common.result.ErrorCode;
import lombok.Getter;

/**
 * 存储异常
 */
@Getter
public class StorageException extends RuntimeException {

    private final int code;

    public StorageException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public StorageException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public StorageException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    public StorageException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
    }
}
