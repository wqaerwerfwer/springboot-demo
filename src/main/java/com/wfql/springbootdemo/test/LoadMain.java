/*
package com.wfql.springbootdemo.test;

import com.sun.jna.Native;
import com.sun.jna.ptr.ShortByReference;
import com.sun.jna.win32.StdCallLibrary;

*/
/**
 * @Package com.wfql.springbootdemo.test
 * @Author guoqing.ling
 * @Date 2026/1/6 16:31
 *//*

public class LoadMain {


    
    public interface DLibrary extends StdCallLibrary {
        //此处我的jdk版本为64位,故加载64位的Dll
        // WINAPI 使用 stdcall 调用约定，所以使用 StdCallLibrary
        
        // 使用类级别的 DLL_PATH 常量
        DLibrary INSTANCE = (DLibrary) Native.loadLibrary("Fwlib64.dll", DLibrary.class);
        
        */
/**
         * 分配库句柄并连接到指定IP地址或主机名的CNC
         * @param ipaddr CNC的IP地址或主机名字符串 (例如: "192.168.0.1" 或 "CNC-1.FACTORY")
         * @param port FOCAS1/Ethernet 或 FOCAS2/Ethernet (TCP) 功能的端口号 (unsigned short)
         * @param timeout 超时秒数。如果指定0，则忽略超时处理，库函数将无限等待 (long, Windows上为32位)
         * @param FlibHndl 输出参数，用于存储库句柄的指针 (unsigned short *)
         * @return 返回状态码，0表示成功，非0表示失败
         *//*

        short cnc_allclibhndl3(String ipaddr, short port, int timeout, ShortByReference FlibHndl);
    }


//    public native short cnc_allclibhndl3(String ipaddr, short port, int timeout, short[] FlibHndl);

    public static void main(String[] args) {
        try {
            // 创建句柄引用对象，用于接收输出参数
            ShortByReference handle = new ShortByReference();
            
            // 调用本地方法
            // port 使用 short 类型，范围 0-65535
            // timeout 建议使用较大的值，如 10-60 秒
            short result = DLibrary.INSTANCE.cnc_allclibhndl3("10.1.13.152", (short) 8193, 10, handle);
            
            System.out.println("返回结果: " + result);
            
            if (result == 0) {
                System.out.println("连接成功，句柄: " + handle.getValue());
            } else {
                System.out.println("连接失败，错误代码: " + result);
                // 常见错误代码说明
                switch (result) {
                    case -15:
                        System.out.println("错误说明: EW_NODLL - 缺少与指定CNC系列对应的DLL文件");
                        System.out.println("可能原因: DLL路径不正确或缺少依赖的DLL文件");
                        break;
                    case -16:
                        System.out.println("错误说明: EW_HANDLE - 句柄错误");
                        break;
                    case -17:
                        System.out.println("错误说明: EW_VERSION - 版本不匹配");
                        break;
                    default:
                        System.out.println("请参考 FOCAS 库文档查看错误代码含义");
                }
            }
        } catch (Exception e) {
            System.err.println("调用异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
*/
