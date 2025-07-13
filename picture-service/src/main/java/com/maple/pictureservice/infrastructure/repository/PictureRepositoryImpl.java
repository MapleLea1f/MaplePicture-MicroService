package com.maple.pictureservice.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maple.maplepicture.domain.picture.entity.Picture;
import com.maple.maplepicture.domain.picture.repository.PictureRepository;
import com.maple.maplepicture.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
 * 图片仓储
 */
@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
