package com.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 项目启动入口类。
 * 作用：启动 Spring 容器并加载全部组件。
 */
@SpringBootApplication
public class AgentApplication {

    /**
     * 程序主入口。
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
