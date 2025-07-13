package com.maple.spaceservice.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maple.maplepicture.domain.space.entity.SpaceUser;
import com.maple.maplepicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.maple.maplepicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.maple.maplepicture.interfaces.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-04-26 15:57:42
*/
public interface SpaceUserApplicationService extends IService<SpaceUser> {
    /**
     * 创建空间成员
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 校验空间成员
     * @param spaceUser
     * @param add 是否为创建时校验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员包装类（单条）
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取空间成员包装类（列表）
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取查询对象
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
