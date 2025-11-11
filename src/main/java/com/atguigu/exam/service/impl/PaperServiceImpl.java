package com.atguigu.exam.service.impl;


import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.entity.PaperQuestion;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.mapper.ExamRecordMapper;
import com.atguigu.exam.mapper.PaperMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.PaperQuestionService;
import com.atguigu.exam.service.PaperService;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.vo.PaperVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {
    private final QuestionMapper questionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final PaperQuestionService paperQuestionService;
    /**
     * 根据试卷id试卷详情
     * 试卷对象
     * 题目集合
     * 注意： 题目的选项sort正序
     * 注意： 所有题目根据类型排序
     * @param id 试卷id
     * @return
     */
    @Override
    public Paper customPaperDetailById(Long id) {
        //1. 单表java代码进行paper查询
        Paper paper = getById(id);
        //2. 校验paper == null -> 抛异常
        if (paper == null){
            throw new RuntimeException("指定id:%s试卷已经被删除，无法查看详情！".formatted(id));
        }
        //3. 根据paperid查询题目集合（中间，题目，答案，选项）
        List<Question> questionList = questionMapper.customQueryQuestionListByPaperId(id);
        //4. 校验题目集合 == null -> 赋空集合！ log->做好记录
        if (ObjectUtils.isEmpty(questionList)){
            paper.setQuestions(new ArrayList<Question>());
            log.warn("试卷中没有题目！可以进行试卷编辑！但是不能用于考试！！,对应试卷id：{}",id);
            return paper;
        }
        log.debug("题目信息排序前：{}",questionList);
        //对题目进行排序（选择 -> 判断 -> 简答）
        questionList.sort((o1, o2) -> Integer.compare(typeToInt(o1.getType()),typeToInt(o2.getType())));
        //注意：type排序，是字符类型 -》 字符 -》 对应 -》 固定的数字 1 2 3
        log.debug("题目信息排序后：{}",questionList);
        //进行paper题目集合赋值
        paper.setQuestions(questionList);
        return paper;
    }

    @Override
    public Paper customCreatePaper(PaperVo paperVo) {
        //1. 完善试卷内信息 名字 描述 时间  -> 状态 ，总题目数 ， 总分数
        Paper paper = new Paper();
        //名字 描述 时间
        BeanUtils.copyProperties(paperVo,paper);
        //态 ，总题目数, 总分数
        paper.setStatus("DRAFT");
        if (ObjectUtils.isEmpty(paperVo.getQuestions())){
            //本次没选题目
            paper.setTotalScore(BigDecimal.ZERO);
            paper.setQuestionCount(0);
            save(paper);
            log.warn("本次{}组卷，没有选择题目！注意没有题目的试卷无法进行考试！！",paper);
            return paper;
        }
    /*
        状态默认值： DRAFT
        总题目数： question长度
        总分数： question分数的和
     */
        paper.setQuestionCount(paperVo.getQuestions().size());
        paper.setTotalScore(paperVo.getQuestions().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        //2. 完成试卷的插入 -》 主键回显 paperId
        save(paper);

        //3. 中间表集合插入 【批量插入】 -》 中间表的service对象
        List<PaperQuestion> paperQuestionList = paperVo.getQuestions().entrySet().stream().
                map(entry -> new PaperQuestion(paper.getId().intValue(), Long.valueOf(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());

        //4. 中间表的批量插入
        paperQuestionService.saveBatch(paperQuestionList);
        return paper;
    }

    /**
     * 获取题目类型的排序顺序
     * @param type 题目类型
     * @return 排序序号
     */
    private int typeToInt(String type) {
        switch (type) {
            case "CHOICE": return 1; // 选择题
            case "JUDGE": return 2;  // 判断题
            case "TEXT": return 3;   // 简答题
            default: return 4;       // 其他类型
        }
    }
}