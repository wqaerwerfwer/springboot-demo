package com.wfql.springbootdemo.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

/**
 * ODBINFO 结构体 - 程序信息数据结构
 * 用于存储从CNC读取的程序信息，包括程序号、长度、注释等
 * 
 * typedef struct odbinfo {
 *     long number;     // 程序号
 *     long length;     // 程序长度
 *     char comment[52]; // 程序注释
 * } ODBINFO;
 */
@Getter
@Setter
public class ODBINFO extends Structure {
    public int number;           // 程序号
    public int length;           // 程序长度（字符数）
    public byte[] comment = new byte[52]; // 程序注释（最多51个字符 + NULL终止符）

    public ODBINFO() {
        super();
    }

    public ODBINFO(Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("number", "length", "comment");
    }
}