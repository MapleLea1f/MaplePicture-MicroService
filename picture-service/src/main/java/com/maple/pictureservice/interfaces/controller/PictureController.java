package com.maple.pictureservice.interfaces.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maple.maplepicture.application.service.PictureApplicationService;
import com.maple.maplepicture.application.service.SpaceApplicationService;
import com.maple.maplepicture.application.service.UserApplicationService;
import com.maple.maplepicture.domain.picture.entity.Picture;
import com.maple.maplepicture.domain.picture.valueobject.PictureReviewStatusEnum;
import com.maple.maplepicture.domain.space.entity.Space;
import com.maple.maplepicture.domain.user.constant.UserConstant;
import com.maple.maplepicture.domain.user.entity.User;
import com.maple.maplepicture.infrastructure.annotation.AuthCheck;
import com.maple.maplepicture.infrastructure.api.aliyunai.AliYunAiApi;
import com.maple.maplepicture.infrastructure.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.maple.maplepicture.infrastructure.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.maple.maplepicture.infrastructure.api.imagesearch.ImageSearchApiFacade;
import com.maple.maplepicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.maple.maplepicture.infrastructure.common.BaseResponse;
import com.maple.maplepicture.infrastructure.common.DeleteRequest;
import com.maple.maplepicture.infrastructure.common.ResultUtils;
import com.maple.maplepicture.infrastructure.exception.BusinessException;
import com.maple.maplepicture.infrastructure.exception.ErrorCode;
import com.maple.maplepicture.infrastructure.exception.ThrowUtils;
import com.maple.maplepicture.interfaces.assembler.PictureAssembler;
import com.maple.maplepicture.interfaces.dto.picture.*;
import com.maple.maplepicture.interfaces.vo.picture.PictureTagCategory;
import com.maple.maplepicture.interfaces.vo.picture.PictureVO;
import com.maple.maplepicture.shared.auth.SpaceUserAuthManager;
import com.maple.maplepicture.shared.auth.StpKit;
import com.maple.maplepicture.shared.auth.annotation.SaSpaceCheckPermission;
import com.maple.maplepicture.shared.auth.model.SpaceUserPermissionConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private PictureApplicationService pictureApplicationService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();


    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        String fileUrl = pictureUploadRequest.getFileUrl();
        User loginUser = userApplicationService.getLoginUser(request);
        PictureVO pictureVO = pictureApplicationService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 批量抓取并创建图片
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        Integer uploadCount = pictureApplicationService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        long id = deleteRequest.getId();
        pictureApplicationService.deletePicture(id, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture pictureEntity = PictureAssembler.toPictureEntity(pictureUpdateRequest);
        // 注意将 list 转为 string
        pictureEntity.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 补充审核参数
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.fillReviewParams(pictureEntity, loginUser);
        // 数据校验
        pictureApplicationService.validPicture(pictureEntity);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = pictureApplicationService.updateById(pictureEntity);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureApplicationService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            // 已改为注解鉴权
            // User loginUser = userService.getLoginUser(request);
            // pictureService.checkPictureAuth(loginUser, picture);
            space = spaceApplicationService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        User loginUser = userApplicationService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureApplicationService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, size),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 公共图库
            // 普通用户默认只能看到审核通过的数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 私有空间
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            // 已经改为使用Sa-Token权限校验
//            User loginUser = userService.getLoginUser(request);
//            Space space = spaceService.getById(spaceId);
//            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
        }
        // 查询数据库
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, size),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureApplicationService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表（封装类，Redis缓存）
     */
    @Deprecated //私人用的
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能看到审核通过的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 查询缓存，缓存中没有，再查询数据库
        // 构建缓存的key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("maplepicture:listPictureVOByPage:%s", hashKey);
        // 1. 先从本地缓存中查询(caffeine)
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，将缓存内容转换成实体类并返回
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        // 2. 本地缓存未命中，查询Redis分布式缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，更新本地缓存并返回
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }

        // 3. 查询数据库
        Page<Picture> picturePage = pictureApplicationService.page(new Page<>(current, size),
                pictureApplicationService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureApplicationService.getPictureVOPage(picturePage, request);
        // 4. 更新缓存
        // 写入 Redis 缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 设置缓存的过期时间，5 - 10 分钟过期（预防缓存雪崩，缓存击穿，缓存一致性）
        int cacheExpireTime = 300 + RandomUtil.randomInt(0,300);
        opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        // 写入本地缓存(caffeine)
        LOCAL_CACHE.put(cacheKey, cacheValue);

        // 获取封装类
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        Picture pictureEntity = PictureAssembler.toPictureEntity(pictureEditRequest);
        pictureApplicationService.editPicture(pictureEntity, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 审核图片
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<?> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureApplicationService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getThumbnailUrl());
        return ResultUtils.success(resultList);
    }


    /**
     * 按照颜色搜索
     */
    @PostMapping("search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request){
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userApplicationService.getLoginUser(request);
        List<PictureVO> result = pictureApplicationService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 批量编辑图片
     * @param pictureEditByBatchRequest
     * @param request
     * @return
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        pictureApplicationService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureApplicationService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }




}
