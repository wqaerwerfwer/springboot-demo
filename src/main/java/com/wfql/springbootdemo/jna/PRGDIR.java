package com.wfql.springbootdemo.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public  class PRGDIR extends Structure {
    public short number;           // 程序号
    public short length;           // 程序长度
    public byte[] comment = new byte[51]; // 程序注释，最多50字节+null终止符

    public PRGDIR() {
        super();
    }

    public PRGDIR(com.sun.jna.Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("number", "length", "comment");
    }
}