package com.atguigu.exam.service;


import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文件上传服务
 * 支持MinIO和本地文件存储两种方式
 */

public interface FileUploadService {
    /**
     * 上传文件
     *
     * @param folder 文件夹名称 (轮播图 banners 视频 videos)
     * @param file   文件 具体的文件对象
     * @return 文件访问URL
     */
    String upload(String folder,MultipartFile file) throws IOException;
} 