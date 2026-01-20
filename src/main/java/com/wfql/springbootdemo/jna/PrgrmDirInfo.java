package com.wfql.springbootdemo.jna;

import com.sun.jna.Structure;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Setter
@Getter
public class PrgrmDirInfo {
    private boolean success;
    private short errorCode;
    private String errorMessage;
    private List<PrgrmInfo> programs;

    public PrgrmDirInfo() {
        this.programs = new java.util.ArrayList<>();
    }

    @Override
    public String toString() {
        if (success) {
            return "PrgrmDirInfo{success=true, programCount=" + programs.size() + "}";
        } else {
            return "PrgrmDirInfo{success=false, errorCode=" + errorCode +
                    ", errorMessage='" + errorMessage + "'}";
        }
    }
}

