package com.maple.assembler;

import com.maple.maplepicture.domain.user.entity.User;
import com.maple.maplepicture.interfaces.dto.user.UserAddRequest;
import com.maple.maplepicture.interfaces.dto.user.UserEditRequest;
import com.maple.maplepicture.interfaces.dto.user.UserUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 用户对象转换
 */
public class UserAssembler {

    public static User toUserEntity(UserAddRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }

    public static User toUserEntity(UserUpdateRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }

    public static User toUserEntity(UserEditRequest request) {
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }
}
