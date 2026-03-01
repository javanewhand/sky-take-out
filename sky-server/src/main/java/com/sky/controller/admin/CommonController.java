package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.AliOSSUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Slf4j
public class  CommonController {

    @Autowired
    private AliOSSUtils ossUtil;

    @PostMapping("/upload")
    public Result<Object> upload(MultipartFile file) throws IOException {
        log.info("文件上传，文件名：{}",file.getOriginalFilename());
        //调用阿里云OSS开发
        Object url= ossUtil.upload(file);
        log.info("文件上传完成，返回URL值{}",url);
        return Result.success(url);
    }
}
