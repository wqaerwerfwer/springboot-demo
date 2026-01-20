package com.wfql.springbootdemo.jna;

import com.sun.jna.Structure;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

/**
 * 对应C语言中的PRGDIR4结构体
 */
@Setter
@Getter
public class PRGDIR4 extends Structure {
    public int number;           // 程序号
    public int length;           // 程序长度（字符数）
    public byte[] comment = new byte[48]; // 程序注释，最多47字节+null终止符
    public byte[] mdate = new byte[16];   // 修改日期，最多15字节+null终止符
    public byte[] cdate = new byte[16];   // 创建日期，最多15字节+null终止符

    public PRGDIR4() {
        super();
    }

    public PRGDIR4(com.sun.jna.Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("number", "length", "comment", "mdate", "cdate");
    }
}