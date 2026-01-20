package com.wfql.springbootdemo.jna;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * @Package com.wfql.fanuc.service
 * @Author guoqing.ling
 * @Date 2026/1/8 14:05
 */
public interface DLibraryService extends StdCallLibrary {

    final String DLL_PATH = "Fwlib64.dll";

    public static final DLibraryService INSTANCE = (DLibraryService) Native.loadLibrary(DLL_PATH, DLibraryService.class);

    /**
     * 获取连接句柄
     *
     * @param ipaddr   IP地址
     * @param port     端口号
     * @param timeout  超时时间
     * @param FlibHndl 连接句柄
     */
    short cnc_allclibhndl3(String ipaddr, short port, int timeout, ShortByReference FlibHndl);

    /**
     * 读取程序
     *
     * @param FlibHndl 连接句柄
     * @param length   程序长度
     * @param blknum   程序块号
     * @param data     程序数据
     */
    short cnc_rdexecprog(short FlibHndl, ShortByReference length, ShortByReference blknum, Memory data);

    /**
     * 执行程序
     *
     * @param FlibHndl 连接句柄
     * @param exeprg   程序名
     */
    short cnc_exeprgname(short FlibHndl, ODBEXEPRG exeprg);

    /**
     * 读取宏变量
     *
     * @param FlibHndl 连接句柄
     * @param number   变量号
     * @param length   变量长度
     * @param macro    变量值
     */
    short cnc_rdmacro(short FlibHndl, short number, short length, ODBM macro);

    short cnc_alarm2(short FlibHndl, IntByReference alarm);

    /**
     * 释放连接句柄
     *
     * @param FlibHndl 连接句柄
     */
    void cnc_freelibhndl(short FlibHndl);


    /**
     * 读取程序目录
     *
     * @param FlibHndl 连接句柄
     * @param type     信息类型: 0-程序号, 1-程序号和注释, 2-程序号、注释、日期和大小
     * @param datano_s 起始程序号
     * @param datano_e 结束程序号
     * @param length   缓冲区长度
     * @param prgdir   程序目录结构
     */
    short cnc_rdprogdir(short FlibHndl, short type, short datano_s, short datano_e, short length, PRGDIR prgdir);


    /**
     * 读取程序目录 (使用数组形式)
     *
     * @param FlibHndl 库句柄
     * @param type     信息类型: 0-程序号, 1-程序号和注释, 2-程序号、注释、日期和大小
     * @param datano_s 起始程序号
     * @param datano_e 结束程序号
     * @param length   缓冲区长度
     * @param prgdir   程序目录结构数组
     */
    short cnc_rdprogdir_fms(short FlibHndl, short type, short datano_s, short datano_e, short length, PRGDIR prgdir);


    /**
     * 读取程序目录版本3 (改进版)
     *
     * @param FlibHndl  库句柄
     * @param type      信息类型: 0-程序号, 1-程序号和注释, 2-程序号、注释、日期和大小
     * @param top_prog  起始程序号指针
     * @param num_prog  程序数量指针
     * @param buf       PRGDIR3结构数组
     */
    short cnc_rdprogdir3(short FlibHndl, short type, IntByReference top_prog, ShortByReference num_prog, PRGDIR3 buf);


    /**
     * 读取程序目录版本4 (最新版)
     *
     * @param FlibHndl  库句柄
     * @param type      信息类型: 0-程序号, 1-程序号和注释, 2-程序号、注释、日期和大小
     * @param top_number 起始程序号
     * @param num_prog  程序数量指针
     * @param buf       PRGDIR4结构数组
     */
    short cnc_rdprogdir4(short FlibHndl, short type, int top_number, ShortByReference num_prog, PRGDIR4 buf);

    /**
     * 读取程序信息
     * 用于读取指定程序的详细信息，如程序号、长度、注释等
     *
     * @param FlibHndl  库句柄
     * @param prog_no   程序号
     * @param info_type 信息类型：0-程序信息, 1-程序信息(带注释)
     * @param dbuf      存储程序信息的结构体
     * @return 错误代码，0表示成功
     */
    short cnc_rdproginfo(short FlibHndl, int prog_no, short info_type, ODBINFO dbuf);


}