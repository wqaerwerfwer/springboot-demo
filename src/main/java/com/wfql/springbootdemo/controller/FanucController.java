package com.wfql.springbootdemo.controller;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import com.wfql.springbootdemo.fanuc.FanucReadDevices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Package com.wfql.springbootdemo.controller
 * @Author guoqing.ling
 * @Date 2026/1/8 13:20
 */
@Slf4j
@RestController
@RequestMapping("/fanuc")
@RequiredArgsConstructor
public class FanucController {

    private final FanucReadDevices fanucReadDevices;

    @GetMapping("/conect")
    public String conect() {
        fanucReadDevices.readDevice();
        return "ok";
    }


    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            while (true) {
                System.out.println("-------------");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    String body = HttpRequest.get("http://127.0.0.1:2635/fanuc/conect").timeout(3000).execute().body();
                    System.out.println( body);
                } catch (HttpException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

}
