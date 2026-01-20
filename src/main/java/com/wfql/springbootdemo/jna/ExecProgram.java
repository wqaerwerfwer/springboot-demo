package com.wfql.springbootdemo.jna;

import com.sun.jna.Memory;
import com.sun.jna.ptr.ShortByReference;

/**
 * @Package com.wfql.fanuc.jna
 * @Author guoqing.ling
 * @Date 2026/1/8 14:34
 */
public class ExecProgram {

    public  static  ExecProgramInfo readExecProgramWithError( DLibraryService dLibraryService,short handle, int maxLength) {
        // 创建输入/输出参数：length（先设置要读取的字符数）
        ShortByReference lengthRef = new ShortByReference((short) maxLength);

        // 创建输出参数：blknum（读取的块数）
        ShortByReference blknumRef = new ShortByReference();

        // 分配数据缓冲区（根据maxLength分配，需要额外空间用于NULL终止符）
        Memory dataBuffer = new Memory(maxLength + 1);

        // 调用 DLL 函数
        short result = dLibraryService.cnc_rdexecprog(handle, lengthRef, blknumRef, dataBuffer);

        ExecProgramInfo info = new ExecProgramInfo();
        info.setErrorCode(result);

        if (result == 0) {
            // 成功：获取实际读取的字符数和块数
            int actualLength = lengthRef.getValue() & 0xFFFF; // 转换为无符号整数
            int blockNumber = blknumRef.getValue();

            // 从缓冲区读取字符串（C字符串以NULL结尾）
            String programData = dataBuffer.getString(0);

            info.setProgramData(programData);
            info.setActualLength(actualLength);
            info.setBlockNumber(blockNumber);
            info.setSuccess(true);
        } else {
            // 失败
            info.setSuccess(false);
            info.setErrorMessage("读取执行程序失败，错误代码: " + result);
        }

        return info;
    }

}
