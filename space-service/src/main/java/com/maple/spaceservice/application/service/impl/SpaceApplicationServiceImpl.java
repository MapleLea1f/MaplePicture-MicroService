package com.maple.spaceservice.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maple.maplepicture.application.service.SpaceApplicationService;
import com.maple.maplepicture.application.service.SpaceUserApplicationService;
import com.maple.maplepicture.application.service.UserApplicationService;
import com.maple.maplepicture.domain.space.entity.Space;
import com.maple.maplepicture.domain.space.entity.SpaceUser;
import com.maple.maplepicture.domain.space.service.SpaceDomainService;
import com.maple.maplepicture.domain.space.valueobject.SpaceLevelEnum;
import com.maple.maplepicture.domain.space.valueobject.SpaceRoleEnum;
import com.maple.maplepicture.domain.space.valueobject.SpaceTypeEnum;
import com.maple.maplepicture.domain.user.entity.User;
import com.maple.maplepicture.infrastructure.exception.BusinessException;
import com.maple.maplepicture.infrastructure.exception.ErrorCode;
import com.maple.maplepicture.infrastructure.exception.ThrowUtils;
import com.maple.maplepicture.infrastructure.mapper.SpaceMapper;
import com.maple.maplepicture.interfaces.dto.space.SpaceAddRequest;
import com.maple.maplepicture.interfaces.dto.space.SpaceQueryRequest;
import com.maple.maplepicture.interfaces.vo.space.SpaceVO;
import com.maple.maplepicture.interfaces.vo.user.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-04-16 14:59:48
*/
@Service
public class SpaceApplicationServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceApplicationService {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Autowired
    private SpaceDomainService spaceDomainService;

//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 填充参数默认值
        // 转换实体类 和 DTO
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 按照级别填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 2. 校验参数
        space.validSpace(true);
        // 3. 校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限创建指定级别空间");
        }
        // 4. 控制同一个用户只能创建一个私有空间(加锁 + 事务)、以及一个团队空间
        // intern()方法确保lock用的是同一个对象
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 判断是否已有空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                // 如果已有空间，就不能再创建
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能拥有一个");
                // 创建新的空间
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                // 如果是团队空间，关联新增团队成员记录
                if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    result = spaceUserApplicationService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                // 创建分表
//                dynamicShardingManager.createSpacePictureTable(space);
                // 返回新写入的数据id
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }



    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 1=> user1, 2=> user2
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userApplicationService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        return spaceDomainService.getQueryWrapper(spaceQueryRequest);
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    @Override
    public void checkSpaceAuth(Space space, User loginUser) {
       spaceDomainService.checkSpaceAuth(space, loginUser);
    }
}




