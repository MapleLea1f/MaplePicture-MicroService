package com.maple.spaceservice.domain.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maple.maplepicture.domain.space.entity.Space;
import com.maple.maplepicture.domain.space.repository.SpaceRepository;
import com.maple.maplepicture.domain.space.service.SpaceDomainService;
import com.maple.maplepicture.domain.space.valueobject.SpaceLevelEnum;
import com.maple.maplepicture.domain.user.entity.User;
import com.maple.maplepicture.infrastructure.exception.BusinessException;
import com.maple.maplepicture.infrastructure.exception.ErrorCode;
import com.maple.maplepicture.interfaces.dto.space.SpaceQueryRequest;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
* @author Administrator
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-04-16 14:59:48
*/
@Service
public class SpaceDomainServiceImpl implements SpaceDomainService {

    @Resource
    private SpaceRepository spaceRepository;

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            // 当管理员未设置空间最大容量时，才按空间级别设置
            if (space.getMaxSize() == null){
                space.setMaxSize(maxSize);
            }
            // 当管理员未设置空间最大图片数量时，才按空间级别设置
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null){
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
        // 仅本人或管理员可删除
        if (!space.getUserId().equals(loginUser.getId()) && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




