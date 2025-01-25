package com.xdq.mianjingtong.service.impl;

import static com.xdq.mianjingtong.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xdq.mianjingtong.common.ErrorCode;
import com.xdq.mianjingtong.constant.CommonConstant;
import com.xdq.mianjingtong.constant.RedisConstant;
import com.xdq.mianjingtong.exception.BusinessException;
import com.xdq.mianjingtong.mapper.UserMapper;
import com.xdq.mianjingtong.model.dto.user.UserQueryRequest;
import com.xdq.mianjingtong.model.entity.User;
import com.xdq.mianjingtong.model.enums.UserRoleEnum;
import com.xdq.mianjingtong.model.vo.LoginUserVO;
import com.xdq.mianjingtong.model.vo.UserVO;
import com.xdq.mianjingtong.service.UserService;
import com.xdq.mianjingtong.utils.SqlUtils;

import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * 用户服务实现
 *
 *  
 *  
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 盐值，混淆密码
     */
    public static final String SALT = "yupi";

    @Autowired
    private RedissonClient redissonClient;

    public UserServiceImpl(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                user.setUserName(wxOAuth2UserInfo.getNickname());
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return getLoginUserVO(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        String userAccount = userQueryRequest.getUserAccount();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 添加用户签到记录
     * @param userId 用户id
     * @return 当前用户是否已经签到成功
     * 年份此处不作为变量提供，因为当前时间可以直接获取
     */
    @Override
    public boolean addUserSignIn(long userId) {
        LocalDate date = LocalDate.now();
        String key = RedisConstant.getUserSignInRedisKey(date.getYear(), userId);

        // 获取到 Redis 的 BitMap
        RBitSet signInBitSet = redissonClient.getBitSet(key);

        // 偏移量：获取当天是一年中的第几天 从 1 开始计算
        int offset = date.getDayOfYear();

        // 查询当天是否签到
        if (!signInBitSet.get(offset)) {
            // 表示当天未签到，执行签到行为
            signInBitSet.set(offset, true);
        }

        // 执行至此，当天用户一定已经签到，返回true即可，返回值代表当天是否签到
        return true;
    }

    /**
     * 获取用户某年份签到记录
     * @param userId 用户id
     * @param year 年份
     * @return 签到记录映射
     */
    @Override
    public List<Integer> getUserSignInStatus(long userId, Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }

        // 获取 Redis 的 BitMap
        RBitSet signInBitSet = redissonClient.getBitSet(RedisConstant.getUserSignInRedisKey(year, userId));
        /*
        优化点1：
            先获取到所有值，并且做缓存，而不是在for循环里根据日期获取值，可以避免多次发送请求，提升效率
            jdk自带的BitSet，将数据缓存在Java内存中
            ！！！加载 BitSet 到内存中，避免后续读取时发送多次请求
         */
        BitSet bitSet = signInBitSet.asBitSet();

        // 优化点2：我们返回了一年中每天的签到情况，接口的传输数据很多，但是事实上我们只需要返回签到了的日期，甚至是签到的时一年中第几天，然后交给前端处理即可
        List<Integer> dayList = new ArrayList<>();

        //方法三,BitSet特性，查找为1的位置即可，大大减少了查找的次数，循环效率要比for循环高的多，基于二进制位运算
        // 从索引0开始查找下一个被设置为1的值
        int index = bitSet.nextSetBit(0);
        while (index >= 0){
            dayList.add(index);
            // 继续查找下一个被设置为1的位
            index = bitSet.nextSetBit(index + 1);
        }


        // 构造返回结果
        // LinkedHashMap 不仅可以作为映射，还可以保证数据的有序性，将二维数组转化为map了 ["01-01", true]
        //Map<LocalDate, Boolean> result = new LinkedHashMap<>();


        // 获取当前年份有多少天
        int days = Year.of(year).length();

        /*// 遍历天数，获取每天的签到状态
        for (int dayOfYear = 1; dayOfYear <= days; dayOfYear++) {
            *//*
            //方法一：
            // key 当前日期
            // 同样是jdk时间包下的方法
            LocalDate currentDate = LocalDate.ofYearDay(year, dayOfYear);

            // 获取 value 当天是否刷题
            // bitSet 实际上是 Redisson 客户端和 Redis 交互的对象，每次调用 get方法都会触发一次 Redis 请求来获取值，没有在本地做缓存
            //boolean isWork = signInBitSet.get(dayOfYear);

            // 改用 Java 的 BitSet ，提前将数据缓存到 Java 内存中，不再需要频繁发送 Redis 请求
            boolean isWork = bitSet.get(dayOfYear);

            result.put(currentDate, isWork);*//*

            *//*//方法二：
            boolean isWork = bitSet.get(dayOfYear);

            if (isWork) {
                dayList.add(dayOfYear);
            }*//*


        }*/

        //return result;
        return dayList;
    }
}
