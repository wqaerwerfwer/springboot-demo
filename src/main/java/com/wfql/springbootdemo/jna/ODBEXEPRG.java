package com.wfql.springbootdemo.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class ODBEXEPRG extends Structure {

    public byte[] name = new byte[36];
    public int o_num;

    public ODBEXEPRG() {
        super();
    }

    public ODBEXEPRG(com.sun.jna.Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("name", "o_num");
    }

}