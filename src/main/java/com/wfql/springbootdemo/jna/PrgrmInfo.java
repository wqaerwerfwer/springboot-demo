package com.wfql.springbootdemo.jna;

import lombok.Getter;
import lombok.Setter;

/**
 * @Package com.wfql.springbootdemo.jna
 * @Author guoqing.ling
 * @Date 2026/1/20 10:59
 */
@Setter
@Getter
public class PrgrmInfo {
    // Getter and Setter methods
    private int number;      // 程序号
    private int length;      // 程序大小（字符数）
    private String comment;  // 程序注释

    public PrgrmInfo(int number, int length, String comment) {
        this.number = number;
        this.length = length;
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "PrgrmInfo{number=" + number + ", length=" + length + ", comment='" + comment + "'}";
    }
}
