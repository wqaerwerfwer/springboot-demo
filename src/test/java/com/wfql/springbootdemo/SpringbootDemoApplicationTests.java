package com.wfql.springbootdemo;

import io.netty.util.internal.NativeLibraryLoader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;

@SpringBootTest
class SpringbootDemoApplicationTests {

    @Test
    void contextLoads() {
//        System.loadLibrary("Fwlib64"); // 不需要.dll后缀
        InputStream in = NativeLibraryLoader.class.getClassLoader().getResourceAsStream("/Fwlib64.dll");

    }

}
