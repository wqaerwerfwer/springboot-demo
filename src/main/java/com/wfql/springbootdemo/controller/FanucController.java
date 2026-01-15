package com.wfql.springbootdemo.controller;

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
    public void  conect() {
        fanucReadDevices.readDevice();
    }
}
