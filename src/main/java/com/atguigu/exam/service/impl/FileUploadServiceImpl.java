package com.atguigu.exam.service.impl;

import com.atguigu.exam.config.MinioConfig;
import com.atguigu.exam.config.properties.MinioProperties;
import com.atguigu.exam.service.FileUploadService;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * projectName: com.atguigu.exam.service.impl
 *
 * @author: 赵伟风
 * description:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    private final MinioProperties minioProperties;
    private final MinioClient minioClient;

    @SneakyThrows
    @Override
    public String upload(String folder, MultipartFile file) throws IOException {
        //1. 连接 minio 客户端
        //2. 判断桶是否存在 不存在则创建并设置访问权限
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
        if (!bucketExists) {
            String config = """
                        {
                              "Statement" : [ {
                                "Action" : "s3:GetObject",
                                "Effect" : "Allow",
                                "Principal" : "*",
                                "Resource" : "arn:aws:s3:::%s/*"
                              } ],
                              "Version" : "2012-10-17"
                        }
                    """.formatted(minioProperties.getBucketName());
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .config(config)
                    .build());
        }
        //防止重复 使用uuid 命名文件
        //文件夹按照日期分割
        String objectName = folder + "/" + new SimpleDateFormat("yyyy/MM/dd").format(new Date())
                + "/" + UUID.randomUUID().toString().replaceAll("-", "") + "_"  + file.getOriginalFilename();
        log.debug("文件上传核心业务方法，处理后的文件对象名：{}",objectName);
        //3. 上传文件
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucketName())
                .contentType(file.getContentType())
                .object(objectName)
                //stream 上穿文件的输入流数据 1.上传文件的输入留  2.上传文件的大小 3.是否切割文件转化为多线程上传 -1智能切割 交由minio去判断
                .stream(file.getInputStream(), file.getSize(), -1)
                .build());
        //4. 拼接返回地址
        String url = String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), objectName);
        log.info("文件上传成功，访问地址为：{}", url);
        return url;
    }
}
