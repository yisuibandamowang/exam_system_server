package com.atguigu.exam.service.impl;

import com.atguigu.exam.common.CacheConstants;
import com.atguigu.exam.entity.Question;
import com.atguigu.exam.entity.QuestionAnswer;
import com.atguigu.exam.entity.QuestionChoice;
import com.atguigu.exam.mapper.QuestionAnswerMapper;
import com.atguigu.exam.mapper.QuestionChoiceMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.QuestionService;
import com.atguigu.exam.utils.RedisUtils;
import com.atguigu.exam.vo.QuestionQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {

    private final QuestionChoiceMapper questionChoiceMapper;
    private final QuestionAnswerMapper questionAnswerMapper;
    private final QuestionMapper questionMapper;
    private final RedisUtils redisUtils;

    @Override
    public void customPageJavaService(Page<Question> pageBean, QuestionQueryVo questionPageVo) {
        //1.分页查询题目列表（多条件）
        LambdaQueryWrapper<Question> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getType()),Question::getType,questionPageVo.getType());
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getDifficulty()),Question::getDifficulty,questionPageVo.getDifficulty());
        lambdaQueryWrapper.eq(!ObjectUtils.isEmpty(questionPageVo.getCategoryId()),Question::getCategoryId,questionPageVo.getCategoryId());
        lambdaQueryWrapper.like(!ObjectUtils.isEmpty(questionPageVo.getKeyword()),Question::getTitle,questionPageVo.getKeyword());
        //时间的倒序排序！！
        lambdaQueryWrapper.orderByDesc(Question::getCreateTime);
        page(pageBean,lambdaQueryWrapper);
        //2.提取一个方法， 给题目进行选项和答案装填（热门题目也需要所以提取方法）
        fillQuestionChoiceAndAnswer(pageBean.getRecords());
    }

    @Override
    public Question customDetailQuestion(Long id) {
        //1.查询题目详情
        Question question = questionMapper.customGetById(id);
        if (question == null){
            throw  new RuntimeException("题目查询详情失败！原因可能提前被删除！题目id为：" + id);
        }
        //2.进行热点题目缓存
        new Thread(() -> {
            incrementQuestion(question.getId());
        }).start();
        return question;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void customSaveQuestion(Question question) {
        //1.一定插入题目信息 （回显题目id）
        //同一个类型不能题目title相同
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getType,question.getType());
        queryWrapper.eq(Question::getTitle,question.getTitle());
        //自己的业务或者自己的mapper: getBaseMapper() baseMapper
        boolean exists = baseMapper.exists(queryWrapper);
        if (exists) {
            //同一类型，title相同
            throw new RuntimeException("在%s下，存在%s 名称的题目已经存在！保存失败！".formatted(question.getType(),question.getTitle()));
        }

        boolean saved = save(question);
        if (!saved){
            //同一类型，title相同
            throw new RuntimeException("在%s下，存在%s 名称的题目！保存失败！".formatted(question.getType(),question.getTitle()));
        }
        //2.获取答案对象，并先配置题目id
        QuestionAnswer answer = question.getAnswer();
        answer.setQuestionId(question.getId());
        //3.判断是不是选择题
        if ("CHOICE".equals(question.getType())){
            //是 -》 循环 -》 选项 + 题目id -> 保存 -》 判断是不是正确 进行 AD
            List<QuestionChoice> choices = question.getChoices();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < choices.size(); i++) {
                //给每个选项匹配questionId
                // [0 [1] 2 [3] ]
                QuestionChoice choice = choices.get(i);
                //确保，正确顺序！ 否则默认是0 随机了
                choice.setSort(i);
                choice.setQuestionId(question.getId());
                questionChoiceMapper.insert(choice);
                if (choice.getIsCorrect()){
                    //true 本次是正确答案
                    if (sb.length() > 0){
                        sb.append(",");
                    }
                    //B,D
                    sb.append((char)('A'+i));
                }
            }

            //进行答案赋值
            answer.setAnswer(sb.toString());
        }
        // 4.保存答案对象
        questionAnswerMapper.insert(answer);
        // 5.保证方法的一致性！ 需要添加事务
    }

    //定义进行题目访问次数增长的方法
//异步方法
    private void incrementQuestion(Long questionId){
        Double score = redisUtils.zIncrementScore(CacheConstants.POPULAR_QUESTIONS_KEY,questionId,1);
        log.info("完成{}题目分数累计，累计后分数为：{}",questionId,score);
    }

    private void fillQuestionChoiceAndAnswer(List<Question> questionList) {
        //1. 非空判断
        if (questionList == null || questionList.size() == 0) {
            log.debug("没有查询对应的问题集合数据！！");
            return;
        }
        //2. 查询所有答案和选项
        //优化查询本次题目的答案和选项
        //查询本地题目集合对应的id集合
        List<Long> ids = questionList.stream().map(Question::getId).collect(Collectors.toList());
        //查询本次题目的选项集合
        List<QuestionChoice> questionChoiceList =
                questionChoiceMapper.selectList(new LambdaQueryWrapper<QuestionChoice>().in(QuestionChoice::getQuestionId,ids));
        //查询本次题目的答案
        List<QuestionAnswer> questionAnswers =
                questionAnswerMapper.selectList(new LambdaQueryWrapper<QuestionAnswer>().in(QuestionAnswer::getQuestionId,ids));
        //3. 答案和选项进行map转化
        Map<Long, List<QuestionChoice>> questionChoiceMap =
                questionChoiceList.stream().collect(Collectors.groupingBy(QuestionChoice::getQuestionId));
        Map<Long, QuestionAnswer> answerMap =
                questionAnswers.stream().collect(Collectors.toMap(QuestionAnswer::getQuestionId, a -> a));
        //4. 循环问题集合，进行选项和答案配置
        questionList.forEach(question -> {
            //每个题目一定答案
            question.setAnswer(answerMap.get(question.getId()));
            //选择题才有选项
            if ("CHOICE".equals(question.getType())){
                List<QuestionChoice> questionChoices = questionChoiceMap.get(question.getId());
                questionChoices.sort(Comparator.comparingInt(QuestionChoice::getSort));
                question.setChoices(questionChoices);
            }
        });
    }
}