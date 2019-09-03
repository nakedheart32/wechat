package com.abc.wechat.service;


import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.GetObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author: admin
 * @date: 2019/7/19 20:07
 * @description:
 */
@Slf4j
@Service
public class OssService {

    private OSSClient ossClient;
    private static long defaultTimeSpanSeconds = 24 * 60L * 60L;
    @Value("${oss.bucket}")
    private String oss_bucket;
    @Value("${oss.extranetHost}")
    private String oss_extranetHost;
    @Value("${oss.accessKeyId}")
    private String oss_accessKeyId;
    @Value("${oss.accessKeySecret}")
    private String oss_accessKeySecret;

    private OSSClient getClient() {
        if (this.ossClient == null) {
            this.ossClient = new OSSClient(
                    oss_extranetHost,
                    oss_accessKeyId,
                    oss_accessKeySecret
            );
        }
        return this.ossClient;
    }



    private String getFileKey(String userId, String fileId) {
        if (userId == null) {
            return fileId;
        }

        return String.format("%s/%s", userId, fileId);
    }

    @Async
    private void uploadFile(String userId, String fileId, InputStream is, CannedAccessControlList acl) {
        String key = this.getFileKey(userId, fileId);
        this.getClient().putObject(oss_bucket, key, is);
        this.setAcl(userId, fileId, acl);
        log.info("success upload {}", fileId);

    }

    public void setAcl(String userId, String fileId, CannedAccessControlList acl) {
        String key = this.getFileKey(userId, fileId);
        this.getClient().setObjectAcl(oss_bucket, key, acl);
    }

    public boolean exists(String userId, String fileId) {
        return this.getClient().doesObjectExist(oss_bucket, this.getFileKey(userId, fileId));
    }

    public void uploadFile(String userId, String fileId, InputStream is) {
        if (exists(userId, fileId)) {
            return;
        }

        this.uploadFile(userId, fileId, is, CannedAccessControlList.PublicRead);
    }

    public void uploadFile(String userId, String fileId, String file, boolean overWrite) throws FileNotFoundException {
        if (!overWrite && exists(userId, fileId)) {
            return;
        }

        InputStream is = new FileInputStream(file);
        try {
            String key = this.getFileKey(userId, fileId);
            this.getClient().putObject(oss_bucket, key, is);
            this.setAcl(userId, fileId, CannedAccessControlList.PublicRead);
        } catch (Exception e) {
            log.error("Failed to upload file {}", fileId, e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    /**
     * 上传文件,私有云
     * @param fileName 云盘上的文件名称
     * @param file 文件的磁盘路径
     * @throws FileNotFoundException
     */
    public void uploadFilePrivate(String fileName, String file) throws Exception {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            OSSClient ossClient = getClient();
            ossClient.putObject(oss_bucket, fileName, inputStream);
            ossClient.setObjectAcl(oss_bucket, fileName, CannedAccessControlList.Private);
            log.info("阿里云上传文件:{},成功", fileName);
        } catch (FileNotFoundException e) {
            log.error("阿里云上传文件:{},失败", fileName);
            throw new Exception(e);
        }finally{
            IOUtils.closeQuietly(inputStream);
        }

    }

    public void uploadFile(String userId, String fileId, String file) throws FileNotFoundException {
        this.uploadFile(userId, fileId, file, false);
    }

    public boolean uploadPublicFile(String userId, String fileId, byte[] content) {
        if (exists(userId, fileId)) {
            return true;
        }

        InputStream is = new ByteArrayInputStream(content);
        try {
            this.uploadFile(userId, fileId, is);
            this.setAcl(userId, fileId, CannedAccessControlList.PublicRead);
            return true;
        } catch (Exception e) {
            log.error("Failed to upload public file {}", fileId, e);
            return false;
        }
    }

    public void uploadOnlineFile(String userId, String fileId, String url) throws IOException {
        InputStream is = null;
        try {
            URL source = new URL(url);
            is = source.openStream();
            this.uploadFile(userId, fileId, is);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public void downloadFile(String userId, String fileId, String destFile) {
        String key = this.getFileKey(userId, fileId);
        this.getClient().getObject(
                new GetObjectRequest(oss_bucket, key),
                new File(destFile)
        );
    }

    public void deleteFile(String userId, String fileId) {
        String key = this.getFileKey(userId, fileId);
        this.getClient().deleteObject(oss_bucket, key);
    }

    public String fileUrl(String userId, String fileId, long expireMillSeconds) {
        if (expireMillSeconds < 0) {
            expireMillSeconds = defaultTimeSpanSeconds * 1000L;
        }

        Date expiration = new Date(System.currentTimeMillis() + expireMillSeconds);
        String key = this.getFileKey(userId, fileId);
        URL url = this.getClient().generatePresignedUrl(oss_bucket, key, expiration);
        return url.toString();
    }

    public String fileUrl(String userId, String fileId) {
        return fileUrl(userId, fileId, -1L);
    }

    /**
     * 获取文件路径
     * @param fileName 云盘上文件名称
     * @param expireTime 过期时间
     * @param timeUnit 时间单位
     * @return
     */
    public String fileUrl(String fileName, Integer expireTime, TimeUnit timeUnit){
        Date expiration = new Date(System.currentTimeMillis() + defaultTimeSpanSeconds * 1000L);
        if (expireTime > 0) {
            Calendar calendar = Calendar.getInstance();
            Integer calendarUnit = Calendar.SECOND;
            if(TimeUnit.MINUTES.equals(timeUnit)){
                calendarUnit = Calendar.MINUTE;
            }else if(TimeUnit.HOURS.equals(timeUnit)){
                calendarUnit = Calendar.HOUR_OF_DAY;
            }else if(TimeUnit.DAYS.equals(timeUnit)){
                calendarUnit = Calendar.DAY_OF_YEAR;
            }
            calendar.add(calendarUnit, expireTime);
            expiration = calendar.getTime();
        }
        URL url = this.getClient().generatePresignedUrl(oss_bucket, fileName, expiration);
        return url.toString();
    }

    public String publicFileUrl(String userId, String fileId) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://")
                .append(oss_bucket)
                .append(".")
                .append(oss_extranetHost)
                .append("/")
                .append(this.getFileKey(userId, fileId));
        return sb.toString();
    }

}