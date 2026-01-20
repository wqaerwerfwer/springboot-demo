package com.wfql.springbootdemo.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class ODBM extends Structure {

    public short datano;
    public short dummy;
    public int mcr_val;
    public short dec_val;

    public ODBM() {
        super();
    }

    public ODBM(com.sun.jna.Pointer p) {
        super(p);
        read();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("datano", "dummy", "mcr_val", "dec_val");
    }

}