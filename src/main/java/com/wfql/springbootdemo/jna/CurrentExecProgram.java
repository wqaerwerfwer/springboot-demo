package com.wfql.springbootdemo.jna;

import java.nio.charset.StandardCharsets;

/**
 * @Package com.wfql.fanuc.jna
 * @Author guoqing.ling
 * @Date 2026/1/8 14:31
 */
public class CurrentExecProgram {

    public static ProgramNameInfo readCurrentExecProgramWithError( DLibraryService dLibraryService,short handle) {
        ODBEXEPRG exeprg = null;
        try {
            // 创建 ODBEXEPRG 结构体实例
            exeprg = new ODBEXEPRG();

            // 调用 DLL 函数
            short result = dLibraryService.cnc_exeprgname(handle, exeprg);

            ProgramNameInfo info = new ProgramNameInfo();
            info.setErrorCode(result);

            if (result == 0) {
                // 成功：读取结构体数据
                exeprg.read();

                // 解析程序名称：从字节数组转换为字符串，查找NULL终止符
                int len = 0;
                for (int i = 0; i < exeprg.name.length; i++) {
                    if (exeprg.name[i] == 0) {
                        break;
                    }
                    len++;
                }
                String programName = new String(exeprg.name, 0, len, StandardCharsets.UTF_8);

                info.setProgramName(programName);
                info.setProgramNumber(exeprg.o_num);
                info.setSuccess(true);
            } else {
                // 失败
                info.setSuccess(false);
                info.setErrorMessage("读取当前执行程序名称失败，错误代码: " + result);
            }

            return info;
        } catch (Exception e) {
            System.err.println("读取当前执行程序名称时发生异常: " + e.getMessage());
            e.printStackTrace();
            ProgramNameInfo info = new ProgramNameInfo();
            info.setSuccess(false);
            info.setErrorMessage("读取当前执行程序名称时发生异常: " + e.getMessage());
            return info;
        } finally {
            // 确保清理内存
            if (exeprg != null) {
                try {
                    exeprg.clear();
                } catch (Exception e) {
                    // 忽略清理时的异常
                }
            }
        }
    }

}
