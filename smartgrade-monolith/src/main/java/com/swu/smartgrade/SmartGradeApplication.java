package com.swu.smartgrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智批作业通 · 单体版 Demo 启动类。
 *
 * <p>第 1 周交付：用「单体应用」跑通最小闭环——教师发布作业 → 学生提交答案 → 自动判分
 * → 评分结果 / 错题本 / 班级看板 / 通知。应用内部按未来微服务边界分包，
 * 第 3 周起可把 user / bank / assignment / grading / result / wrong / notice / report
 * 这些包逐个拆成独立服务。</p>
 */
@SpringBootApplication
public class SmartGradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartGradeApplication.class, args);
    }
}
