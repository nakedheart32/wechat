package com.abc.wechat.conroller;

import com.abc.wechat.service.ChatRecordsService;
import com.abc.wechat.utils.ImageRevert;
import com.abc.wechat.utils.ResultMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MainController {
    @Autowired
    private ChatRecordsService chatRecordsService;
    @Autowired
    private ImageRevert imageRevertUtil;
    @Value("${wechat.communet.notify.url}")
    private String url;


    //上传新增聊天记录
    @GetMapping("/upload")
    //@Scheduled(cron = "30 16 * * * *")
    public ResultMsg uploadToMongo(){
        List<Integer> res =  chatRecordsService.upload();
        return responseResultMsg(200, "success", res.get(0) + " new text messages uploaded");
    }



    //每天0:0reupload失敗的消息到Mongo
    //@Scheduled(cron = "0 0 0 * * ? *")
    public ResultMsg reuploadToMongo(){
        List<Integer> res = chatRecordsService.reupload();
        return responseResultMsg(200, "success", res.get(0) + " new text messages reuploaded");
    }

    private ResultMsg responseResultMsg(int code, String msg, Object data){
        ResultMsg resultMsg = new ResultMsg();
        resultMsg.setCode(code);
        resultMsg.setMsg(msg);
        resultMsg.setData(data);
        return resultMsg;
    }



}
