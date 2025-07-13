package com.maple.spaceservice.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maple.maplepicture.domain.space.entity.SpaceUser;
import com.maple.maplepicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;

/**
* @author Administrator
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-04-26 15:57:42
*/
public interface SpaceUserDomainService {

    /**
     * 获取查询对象
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
