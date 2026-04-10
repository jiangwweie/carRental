package com.carrental.domain.user;

import java.util.Optional;

/**
 * UserRepository 接口定义
 * 由 infrastructure 层实现
 */
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByOpenid(String openid);

    Optional<User> findByPhone(String phone);

    User save(User user);

    boolean existsByOpenid(String openid);
}
