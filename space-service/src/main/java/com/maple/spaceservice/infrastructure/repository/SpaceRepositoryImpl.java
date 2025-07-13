package com.maple.spaceservice.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maple.maplepicture.domain.space.entity.Space;
import com.maple.maplepicture.domain.space.repository.SpaceRepository;
import com.maple.maplepicture.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}
