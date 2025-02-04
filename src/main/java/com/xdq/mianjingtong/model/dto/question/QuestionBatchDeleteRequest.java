package com.xdq.mianjingtong.model.dto.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author xdq
 * @date 2025/2/4 23:46
 * @description QuestionBatchDeleteRequest
 */
@Data
public class QuestionBatchDeleteRequest implements Serializable {

    /**
     * 题目 id 列表
     */
    private List<Long> questionIdList;

    private static final long serialVersionUID = 1L;
}
