package com.xiapi.xiaaiagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {
        // 为了便于大家开发调试和部署，取消数据库自动配置，需要使用 PgVector 时把 DataSourceAutoConfiguration.class 删除
        //DataSourceAutoConfiguration 是 Spring Boot 的自动配置类，它的作用是：
        //自动根据 application.yml 中的 spring.datasource 配置，创建并注册一个 DataSource Bean（数据库连接池）DataSource 、 JdbcTemplate 等 Bean
        DataSourceAutoConfiguration.class
})
public class XiaAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaAiAgentApplication.class, args);
    }

}
