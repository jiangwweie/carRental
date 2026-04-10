package com.carrental.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carrental.infrastructure.persistence.dataobject.MessageSubscriptionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageSubscriptionMapper extends BaseMapper<MessageSubscriptionDO> {
}
