package com.maple.spaceservice.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maple.maplepicture.domain.space.entity.SpaceUser;
import com.maple.maplepicture.domain.space.repository.SpaceUserRepository;
import com.maple.maplepicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
