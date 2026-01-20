package com.wfql.springbootdemo.fanuc;

import lombok.Getter;
import lombok.Setter;

/**
 * 程序信息结果类
 * 用于封装从CNC读取的单个程序信息
 */
@Getter
@Setter
public class ProgramInfoResult {
    private boolean success;
    private int programNumber;   // 程序号
    private int programLength;   // 程序长度（字符数）
    private String comment;      // 程序注释
    private short errorCode;
    private String errorMessage;

    public ProgramInfoResult() {
        this.success = false;
        this.programNumber = -1;
        this.programLength = -1;
        this.comment = "";
        this.errorCode = -1;
        this.errorMessage = "";
    }

    @Override
    public String toString() {
        if (success) {
            return "ProgramInfoResult{success=true, programNumber=" + programNumber +
                    ", programLength=" + programLength +
                    ", comment='" + comment + "'}";
        } else {
            return "ProgramInfoResult{success=false, errorCode=" + errorCode +
                    ", errorMessage='" + errorMessage + "'}";
        }
    }
}