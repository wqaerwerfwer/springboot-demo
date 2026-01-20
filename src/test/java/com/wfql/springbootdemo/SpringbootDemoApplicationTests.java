package com.wfql.springbootdemo;

import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class SpringbootDemoApplicationTests {


    private static void run() {
        while (true) {
            String body = HttpRequest.get("http://127.0.0.1:2635/fanuc/conect").execute().body();
            log.debug("{}", body);
        }
    }

    @Test
    void contextLoads() {
        Thread thread = new Thread(SpringbootDemoApplicationTests::run);
        thread.start();
    }


}
