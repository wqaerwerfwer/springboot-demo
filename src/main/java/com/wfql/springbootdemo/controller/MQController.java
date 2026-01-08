/*
package com.wfql.springbootdemo.controller;

import cn.hutool.json.JSONUtil;
import com.wfql.springbootdemo.common.ResponseResult;
import com.wfql.springbootdemo.mq.MyMessageProducer;
import com.wfql.springbootdemo.mq.Student;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.transform.Source;

*/
/**
 * @Package com.wfql.springbootdemo.controller
 * @Author guoqing.ling
 * @Date 2025/12/24 11:18
 *//*

@Slf4j
@RestController
@RequestMapping("/mq")
@RequiredArgsConstructor
public class MQController {

    private final MyMessageProducer myMessageProducer;

    @GetMapping("/send")
    public ResponseResult<String> send() {
        Student student = new Student();
        student.setName("test");
        student.setAge(21);
        student.setSex("1");
        String jsonStr = JSONUtil.toJsonStr(student);
        myMessageProducer.sendMessage("code_exchange", "my_routingKey", jsonStr);
        return ResponseResult.success();
    }

}
*/
