package com.xdq.mianjingtong.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xdq.mianjingtong.model.dto.user.UserQueryRequest;
import com.xdq.mianjingtong.model.entity.User;
import com.xdq.mianjingtong.model.vo.LoginUserVO;
import com.xdq.mianjingtong.model.vo.UserVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import io.swagger.models.auth.In;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;

/**
 * 用户服务
 *
 *  
 *  
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户登录（微信开放平台）
     *
     * @param wxOAuth2UserInfo 从微信获取的用户信息
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 添加用户签到记录
     * @param userId 用户id
     * @return 当前用户是否已经签到成功
     * 年份此处不作为变量提供，因为当前时间可以直接获取
     */
    boolean addUserSignIn(long userId);

    /**
     * 获取用户某年份签到记录
     * @param userId 用户id
     * @param year 年份
     * @return 签到记录映射
     */
    //Map<LocalDate, Boolean> getUserSignInStatus(long userId, Integer year);
    List<Integer> getUserSignInStatus(long userId, Integer year);

}
