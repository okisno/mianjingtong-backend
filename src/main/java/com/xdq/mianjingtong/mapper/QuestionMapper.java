package com.xdq.mianjingtong.mapper;

import com.xdq.mianjingtong.model.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author 31216
* @description 针对表【question(题目)】的数据库操作Mapper
* @createDate 2025-01-17 22:52:15
* @Entity com.xdq.mianjingtong.model.entity.Question
*/
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 查询题目列表（包括已被删除的数据）
     */
    @Select("select * from question where updateTime >= #{minUpdateTime}")
    List<Question> listQuestionWithDelete(Date minUpdateTime);
}



