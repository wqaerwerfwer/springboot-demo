package com.wfql.springbootdemo.jna;

import com.sun.jna.ptr.IntByReference;

/**
 * @Package com.wfql.fanuc.jna
 * @Author guoqing.ling
 * @Date 2026/1/8 15:15
 */
public class AlarmStatus {

    public static AlarmStatusInfo readAlarmStatusWithError(DLibraryService dLibraryService, short handle) {
        IntByReference alarmRef = new IntByReference();

        // 调用 DLL 函数
        short result = dLibraryService.cnc_alarm2(handle, alarmRef);

        AlarmStatusInfo info = new AlarmStatusInfo();
        info.setErrorCode(result);

        if (result == 0) {
            // 成功：获取报警状态
            int alarmStatus = alarmRef.getValue();

            info.setAlarmStatus(alarmStatus);
            info.setSuccess(true);
        } else {
            // 失败
            info.setSuccess(false);
            info.setErrorMessage("读取报警状态失败，错误代码: " + result);
        }

        return info;
    }

}
