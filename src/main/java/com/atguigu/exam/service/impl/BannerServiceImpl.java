package com.atguigu.exam.service.impl;

import com.atguigu.exam.entity.Banner;
import com.atguigu.exam.mapper.BannerMapper;
import com.atguigu.exam.service.BannerService;

import com.atguigu.exam.service.FileUploadService;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


/**
 * 轮播图服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {
    private final FileUploadService fileUploadService;
    /**
     *
     * 实现逻辑：
     *   核心校验 【1.文件非空校验 2.格式校验需要是image 3. 文件大小限制】
     *   文件上传
     * 上传图片页面
     * @param file 上传的文件
     * @return 返回图片
     */
    @Override
    public String uploadImage(MultipartFile file) throws Exception {
        //1. 非空校验
        if (file.isEmpty()) {
            //配合全局异常处理，快速返回失败结果！！
            throw new RuntimeException("请选择要上传的文件！");
        }
        //2. 图片格式校验
        //获取文件的mimetype类型
        String contentType = file.getContentType();
        if (ObjectUtils.isEmpty(contentType) || !contentType.startsWith("image")) {
            //配合全局异常处理，快速返回失败结果！！
            throw new RuntimeException("轮播图只能上传图片文件！");
        }
        //3. 文件大小限制
        if (file.getSize() > 5 * 1024 * 1024) {
            //配合全局异常处理，快速返回失败结果！！
            throw new RuntimeException("图片文件大小不能超过5MB");
        }
        //4. 调用文件上传业务
        String imgUrl = fileUploadService.upload("banner",file);
        //5. 返回结果
        log.info("完成banner图片上传，图片回显地址：{}",imgUrl);
        return imgUrl;
    }

    /**
     * 完成轮播图添加
     * 插入失败抛出异常
     * @param banner 【要插入的轮播图】
     */
    @Override
    public void addBanner(Banner banner) {
        //1.确认banner createTime和updateTime有时间
        //方式1：数据库设置时间  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
        //方案2：代码时间赋值   set new Date();
        //方案3：使用mybatis-plus自动填充功能 [知识点中会说明]
        //2.判断下启动状态
        if (banner.getIsActive() == null){
            banner.setIsActive(true);
        }
        //3.判断优先级
        if (banner.getSortOrder() == null){
            banner.setSortOrder(0);
        }
        //4.进行保存
        boolean isSuccess = save(banner);

        if (!isSuccess) {
            throw new RuntimeException("轮播图保存失败！");
        }

        log.info("轮播图保存成功！！");
    }

    /**
     * 轮播图更新业务！
     * 更新错误，抛出异常
     * @param banner
     */
    @Override
    public void updateBanner(Banner banner) {
        boolean success = this.updateById(banner);
        if (!success) {
            throw new RuntimeException("轮播图更新失败");
        }
    }
}