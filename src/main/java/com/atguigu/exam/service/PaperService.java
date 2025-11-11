package com.atguigu.exam.service;

import com.atguigu.exam.entity.Paper;
import com.atguigu.exam.vo.PaperVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {

    /**
     * 根据试卷id试卷详情
     *    试卷对象
     *    题目集合
     *    注意： 题目的选项sort正序
     *    注意： 所有题目根据类型排序
     * @param id 试卷id
     * @return
     */
    Paper customPaperDetailById(Long id);

    /**
     * 手动组卷
     * @param paperVo
     * @return
     */
    Paper customCreatePaper(PaperVo paperVo);
}