package com.wfql.springbootdemo.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * mybatisplus 配置类
 *
 * @Author lingguoqing
 * @PROJECT_NAME: project-backend
 * @CLASS_NAME: MybatisplusConfig
 * @PACKAGE_NAME: com.ling.projectbackend.config
 * @Date 2025/2/14 13:50
 * @Version 1.0
 */
@Slf4j
@Configuration
@MapperScan(basePackages = {"com.wfql.springbootdemo.mapper",})
public class MybatisplusConfig implements MetaObjectHandler {

    /**
     * 添加分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    /**
     * 自动插入通用数据
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatetime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createtime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "datatime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "addtime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 自动更新通用数据
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updatetime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "datatime", LocalDateTime.class, LocalDateTime.now());
    }

}
