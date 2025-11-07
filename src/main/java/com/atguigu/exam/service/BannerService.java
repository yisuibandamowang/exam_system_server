package com.atguigu.exam.service;

import com.atguigu.exam.entity.Banner;
import com.baomidou.mybatisplus.extension.service.IService;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.XmlParserException;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 轮播图服务接口
 */
public interface BannerService extends IService<Banner> {
    /**
     * 轮播图服务接口
     */

    /**
     * 上传图片页面
     *
     * @param file
     * @return 返回图片
     */
    String uploadImage(MultipartFile file) throws Exception;
}