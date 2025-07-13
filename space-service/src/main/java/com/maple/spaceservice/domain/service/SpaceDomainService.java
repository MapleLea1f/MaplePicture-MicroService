package com.maple.spaceservice.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maple.maplepicture.domain.space.entity.Space;
import com.maple.maplepicture.domain.user.entity.User;
import com.maple.maplepicture.interfaces.dto.space.SpaceQueryRequest;

/**
* @author Administrator
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-04-16 14:59:48
*/
public interface SpaceDomainService {

    /**
     * 获取查询对象
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间等级填充空间对象
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     * @param space
     * @param loginUser
     */
    void checkSpaceAuth(Space space, User loginUser);
}
