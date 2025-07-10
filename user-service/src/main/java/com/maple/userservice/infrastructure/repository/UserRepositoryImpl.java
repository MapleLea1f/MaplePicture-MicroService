package com.maple.userservice.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maple.maplepicture.domain.user.entity.User;
import com.maple.maplepicture.domain.user.repository.UserRepository;
import com.maple.maplepicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户仓储实现
 */
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
