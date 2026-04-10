package com.carrental.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.domain.user.User;
import com.carrental.domain.user.UserRepository;
import com.carrental.infrastructure.persistence.dataobject.UserDO;
import com.carrental.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(Long id) {
        UserDO userDO = userMapper.selectById(id);
        return Optional.ofNullable(userDO).map(this::toDomain);
    }

    @Override
    public Optional<User> findByOpenid(String openid) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getWechatOpenid, openid);
        UserDO userDO = userMapper.selectOne(wrapper);
        return Optional.ofNullable(userDO).map(this::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getPhone, phone);
        UserDO userDO = userMapper.selectOne(wrapper);
        return Optional.ofNullable(userDO).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserDO userDO = toDO(user);
        if (user.getId() == null) {
            userMapper.insert(userDO);
            user.setId(userDO.getId());
        } else {
            userMapper.updateById(userDO);
        }
        return user;
    }

    @Override
    public boolean existsByOpenid(String openid) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getWechatOpenid, openid);
        return userMapper.exists(wrapper);
    }

    private User toDomain(UserDO userDO) {
        User user = new User(userDO.getPhone(), userDO.getWechatOpenid(), userDO.getNickname());
        user.setId(userDO.getId());
        user.setRole(userDO.getRole());
        user.setStatus(userDO.getStatus());
        user.setPasswordHash(userDO.getPasswordHash());
        user.setMustChangePwd(Boolean.TRUE.equals(userDO.getMustChangePwd()));
        user.setCreatedAt(userDO.getCreatedAt());
        user.setUpdatedAt(userDO.getUpdatedAt());
        return user;
    }

    private UserDO toDO(User user) {
        UserDO userDO = new UserDO();
        userDO.setId(user.getId());
        userDO.setPhone(user.getPhone());
        userDO.setWechatOpenid(user.getWechatOpenid());
        userDO.setNickname(user.getNickname());
        userDO.setRole(user.getRole());
        userDO.setStatus(user.getStatus());
        userDO.setPasswordHash(user.getPasswordHash());
        userDO.setMustChangePwd(user.isMustChangePwd());
        return userDO;
    }
}
