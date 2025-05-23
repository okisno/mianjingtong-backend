package com.xdq.mianjingtong.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.xdq.mianjingtong.annotation.AuthCheck;
import com.xdq.mianjingtong.common.BaseResponse;
import com.xdq.mianjingtong.common.DeleteRequest;
import com.xdq.mianjingtong.common.ErrorCode;
import com.xdq.mianjingtong.common.ResultUtils;
import com.xdq.mianjingtong.constant.UserConstant;
import com.xdq.mianjingtong.exception.BusinessException;
import com.xdq.mianjingtong.exception.ThrowUtils;
import com.xdq.mianjingtong.model.dto.question.QuestionQueryRequest;
import com.xdq.mianjingtong.model.dto.questionBank.QuestionBankAddRequest;
import com.xdq.mianjingtong.model.dto.questionBank.QuestionBankEditRequest;
import com.xdq.mianjingtong.model.dto.questionBank.QuestionBankQueryRequest;
import com.xdq.mianjingtong.model.dto.questionBank.QuestionBankUpdateRequest;
import com.xdq.mianjingtong.model.entity.Question;
import com.xdq.mianjingtong.model.entity.QuestionBank;
import com.xdq.mianjingtong.model.entity.User;
import com.xdq.mianjingtong.model.vo.QuestionBankVO;
import com.xdq.mianjingtong.service.QuestionBankService;
import com.xdq.mianjingtong.service.QuestionService;
import com.xdq.mianjingtong.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 题库接口
 *
 *
 *
 */
@RestController
@RequestMapping("/questionBank")
@Slf4j
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private QuestionService questionService;

    @Resource
    private UserService userService;

    // region 增删改查

    /**
     * 创建题库
     *
     * @param questionBankAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestionBank(@RequestBody QuestionBankAddRequest questionBankAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankAddRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        questionBank.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankService.save(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankId = questionBank.getId();
        return ResultUtils.success(newQuestionBankId);
    }

    /**
     * 删除题库
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestionBank(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionBank.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题库（仅管理员可用）
     *
     * @param questionBankUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBankUpdateRequest questionBankUpdateRequest) {
        if (questionBankUpdateRequest == null || questionBankUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankUpdateRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        // 判断是否存在
        long id = questionBankUpdateRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题库（封装类）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(QuestionBankQueryRequest questionBankQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = questionBankQueryRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        //TODO: hotkey理解
        //TODO: 取消注释开启hotkey
        /*//生成 key
        String key = "bank_detail_" + id;
        //如果是热 key
        if (JdHotKeyStore.isHotKey(key)){
            //从本地缓存中获取缓存值
            Object cachedQuestionBankVo = JdHotKeyStore.get(key);
            if (cachedQuestionBankVo != null) {
                //如果缓存中有值，直接返回缓存的值
                return ResultUtils.success((QuestionBankVO) cachedQuestionBankVo);
            }
        }*/

        // 查询数据库
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 查询题库封装类
        QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionBank, request);
        // 是否要关联查询题库下的题目列表
        boolean needQueryQuestionList = questionBankQueryRequest.isNeedQueryQuestionList();
        if (needQueryQuestionList) {
            QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
            questionQueryRequest.setQuestionBankId(id);
            // 可以按需传递更多参数
            questionQueryRequest.setPageSize(questionBankQueryRequest.getPageSize());
            questionQueryRequest.setCurrent(questionBankQueryRequest.getCurrent());
            // 封装 question => questionVO
            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
            questionBankVO.setQuestionPage(questionService.getQuestionVOPage(questionPage, request));
        }

        //TODO: hotkey 理解 设置本地缓存
        //TODO:取消注释开启hotkey
        //本地缓存 (如果不是热key，这个方法不会设置缓存)
        //JdHotKeyStore.smartSet(key, questionBankVO);

        // 获取封装类
        return ResultUtils.success(questionBankVO);
    }


    /**
     * 分页获取题库列表（仅管理员可用）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionBank>> listQuestionBankByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        return ResultUtils.success(questionBankPage);
    }

    /**
     * 分页获取题库列表（封装类）
     * 因为考虑到系统题库数量不多，可以不需要分页实现，减少开发成本
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    /*
    sentinel 避免系统崩溃和保证服务稳定性，通常采取限流防止过载，除此之外，
    为了进一步隔离和保护系统，防止某些组件异常时影响系统的稳定性，还会采用 熔断机制 + 降级策略 进行兜底处理
    三件套： 限流，熔断，降级
    SentinelResource sentinel网站流量控制和熔断
        value 资源名称
        blockHandler 流量访问超过限制后，触发限流，阻塞操作    blockHandler指定处理阻塞操作的方法名
        fallback 熔断后触发降级，调用哪个方法进行降级操作
     */
    @PostMapping("/list/page/vo")
    @SentinelResource(value = "listQuestionBankVOByPage",
                blockHandler = "handleBlockException",
                fallback = "handleFallback")
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                               HttpServletRequest request) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 200, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    /**
     * 熔断规则：
     *  熔断条件：如果接口异常率超过10%，或者慢调用（响应时长>3s）的比例大于20%，触发60s熔断
     *  熔断：60s后进入半开状态，如果还是满足熔断条件，继续熔断，反之恢复正常
     *  listQuestionBankVOByPage 降级操作：直接返回本地数据或者空数据
     *
     *  熔断策略(降级方法)不止针对熔断规则执行，出现了任何业务异常都会调用降级处理器
     *
     *  handleFallback针对普通业务异常做处理
     */
    public BaseResponse<Page<QuestionBankVO>> handleFallback(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                             HttpServletRequest request, Throwable ex) {
        // 可以返回本地数据或空数据
        return ResultUtils.success(null);
    }

    /**
     * 限流规则：
     *  限流条件：整个接口每秒钟不超过10次请求
     *  listQuestionBankVOByPage 流控操作
     *  阻塞操作：
     *  限流：提示“系统压力过大，请耐心等待”
     *  熔断：执行奖及操作
     *
     *  sentinel的异常都叫BlockException，熔断触发后，
     *  handleBlockException会处理熔断之后的降级以及限流
     */
    public BaseResponse<Page<QuestionBankVO>> handleBlockException(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                   HttpServletRequest request, BlockException ex) {
        // 降级操作
        if (ex instanceof DegradeException) {
            return handleFallback(questionBankQueryRequest, request, ex);
        }
        // 限流操作
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统压力过大，请耐心等待");
    }


    /**
     * 分页获取当前登录用户创建的题库列表
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listMyQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionBankQueryRequest.setUserId(loginUser.getId());
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    /**
     * 编辑题库（给用户使用）
     *
     * @param questionBankEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestionBank(@RequestBody QuestionBankEditRequest questionBankEditRequest, HttpServletRequest request) {
        if (questionBankEditRequest == null || questionBankEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankEditRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionBankEditRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestionBank.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
