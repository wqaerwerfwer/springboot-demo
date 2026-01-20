package com.wfql.springbootdemo.jna;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于读取Fanuc CNC程序目录的工具类
 */
public class ReadProgramDirectory {

    /**
     * 读取程序目录的主方法（使用cnc_rdprogdir3函数）
     *
     * @param dLibraryService DLibraryService实例
     * @param handle          连接句柄
     * @return 包含程序信息的PrgrmDirInfo对象
     */
    public static PrgrmDirInfo readProgramDirectoryWithError(DLibraryService dLibraryService, short handle) {
        PrgrmDirInfo result = new PrgrmDirInfo();

        try {
            // 设置参数：读取所有程序，类型为1（包含程序号和注释）
            short type = 0;        // 读取程序号和注释
            int maxPrograms = 1000;  // 最大程序数量
            
            // 创建输入/输出参数
            IntByReference topProg = new IntByReference(1);  // 起始程序号
            ShortByReference numProg = new ShortByReference((short)maxPrograms);  // 程序数量
            
            // 分配内存给PRGDIR3数组
            int structSize = Native.getNativeSize(PRGDIR3.class, null);
            Memory buffer = new Memory(structSize * maxPrograms);
            PRGDIR3[] prgDirArray = new PRGDIR3[maxPrograms];
            
            // 初始化数组元素
            for (int i = 0; i < maxPrograms; i++) {
                prgDirArray[i] = new PRGDIR3(buffer.share(i * structSize));
            }
            
            // 调用FOCAS函数读取程序目录
            short ret = dLibraryService.cnc_rdprogdir3(handle, type, topProg, numProg, prgDirArray[0]);

            result.setErrorCode(ret);

            if (ret == 0) { // 成功
                result.setSuccess(true);

                // 解析返回的数据
                int actualNum = numProg.getValue();
                List<PrgrmInfo> programs = parseProgramDirectoryArray(prgDirArray, actualNum);
                result.setPrograms(programs);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("读取程序目录失败，错误代码: " + ret);
            }

            return result;
        } catch (Exception e) {
            System.err.println("读取程序目录时发生异常: " + e.getMessage());
            e.printStackTrace();
            
            result.setSuccess(false);
            result.setErrorMessage("读取程序目录时发生异常: " + e.getMessage());
            return result;
        }
    }

    /**
     * 解析PRGDIR3数组中的程序目录数据
     *
     * @param prgDirArray PRGDIR3数组实例
     * @param count       需要解析的元素数量
     * @return 解析后的程序信息列表
     */
    private static List<PrgrmInfo> parseProgramDirectoryArray(PRGDIR3[] prgDirArray, int count) {
        List<PrgrmInfo> programs = new ArrayList<>();

        try {
            for (int i = 0; i < Math.min(count, prgDirArray.length); i++) {
                PRGDIR3 entry = prgDirArray[i];
                
                // 将字节数组转换为字符串（处理注释）
                String comment = parseStringFromBytes(entry.comment);
                
                // 检查程序号是否有效
                if (entry.number > 0) {
                    programs.add(new PrgrmInfo(entry.number, entry.length, comment));
                }
            }
        } catch (Exception e) {
            System.err.println("解析程序目录数据时发生异常: " + e.getMessage());
            e.printStackTrace();
        }

        return programs;
    }

    /**
     * 从字节数组解析字符串（处理C风格字符串的NULL终止符）
     *
     * @param bytes 字节数组
     * @return 解析出的字符串
     */
    private static String parseStringFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        int len = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                break;
            }
            len++;
        }

        if (len == 0) {
            return "";
        }

        return new String(bytes, 0, len, StandardCharsets.UTF_8).trim();
    }
}