package com.wfql.springbootdemo.jna;

/**
 * @Package com.wfql.fanuc.jna
 * @Author guoqing.ling
 * @Date 2026/1/8 14:28
 */
public class MacroVariable {

    public static MacroVariableInfo readMacroVariableWithError( DLibraryService dLibraryService,short handle, short number, short length) {
        ODBM macro = null;
        try {
            // 创建 ODBM 结构体实例
            macro = new ODBM();

            // 调用 DLL 函数
            short result = dLibraryService.cnc_rdmacro(handle, number, length, macro);

            MacroVariableInfo info = new MacroVariableInfo();
            info.setErrorCode(result);

            if (result == 0) {
                // 成功：读取结构体数据
                macro.read();

                info.setVariableNumber(macro.datano & 0xFFFF); // 转换为无符号整数
                info.setValue(macro.mcr_val);
                info.setDecimalPlaces(macro.dec_val);
                info.setSuccess(true);
            } else {
                // 失败
                info.setSuccess(false);
                info.setErrorMessage("读取宏变量失败，错误代码: " + result);
            }

            return info;
        } catch (Exception e) {
            System.err.println("读取宏变量时发生异常: " + e.getMessage());
            e.printStackTrace();
            MacroVariableInfo info = new MacroVariableInfo();
            info.setSuccess(false);
            info.setErrorMessage("读取宏变量时发生异常: " + e.getMessage());
            return info;
        } finally {
            // 确保清理内存
            if (macro != null) {
                try {
                    macro.clear();
                } catch (Exception e) {
                    // 忽略清理时的异常
                }
            }
        }
    }

}
