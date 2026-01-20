package com.wfql.springbootdemo.jna;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AlarmStatusInfo {

    private boolean success;
    private int alarmStatus;  // 报警状态位
    private short errorCode;
    private String errorMessage;


    @Override
    public String toString() {
        if (success) {
            StringBuilder sb = new StringBuilder();
            sb.append("AlarmStatusInfo{success=true, alarmStatus=" + alarmStatus + "}");
            return sb.toString();
        } else {
            return "AlarmStatusInfo{success=false, errorCode=" + errorCode +
                    ", errorMessage='" + errorMessage + "'" +
                    '}';
        }
    }

}