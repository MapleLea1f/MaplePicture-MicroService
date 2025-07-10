package com.maple.userservice.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maple.maplepicture.domain.user.entity.User;
import com.maple.maplepicture.infrastructure.common.DeleteRequest;
import com.maple.maplepicture.interfaces.dto.user.UserLoginRequest;
import com.maple.maplepicture.interfaces.dto.user.UserQueryRequest;
import com.maple.maplepicture.interfaces.dto.user.UserRegisterRequest;
import com.maple.maplepicture.interfaces.vo.user.LoginUserVO;
import com.maple.maplepicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-03-16 14:23:27
*/
public interface UserApplicationService {


    long userRegister(UserRegisterRequest userRegisterRequest);

    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    String getEncryptPassword(String userPassword);

    LoginUserVO getLoginUserVO(User user);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);

    User getLoginUser(HttpServletRequest request);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    long addUser(User user);

    User getUserById(long id);

    UserVO getUserVOById(long id);

    boolean deleteUser(DeleteRequest deleteRequest);

    void updateUser(User user);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    long saveUser(User userEntity);

    void editUser(User userEntity);

    boolean changePassword(Long userId, String oldPassword, String newPassword);
}
