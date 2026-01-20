package com.wfql.springbootdemo.jna;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ExecProgramInfo {

    private boolean success;
    private String programData;
    private int actualLength;
    private int blockNumber;
    private short errorCode;
    private String errorMessage;

    @Override
    public String toString() {
        if (success) {
            return "ExecProgramInfo{success=true, actualLength=" + actualLength +
                    ", blockNumber=" + blockNumber + ", programData='" + programData + "'}";
        } else {
            return "ExecProgramInfo{success=false, errorCode=" + errorCode +
                    ", errorMessage='" + errorMessage + "'}";
        }
    }

}