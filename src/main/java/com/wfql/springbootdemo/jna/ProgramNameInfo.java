package com.wfql.springbootdemo.jna;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProgramNameInfo {

    private boolean success;
    private String programName;
    private int programNumber;
    private short errorCode;
    private String errorMessage;

    @Override
    public String toString() {
        if (success) {
            return "ProgramNameInfo{success=true, programName='" + programName + "', " +
                    "programNumber=" + programNumber + "}";
        } else {
            return "ProgramNameInfo{success=false, errorCode=" + errorCode +
                    ", errorMessage='" + errorMessage + "'}";
        }
    }

}