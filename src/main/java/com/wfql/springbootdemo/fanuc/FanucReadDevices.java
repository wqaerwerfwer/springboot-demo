package com.wfql.springbootdemo.fanuc;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.ShortByReference;
import com.sun.jna.win32.StdCallLibrary;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.lionsoul.ip2region.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * @Package com.wfql.springbootdemo.fanuc
 * @Author guoqing.ling
 * @Date 2026/1/7 10:31
 */
@Slf4j
@Component
public class FanucReadDevices {

    public void readDevice() {
        ShortByReference handle=new ShortByReference();
        System.out.println(handle.getValue());
        System.out.println(handle);
        short result = DLibrary.INSTANCE.cnc_allclibhndl3("10.1.13.152", (short) 8193, 10, handle);
        System.out.println(handle.getValue());
        System.out.println("返回结果: " + result);
        System.out.println(readMainProgram(handle.getValue()));
        System.out.println(readCurrentExecProgramWithError(handle.getValue()));
        System.out.println(handle.getValue());
        System.out.println(readExecProgramWithError(handle.getValue(),1024));
    }

    /**
     * ODBPMCINF 结构体 - PMC 数据信息
     * 用于存储 PMC 地址属性、有效范围等信息
     * 注意：根据 FOCAS 库文档，结构体大小可能因版本而异
     */
    public static class ODBPMCINF extends Structure {
        public short datano_s;  // 起始 PMC 地址
        public short datano_e;  // 结束 PMC 地址
        public short unit;      // 数据单位
        public short size;      // 数据大小

        // 构造函数：确保内存正确分配
        public ODBPMCINF() {
            super();
            // 使用默认对齐方式（按字段大小对齐）
        }

        // 使用 Pointer 构造（如果需要）
        public ODBPMCINF(com.sun.jna.Pointer p) {
            super(p);
            read();
        }

        protected List<String> getFieldOrder() {
            return Arrays.asList("datano_s", "datano_e", "unit", "size");
        }
    }

    /**
     * ODBDNCDGN 结构体 - DNC 下载诊断数据
     * <p>
     * typedef struct odbdncdgn {
     * short          ctrl_word;
     * short          can_word;
     * char           nc_file[16];
     * unsigned short read_ptr;
     * unsigned short write_ptr;
     * unsigned short empty_cnt;
     * unsigned long  total_size;
     * } ODBDNCDGN;
     */
    public static class ODBDNCDGN extends Structure {
        public short ctrl_word;          // 控制字
        public short can_word;           // 取消字
        public byte[] nc_file = new byte[16]; // 程序号/程序名（最多15字符，最后一个为 NULL）
        public short read_ptr;           // 读指针 (unsigned short)
        public short write_ptr;          // 写指针 (unsigned short)
        public short empty_cnt;          // 读指针追上写指针的次数 (unsigned short)
        public int total_size;           // 已输出的字符总数 (unsigned long, 32位)

        public ODBDNCDGN() {
            super();
        }

        public ODBDNCDGN(com.sun.jna.Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "ctrl_word",
                    "can_word",
                    "nc_file",
                    "read_ptr",
                    "write_ptr",
                    "empty_cnt",
                    "total_size"
            );
        }
    }

    /**
     * REALPRM 结构体 - 实参数参数
     * 用于存储实数参数的值和小数位数
     * typedef struct realprm {
     * long prm_val;             // value of variable
     * long dec_val;             // number of places of decimals
     * } REALPRM;
     */
    public static class REALPRM extends Structure {
        public int prm_val;    // 变量值
        public int dec_val;    // 小数位数

        public REALPRM() {
            super();
        }

        public REALPRM(com.sun.jna.Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("prm_val", "dec_val");
        }
    }

    /**
     * IODBPSD 结构体 - 参数数据结构
     * 用于读取和写入 CNC 参数
     * <p>
     * typedef struct  iodbpsd {
     * short datano;              // parameter number
     * short type;                // upper byte:type
     * // lower byte:axis
     * union {
     * char  cdata;            // bit/byte parameter
     * short idata;            // word parameter
     * int  ldata;            // 2-word parameter
     * REALPRM rdata;          // real parameter
     * char  cdatas[MAX_AXIS]; //bit/byte parameter with axis
     * short idatas[MAX_AXIS]; // word parameter with axis
     * int   ldatas[MAX_AXIS]; // 2-word parameter with axis
     * REALPRM rdatas[MAX_AXIS]; // real parameter with axis
     * } u;
     * } IODBPSD;
     */
    public static class IODBPSD extends Structure {
        public short datano;  // 参数号
        public short type;    // 类型和轴信息

        // 为简单起见，我们定义一个足够大的字节数组来表示union部分
        // 根据不同的参数类型，使用不同的方式读取数据
        public byte[] u = new byte[256]; // 足够大的空间来存储各种数据类型

        public IODBPSD() {
            super();
        }

        public IODBPSD(com.sun.jna.Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("datano", "type", "u");
        }
    }

    @Autowired
    private ResourceLoader resourceLoader;

    private static final String DLL_RESOURCE_PATH = "dll/Fwlib64.dll";

    private static String DLL_PATH;

    // 定义常量
    public static final short ALL_AXES = -1; // 全部轴

    /**
     * ODBM 结构体 - 自定义宏变量数据结构
     * 用于读取和写入自定义宏变量
     * <p>
     * typedef struct  odbm {
     * short   datano ;    // custom macro variable number
     * short   dummy ;     // (not used)
     * long    mcr_val ;   // value of custom macro variable
     * short   dec_val ;   // number of places of decimals
     * } ODBM ;
     */
    public static class ODBM extends Structure {
        public short datano;    // 宏变量号
        public short dummy;     // 未使用
        public int mcr_val;     // 宏变量值
        public short dec_val;   // 小数位数

        public ODBM() {
            super();
        }

        public ODBM(com.sun.jna.Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("datano", "dummy", "mcr_val", "dec_val");
        }
    }

    /**
     * ODBPRO 结构体 - 程序号数据结构
     * 用于读取当前运行的程序号和主程序号
     * <p>
     * 对于4位程序号：
     * typedef struct odbpro {
     * short  dummy[2] ;   // 未使用
     * short  data ;       // 运行中的程序号
     * short  mdata ;      // 主程序号
     * } ODBPRO ;
     * <p>
     * 对于8位程序号：
     * typedef struct odbpro {
     * short  dummy[2] ;   // 未使用
     * long   data ;       // 运行中的程序号
     * long   mdata ;      // 主程序号
     * } ODBPRO ;
     */
    public static class ODBPRO extends Structure {
        public short[] dummy = new short[2];  // 未使用
        public int data;                      // 运行中的程序号
        public int mdata;                     // 主程序号

        public ODBPRO() {
            super();
        }

        public ODBPRO(com.sun.jna.Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("dummy", "data", "mdata");
        }
    }

    /**
     * ODBEXEPRG 结构体 - 当前执行程序名称数据结构
     * 用于读取CNC当前执行程序的完整路径名
     * <p>
     * typedef struct odbexeprg {
     * char  name[36] ;   // 正在执行的程序名称
     * long  o_num ;      // 正在执行的程序号
     * } ODBEXEPRG ;
     */
    public static class ODBEXEPRG extends Structure {
        public byte[] name = new byte[36];    // 正在执行的程序名称（最多35个字符 + NULL终止符）
        public int o_num;                     // 正在执行的程序号

        public ODBEXEPRG() {
            super();
        }

        public ODBEXEPRG(com.sun.jna.Pointer p) {
            super(p);
            read();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("name", "o_num");
        }
    }

    public interface DLibrary extends StdCallLibrary {
        DLibrary INSTANCE = (DLibrary) Native.loadLibrary(DLL_PATH, DLibrary.class);

        /**
         * 分配库句柄并连接到指定IP地址或主机名的CNC
         *
         * @param ipaddr   CNC的IP地址或主机名字符串 (例如: "192.168.0.1" 或 "CNC-1.FACTORY")
         * @param port     FOCAS1/Ethernet 或 FOCAS2/Ethernet (TCP) 功能的端口号 (unsigned short)
         * @param timeout  超时秒数。如果指定0，则忽略超时处理，库函数将无限等待 (long, Windows上为32位)
         * @param FlibHndl 输出参数，用于存储库句柄的指针 (unsigned short *)
         * @return 返回状态码，0表示成功，非0表示失败
         */
        short cnc_allclibhndl3(String ipaddr, short port, int timeout, ShortByReference FlibHndl);

        /**
         * 读取当前选择为主程序的文件信息
         *
         * @param FlibHndl  库句柄 (unsigned short)
         * @param file_path 输出参数，用于存储文件路径的缓冲区指针 (char *)，需要分配244字节
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_pdf_rdmain(short FlibHndl, Memory file_path);

        /**
         * 读取块计数器的值
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param prog_bc  输出参数，用于存储块计数器值的指针 (long *)，在Windows上long为32位
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_rdblkcount(short FlibHndl, IntByReference prog_bc);

        /**
         * 读取正在CNC上执行的NC程序内容
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param length   输入/输出参数，输入时指定要读取的字符数，输出时返回实际读取的字符数 (unsigned short *)
         * @param blknum   输出参数，用于存储读取的块数 (short *)
         * @param data     输出参数，用于存储执行中的NC程序的字符串缓冲区 (char *)
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_rdexecprog(short FlibHndl, ShortByReference length, ShortByReference blknum, Memory data);

        /**
         * 读取 PMC 数据信息（关于各种 PMC 的属性、PMC 地址的有效范围等）
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param adr_type PMC地址类型：0,... 参考 pmc_rdpmcrng 函数表；-1 表示所有地址信息
         * @param pmcif    输出参数，指向 ODBPMCINF 结构体的指针，用于存储 PMC 数据信息
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short pmc_rdpmcinfo(short FlibHndl, short adr_type, ODBPMCINF pmcif);

        /**
         * 读取 DNC 下载程序的诊断数据
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param dgndt    输出参数，指向 ODBDNCDGN 结构体的指针，用于返回诊断数据
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_rddncdgndt(short FlibHndl, ODBDNCDGN dgndt);

        /**
         * 读取指定参数号和轴的参数值
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param number   参数号 (short)
         * @param axis     轴号 (short): 0表示无轴, 1到m表示单轴, ALL_AXES(-1)表示全部轴
         * @param length   数据块长度 (short): (4+(参数字节大小)*(轴数))
         * @param param    输出参数，指向IODBPSD结构体的指针，包含参数值
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_rdparam(short FlibHndl, short number, short axis, short length, IODBPSD param);

        /**
         * 读取指定编号的自定义宏变量值
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param number   宏变量号 (short)
         * @param length   数据块长度 (short): ODBM结构体大小=10
         * @param macro    输出参数，指向ODBM结构体的指针，包含宏变量值
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_rdmacro(short FlibHndl, short number, short length, ODBM macro);

        /**
         * 读取CNC中当前选中的程序号（模态O号）
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param prgnum   输出参数，指向ODBPRO结构体的指针，包含程序号信息
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_rdprgnum(short FlibHndl, ODBPRO prgnum);

        /**
         * 读取CNC的报警状态
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param alarm    输出参数，指向存储报警状态的变量地址
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_alarm2(short FlibHndl, IntByReference alarm);

        /**
         * 读取CNC当前执行程序的完整路径名
         * 当CNC停止时，获取执行程序的名称
         * 程序名称存储在"exeprg.name"中，最大32字符字符串格式
         * <p>
         * O号码程序的情况：
         * exeprg.name:  'O'和数字以ASCII码形式存储，例如"O123"
         * exeprg.o_num: O号码以二进制格式存储，例如123
         * <p>
         * 非O号码程序的情况：
         * exeprg.name:  程序名以ASCII码形式存储，例如"ABC"
         * exeprg.o_num: 0以二进制格式存储
         *
         * @param FlibHndl 库句柄 (unsigned short)
         * @param exeprg   输出参数，指向ODBEXEPRG结构体的指针，包含当前执行程序信息
         * @return 返回状态码，0(EW_OK)表示成功，非0表示失败
         */
        short cnc_exeprgname(short FlibHndl, ODBEXEPRG exeprg);
    }

    static {
        DLL_PATH = "Fwlib64.dll";
        System.out.println(DLL_PATH);
        System.out.println("===================");
    }

    //    @PostConstruct()
    public void init() throws IOException {
//        Resource resource = resourceLoader.getResource("classpath:dll/Fwlib64.dll");
//        String path =resource
//        File file = resource.getFile();
//        System.out.println(file.getAbsolutePath());
        System.out.println("--------------------");
        ShortByReference handle = new ShortByReference();
        short result = DLibrary.INSTANCE.cnc_allclibhndl3("10.1.13.152", (short) 8193, 10, handle);
        System.out.println("返回结果: " + result);
        System.out.println(readMainProgram(handle.getValue()));
//        System.out.println(readBlockCount(handle.getValue()));
//        System.out.println(readMainProgramWithError(handle.getValue()));
//        System.out.println(readBlockCountWithError(handle.getValue()));
//        System.out.println(readExecProgramWithError(handle.getValue(),1024));
//        System.out.println(readDncDiag(handle.getValue()));

        // 测试读取参数功能
        short paramNumber = 10; // 示例参数号
        short axis = 0;         // 无轴
        short length = 6;       // 示例长度 (4 + 2 * 1) 用于word类型单轴参数
//        System.out.println(readParameterWithError(handle.getValue(), paramNumber, axis, length));

        // 测试读取宏变量功能
        short macroNumber = 3901;  // 示例宏变量号 #1
        short macroLength = 10; // ODBM结构体大小

        // 测试读取程序号功能
        System.out.println(readProgramNumberWithError(handle.getValue()));

        // 测试读取报警状态功能
        System.out.println(readAlarmStatusWithError(handle.getValue()));
        while (true) {
            log.info("{}", readMacroVariableWithError(handle.getValue(), macroNumber, macroLength));
            log.info("{}", readCurrentExecProgram(handle.getValue()));
            log.info("{}", readExecProgramWithError(handle.getValue(), 1024));
        }
    }

    /**
     * 读取当前选择为主程序的文件信息
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 文件路径字符串，如果失败则返回 null
     */
    public String readMainProgram(short handle) {
        // 分配244字节的缓冲区（最大242字符 + NULL终止符）
        Memory filePathBuffer = new Memory(244);

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_pdf_rdmain(handle, filePathBuffer);

        if (result == 0) {
            // 成功：从缓冲区读取字符串（C字符串以NULL结尾）
            String filePath = filePathBuffer.getString(0);
            return filePath;
        } else {
            // 失败：返回错误信息
            System.err.println("读取主程序文件信息失败，错误代码: " + result);
            return null;
        }
    }

    /**
     * 读取当前选择为主程序的文件信息（带错误处理）
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 包含文件路径和错误代码的结果对象
     */
    public ProgramFileInfo readMainProgramWithError(short handle) {
        // 分配244字节的缓冲区（最大242字符 + NULL终止符）
        Memory filePathBuffer = new Memory(244);

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_pdf_rdmain(handle, filePathBuffer);

        ProgramFileInfo info = new ProgramFileInfo();
        info.setErrorCode(result);

        if (result == 0) {
            // 成功：从缓冲区读取字符串（C字符串以NULL结尾）
            String filePath = filePathBuffer.getString(0);
            info.setFilePath(filePath);
            info.setSuccess(true);
        } else {
            // 失败
            info.setSuccess(false);
            info.setErrorMessage("读取主程序文件信息失败，错误代码: " + result);
        }

        return info;
    }

    /**
     * 程序文件信息结果类
     */
    @Setter
    @Getter
    public static class ProgramFileInfo {
        private boolean success;
        private String filePath;
        private short errorCode;
        private String errorMessage;

        @Override
        public String toString() {
            if (success) {
                return "ProgramFileInfo{success=true, filePath='" + filePath + "'}";
            } else {
                return "ProgramFileInfo{success=false, errorCode=" + errorCode + ", errorMessage='" + errorMessage + "'}";
            }
        }
    }

    /**
     * 读取块计数器的值
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 块计数器值，如果失败则返回 null
     */
    public Long readBlockCount(short handle) {
        // 创建输出参数引用
        IntByReference blockCountRef = new IntByReference();

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_rdblkcount(handle, blockCountRef);

        if (result == 0) {
            // 成功：获取块计数器值（注意：Windows上的long是32位，所以使用int）
            // 但为了保持与Java long的一致性，转换为long返回
            return (long) blockCountRef.getValue();
        } else {
            // 失败：返回错误信息
            System.err.println("读取块计数器失败，错误代码: " + result);
            return null;
        }
    }

    /**
     * 读取块计数器的值（带错误处理）
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 包含块计数器值和错误代码的结果对象
     */
    public BlockCountInfo readBlockCountWithError(short handle) {
        // 创建输出参数引用
        IntByReference blockCountRef = new IntByReference();

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_rdblkcount(handle, blockCountRef);

        BlockCountInfo info = new BlockCountInfo();
        info.setErrorCode(result);

        if (result == 0) {
            // 成功：获取块计数器值（注意：Windows上的long是32位，所以使用int）
            // 但为了保持与Java long的一致性，转换为long返回
            info.setBlockCount((long) blockCountRef.getValue());
            info.setSuccess(true);
        } else {
            // 失败
            info.setSuccess(false);
            info.setErrorMessage("读取块计数器失败，错误代码: " + result);
        }

        return info;
    }

    /**
     * 块计数器信息结果类
     */
    public static class BlockCountInfo {
        private boolean success;
        private Long blockCount;
        private short errorCode;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public Long getBlockCount() {
            return blockCount;
        }

        public void setBlockCount(Long blockCount) {
            this.blockCount = blockCount;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return "BlockCountInfo{success=true, blockCount=" + blockCount + "}";
            } else {
                return "BlockCountInfo{success=false, errorCode=" + errorCode + ", errorMessage='" + errorMessage + "'}";
            }
        }
    }

    /**
     * 读取正在CNC上执行的NC程序内容
     *
     * @param handle    库句柄（通过 cnc_allclibhndl3 获取）
     * @param maxLength 要读取的最大字符数（建议值：1024 或更大）
     * @return 执行程序信息，如果失败则返回 null
     */
    public ExecProgramInfo readExecProgram(short handle, int maxLength) {
        // 创建输入/输出参数：length（先设置要读取的字符数）
        ShortByReference lengthRef = new ShortByReference((short) maxLength);

        // 创建输出参数：blknum（读取的块数）
        ShortByReference blknumRef = new ShortByReference();

        // 分配数据缓冲区（根据maxLength分配，需要额外空间用于NULL终止符）
        Memory dataBuffer = new Memory(maxLength + 1);

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_rdexecprog(handle, lengthRef, blknumRef, dataBuffer);

        if (result == 0) {
            // 成功：获取实际读取的字符数和块数
            int actualLength = lengthRef.getValue() & 0xFFFF; // 转换为无符号整数
            int blockNumber = blknumRef.getValue();

            // 从缓冲区读取字符串（C字符串以NULL结尾）
            String programData = dataBuffer.getString(0);

            ExecProgramInfo info = new ExecProgramInfo();
            info.setProgramData(programData);
            info.setActualLength(actualLength);
            info.setBlockNumber(blockNumber);
            info.setSuccess(true);
            return info;
        } else {
            // 失败
            System.err.println("读取执行程序失败，错误代码: " + result);
            return null;
        }
    }

    /**
     * 读取正在CNC上执行的NC程序内容（带错误处理）
     *
     * @param handle    库句柄（通过 cnc_allclibhndl3 获取）
     * @param maxLength 要读取的最大字符数（建议值：1024 或更大）
     * @return 包含程序内容和错误代码的结果对象
     */
    public ExecProgramInfo readExecProgramWithError(short handle, int maxLength) {
        // 创建输入/输出参数：length（先设置要读取的字符数）
        ShortByReference lengthRef = new ShortByReference((short) maxLength);

        // 创建输出参数：blknum（读取的块数）
        ShortByReference blknumRef = new ShortByReference();

        // 分配数据缓冲区（根据maxLength分配，需要额外空间用于NULL终止符）
        Memory dataBuffer = new Memory(maxLength + 1);

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_rdexecprog(handle, lengthRef, blknumRef, dataBuffer);

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

    /**
     * 执行程序信息结果类
     */
    public static class ExecProgramInfo {
        private boolean success;
        private String programData;
        private int actualLength;
        private int blockNumber;
        private short errorCode;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getProgramData() {
            return programData;
        }

        public void setProgramData(String programData) {
            this.programData = programData;
        }

        public int getActualLength() {
            return actualLength;
        }

        public void setActualLength(int actualLength) {
            this.actualLength = actualLength;
        }

        public int getBlockNumber() {
            return blockNumber;
        }

        public void setBlockNumber(int blockNumber) {
            this.blockNumber = blockNumber;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

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

    /**
     * 读取 DNC 下载程序的诊断数据
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 诊断信息，如果失败则返回 null
     */
    public DncDiagInfo readDncDiag(short handle) {
        ODBDNCDGN nativeData = new ODBDNCDGN();

        short result = DLibrary.INSTANCE.cnc_rddncdgndt(handle, nativeData);
        if (result != 0) {
            System.err.println("读取 DNC 诊断数据失败，错误代码: " + result);
            return null;
        }

        nativeData.read();
        return mapDncDiagInfo(nativeData, result);
    }

    /**
     * 读取 DNC 下载程序的诊断数据（带错误处理）
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 包含诊断数据和错误代码的结果对象
     */
    public DncDiagInfo readDncDiagWithError(short handle) {
        ODBDNCDGN nativeData = new ODBDNCDGN();

        short result = DLibrary.INSTANCE.cnc_rddncdgndt(handle, nativeData);
        nativeData.read();

        DncDiagInfo info = mapDncDiagInfo(nativeData, result);
        info.setErrorCode(result);
        info.setSuccess(result == 0);

        if (result != 0) {
            info.setErrorMessage("读取 DNC 诊断数据失败，错误代码: " + result);
        }

        return info;
    }

    /**
     * 将本地结构体映射为 Java 的诊断信息对象
     */
    private DncDiagInfo mapDncDiagInfo(ODBDNCDGN nativeData, short result) {
        DncDiagInfo info = new DncDiagInfo();

        // ctrl_word / can_word 直接复制
        info.setCtrlWord(nativeData.ctrl_word);
        info.setCanWord(nativeData.can_word);

        // nc_file 为 C char[16]，以 NULL 结尾，使用 ASCII 解码
        int len = 0;
        for (int i = 0; i < nativeData.nc_file.length; i++) {
            if (nativeData.nc_file[i] == 0) {
                break;
            }
            len++;
        }
        String ncFile = new String(nativeData.nc_file, 0, len, StandardCharsets.US_ASCII);
        info.setNcFile(ncFile);

        // unsigned short -> int（0~65535）
        info.setReadPtr(nativeData.read_ptr & 0xFFFF);
        info.setWritePtr(nativeData.write_ptr & 0xFFFF);
        info.setEmptyCount(nativeData.empty_cnt & 0xFFFF);

        // unsigned long(32位) -> long
        long totalSize = nativeData.total_size & 0xFFFFFFFFL;
        info.setTotalSize(totalSize);

        return info;
    }

    /**
     * DNC 诊断信息结果类
     */
    public static class DncDiagInfo {
        private boolean success;
        private short ctrlWord;      // 控制字
        private short canWord;       // 取消字
        private String ncFile;       // 程序号/程序名
        private int readPtr;         // 读指针
        private int writePtr;        // 写指针
        private int emptyCount;      // 读指针追上写指针的次数
        private long totalSize;      // 已输出的字符总数
        private short errorCode;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public short getCtrlWord() {
            return ctrlWord;
        }

        public void setCtrlWord(short ctrlWord) {
            this.ctrlWord = ctrlWord;
        }

        public short getCanWord() {
            return canWord;
        }

        public void setCanWord(short canWord) {
            this.canWord = canWord;
        }

        public String getNcFile() {
            return ncFile;
        }

        public void setNcFile(String ncFile) {
            this.ncFile = ncFile;
        }

        public int getReadPtr() {
            return readPtr;
        }

        public void setReadPtr(int readPtr) {
            this.readPtr = readPtr;
        }

        public int getWritePtr() {
            return writePtr;
        }

        public void setWritePtr(int writePtr) {
            this.writePtr = writePtr;
        }

        public int getEmptyCount() {
            return emptyCount;
        }

        public void setEmptyCount(int emptyCount) {
            this.emptyCount = emptyCount;
        }

        public long getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(long totalSize) {
            this.totalSize = totalSize;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return "DncDiagInfo{success=true, ctrlWord=" + ctrlWord +
                        ", canWord=" + canWord +
                        ", ncFile='" + ncFile + '\'' +
                        ", readPtr=" + readPtr +
                        ", writePtr=" + writePtr +
                        ", emptyCount=" + emptyCount +
                        ", totalSize=" + totalSize +
                        '}';
            } else {
                return "DncDiagInfo{success=false, errorCode=" + errorCode +
                        ", errorMessage='" + errorMessage + '\'' +
                        '}';
            }
        }
    }

    /**
     * 读取 PMC 数据信息
     *
     * @param handle  库句柄（通过 cnc_allclibhndl3 获取）
     * @param adrType PMC地址类型：0,... 参考 pmc_rdpmcrng 函数表；-1 表示所有地址信息
     * @return PMC信息，如果失败则返回 null
     */
    public PmcInfo readPmcInfo(short handle, short adrType) {
        try {
            // 创建 ODBPMCINF 结构体实例（会自动分配内存）
            ODBPMCINF pmcInfo = new ODBPMCINF();

            // 确保结构体已分配内存
//            pmcInfo.allocateMemory();

            // 调用 DLL 函数
            short result = DLibrary.INSTANCE.pmc_rdpmcinfo(handle, adrType, pmcInfo);

            if (result == 0) {
                // 成功：读取结构体数据
                pmcInfo.read();

                PmcInfo info = new PmcInfo();
                info.setStartAddress(pmcInfo.datano_s & 0xFFFF); // 转换为无符号整数
                info.setEndAddress(pmcInfo.datano_e & 0xFFFF);
                info.setUnit(pmcInfo.unit);
                info.setSize(pmcInfo.size);
                info.setSuccess(true);

                // 清理内存
                pmcInfo.clear();
                return info;
            } else {
                // 失败
                System.err.println("读取 PMC 信息失败，错误代码: " + result);
                pmcInfo.clear();
                return null;
            }
        } catch (Exception e) {
            System.err.println("读取 PMC 信息时发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 读取 PMC 数据信息（带错误处理）
     *
     * @param handle  库句柄（通过 cnc_allclibhndl3 获取）
     * @param adrType PMC地址类型：0,... 参考 pmc_rdpmcrng 函数表；-1 表示所有地址信息
     * @return 包含 PMC 信息和错误代码的结果对象
     */
    public PmcInfo readPmcInfoWithError(short handle, short adrType) {
        ODBPMCINF pmcInfo = null;
        try {
            // 创建 ODBPMCINF 结构体实例（会自动分配内存）
            pmcInfo = new ODBPMCINF();

            // 确保结构体已分配内存
//            pmcInfo.allocateMemory();

            // 调用 DLL 函数
            short result = DLibrary.INSTANCE.pmc_rdpmcinfo(handle, adrType, pmcInfo);

            PmcInfo info = new PmcInfo();
            info.setErrorCode(result);

            if (result == 0) {
                // 成功：读取结构体数据
                pmcInfo.read();

                info.setStartAddress(pmcInfo.datano_s & 0xFFFF); // 转换为无符号整数
                info.setEndAddress(pmcInfo.datano_e & 0xFFFF);
                info.setUnit(pmcInfo.unit);
                info.setSize(pmcInfo.size);
                info.setSuccess(true);
            } else {
                // 失败
                info.setSuccess(false);
                info.setErrorMessage("读取 PMC 信息失败，错误代码: " + result);
            }

            return info;
        } catch (Exception e) {
            System.err.println("读取 PMC 信息时发生异常: " + e.getMessage());
            e.printStackTrace();
            PmcInfo info = new PmcInfo();
            info.setSuccess(false);
            info.setErrorMessage("读取 PMC 信息时发生异常: " + e.getMessage());
            return info;
        } finally {
            // 确保清理内存
            if (pmcInfo != null) {
                try {
                    pmcInfo.clear();
                } catch (Exception e) {
                    // 忽略清理时的异常
                }
            }
        }
    }

    /**
     * PMC 信息结果类
     */
    public static class PmcInfo {
        private boolean success;
        private int startAddress;  // 起始 PMC 地址
        private int endAddress;    // 结束 PMC 地址
        private short unit;         // 数据单位
        private short size;         // 数据大小
        private short errorCode;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getStartAddress() {
            return startAddress;
        }

        public void setStartAddress(int startAddress) {
            this.startAddress = startAddress;
        }

        public int getEndAddress() {
            return endAddress;
        }

        public void setEndAddress(int endAddress) {
            this.endAddress = endAddress;
        }

        public short getUnit() {
            return unit;
        }

        public void setUnit(short unit) {
            this.unit = unit;
        }

        public short getSize() {
            return size;
        }

        public void setSize(short size) {
            this.size = size;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return "PmcInfo{success=true, startAddress=" + startAddress +
                        ", endAddress=" + endAddress + ", unit=" + unit +
                        ", size=" + size + "}";
            } else {
                return "PmcInfo{success=false, errorCode=" + errorCode +
                        ", errorMessage='" + errorMessage + "'}";
            }
        }
    }

    /**
     * 读取 CNC 参数值
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @param number 参数号
     * @param axis   轴号: 0表示无轴, 1到m表示单轴, ALL_AXES(-1)表示全部轴
     * @param length 数据块长度: (4+(参数字节大小)*(轴数))
     * @return 参数信息，如果失败则返回 null
     */
    public ParameterInfo readParameter(short handle, short number, short axis, short length) {
        // 创建 IODBPSD 结构体实例
        IODBPSD param = new IODBPSD();

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_rdparam(handle, number, axis, length, param);

        if (result == 0) {
            // 成功：读取结构体数据
            param.read();

            ParameterInfo info = new ParameterInfo();
            info.setParamNumber(param.datano & 0xFFFF); // 转换为无符号整数

            // 解析类型信息：高字节为类型，低字节为轴
            short typeInfo = param.type;
            short paramType = (short) ((typeInfo >> 8) & 0xFF); // 高字节
            short paramAxis = (short) (typeInfo & 0xFF);       // 低字节

            info.setParamType(paramType);
            info.setAxis(paramAxis);
            info.setData(parseParameterData(param, paramType, paramAxis));
            info.setSuccess(true);

            // 清理内存
            param.clear();
            return info;
        } else {
            // 失败
            System.err.println("读取参数失败，错误代码: " + result);
            param.clear();
            return null;
        }
    }

    /**
     * 读取 CNC 参数值（带错误处理）
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @param number 参数号
     * @param axis   轴号: 0表示无轴, 1到m表示单轴, ALL_AXES(-1)表示全部轴
     * @param length 数据块长度: (4+(参数字节大小)*(轴数))
     * @return 包含参数信息和错误代码的结果对象
     */
    public ParameterInfo readParameterWithError(short handle, short number, short axis, short length) {
        IODBPSD param = null;
        try {
            // 创建 IODBPSD 结构体实例
            param = new IODBPSD();

            // 调用 DLL 函数
            short result = DLibrary.INSTANCE.cnc_rdparam(handle, number, axis, length, param);

            ParameterInfo info = new ParameterInfo();
            info.setErrorCode(result);

            if (result == 0) {
                // 成功：读取结构体数据
                param.read();

                info.setParamNumber(param.datano & 0xFFFF); // 转换为无符号整数

                // 解析类型信息：高字节为类型，低字节为轴
                short typeInfo = param.type;
                short paramType = (short) ((typeInfo >> 8) & 0xFF); // 高字节
                short paramAxis = (short) (typeInfo & 0xFF);       // 低字节

                info.setParamType(paramType);
                info.setAxis(paramAxis);
                info.setData(parseParameterData(param, paramType, paramAxis));
                info.setSuccess(true);
            } else {
                // 失败
                info.setSuccess(false);
                info.setErrorMessage("读取参数失败，错误代码: " + result);
            }

            return info;
        } catch (Exception e) {
            System.err.println("读取参数时发生异常: " + e.getMessage());
            e.printStackTrace();
            ParameterInfo info = new ParameterInfo();
            info.setSuccess(false);
            info.setErrorMessage("读取参数时发生异常: " + e.getMessage());
            return info;
        } finally {
            // 确保清理内存
            if (param != null) {
                try {
                    param.clear();
                } catch (Exception e) {
                    // 忽略清理时的异常
                }
            }
        }
    }

    /**
     * 解析参数数据，根据参数类型和轴信息
     *
     * @param param     结构体参数
     * @param paramType 参数类型
     * @param paramAxis 轴信息
     * @return 解析后的参数数据
     */
    private Object parseParameterData(IODBPSD param, short paramType, short paramAxis) {
        // 参数类型:
        // 0: bit type
        // 1: byte type
        // 2: word type
        // 3: 2-word type
        // 4: real type (only Series 15i)

        switch (paramType) {
            case 0: // bit type
            case 1: // byte type
                if (paramAxis == 0 || paramAxis == 1) {
                    // 单个字节数据
                    return param.u[0];
                } else {
                    // 多轴数据
                    byte[] bytes = new byte[256];
                    System.arraycopy(param.u, 0, bytes, 0, 256);
                    return bytes;
                }
            case 2: // word type
                if (paramAxis == 0 || paramAxis == 1) {
                    // 单个短整型数据
                    return (short) ((param.u[1] & 0xFF) << 8 | (param.u[0] & 0xFF));
                } else {
                    // 多轴数据
                    short[] shorts = new short[128]; // 假设最多128个轴
                    for (int i = 0; i < shorts.length && (i * 2 + 1) < param.u.length; i++) {
                        shorts[i] = (short) ((param.u[i * 2 + 1] & 0xFF) << 8 | (param.u[i * 2] & 0xFF));
                    }
                    return shorts;
                }
            case 3: // 2-word type
                if (paramAxis == 0 || paramAxis == 1) {
                    // 单个整型数据
                    return (param.u[3] & 0xFF) << 24 | (param.u[2] & 0xFF) << 16 |
                            (param.u[1] & 0xFF) << 8 | (param.u[0] & 0xFF);
                } else {
                    // 多轴数据
                    int[] ints = new int[64]; // 假设最多64个轴
                    for (int i = 0; i < ints.length && (i * 4 + 3) < param.u.length; i++) {
                        ints[i] = (param.u[i * 4 + 3] & 0xFF) << 24 | (param.u[i * 4 + 2] & 0xFF) << 16 |
                                (param.u[i * 4 + 1] & 0xFF) << 8 | (param.u[i * 4] & 0xFF);
                    }
                    return ints;
                }
            case 4: // real type (only Series 15i)
                if (paramAxis == 0 || paramAxis == 1) {
                    // 单个实数数据
                    REALPRM realParam = new REALPRM();
                    // 将字节数据复制到REALPRM结构
                    System.arraycopy(param.u, 0, realParam.getPointer(), 0, 8);
                    realParam.read();
                    return realParam;
                } else {
                    // 多轴实数数据
                    REALPRM[] realParams = new REALPRM[32]; // 假设最多32个轴
                    for (int i = 0; i < realParams.length && (i * 8 + 7) < param.u.length; i++) {
                        REALPRM rp = new REALPRM();
                        System.arraycopy(param.u, i * 8, rp.getPointer(), 0, 8);
                        rp.read();
                        realParams[i] = rp;
                    }
                    return realParams;
                }
            default:
                return param.u; // 返回原始字节数组
        }
    }

    /**
     * 参数信息结果类
     */
    public static class ParameterInfo {
        private boolean success;
        private int paramNumber;  // 参数号
        private short paramType;  // 参数类型
        private short axis;       // 轴信息
        private Object data;      // 参数数据
        private short errorCode;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getParamNumber() {
            return paramNumber;
        }

        public void setParamNumber(int paramNumber) {
            this.paramNumber = paramNumber;
        }

        public short getParamType() {
            return paramType;
        }

        public void setParamType(short paramType) {
            this.paramType = paramType;
        }

        public short getAxis() {
            return axis;
        }

        public void setAxis(short axis) {
            this.axis = axis;
        }

        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return "ParameterInfo{success=true, paramNumber=" + paramNumber +
                        ", paramType=" + paramType +
                        ", axis=" + axis +
                        ", data=" + data + "}";
            } else {
                return "ParameterInfo{success=false, errorCode=" + errorCode +
                        ", errorMessage='" + errorMessage + "'" +
                        '}';
            }
        }
    }

    /**
     * 读取自定义宏变量值
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @param number 宏变量号
     * @param length 数据块长度: ODBM结构体大小=10
     * @return 宏变量信息，如果失败则返回 null
     */
    public MacroVariableInfo readMacroVariable(short handle, short number, short length) {
        // 创建 ODBM 结构体实例
        ODBM macro = new ODBM();

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_rdmacro(handle, number, length, macro);

        if (result == 0) {
            // 成功：读取结构体数据
            macro.read();

            MacroVariableInfo info = new MacroVariableInfo();
            info.setVariableNumber(macro.datano & 0xFFFF); // 转换为无符号整数
            info.setValue(macro.mcr_val);
            info.setDecimalPlaces(macro.dec_val);
            info.setSuccess(true);

            // 清理内存
            macro.clear();
            return info;
        } else {
            // 失败
            System.err.println("读取宏变量失败，错误代码: " + result);
            macro.clear();
            return null;
        }
    }

    /**
     * 读取自定义宏变量值（带错误处理）
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @param number 宏变量号
     * @param length 数据块长度: ODBM结构体大小=10
     * @return 包含宏变量信息和错误代码的结果对象
     */
    public MacroVariableInfo readMacroVariableWithError(short handle, short number, short length) {
        ODBM macro = null;
        try {
            // 创建 ODBM 结构体实例
            macro = new ODBM();

            // 调用 DLL 函数
            short result = DLibrary.INSTANCE.cnc_rdmacro(handle, number, length, macro);

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

    /**
     * 宏变量信息结果类
     */
    public static class MacroVariableInfo {
        private boolean success;
        private int variableNumber;  // 变量号
        private int value;           // 变量值
        private short decimalPlaces; // 小数位数
        private short errorCode;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getVariableNumber() {
            return variableNumber;
        }

        public void setVariableNumber(int variableNumber) {
            this.variableNumber = variableNumber;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public short getDecimalPlaces() {
            return decimalPlaces;
        }

        public void setDecimalPlaces(short decimalPlaces) {
            this.decimalPlaces = decimalPlaces;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        /**
         * 计算实际的浮点值
         * 实际值 = mcr_val * 10^(-dec_val)
         *
         * @return 实际的浮点值
         */
        public double getActualValue() {
            if (!success) {
                return Double.NaN; // 如果操作失败，返回NaN
            }

            // 如果是未定义的变量（vacant），根据文档返回值
            if (value == 0 && decimalPlaces == -1) {
                return Double.NaN; // 表示未定义的变量
            }

            return value * Math.pow(10, -decimalPlaces);
        }

        @Override
        public String toString() {
            if (success) {
                return "MacroVariableInfo{success=true, variableNumber=" + variableNumber +
                        ", value=" + value +
                        ", decimalPlaces=" + decimalPlaces +
                        ", actualValue=" + getActualValue() + "}";
            } else {
                return "MacroVariableInfo{success=false, errorCode=" + errorCode +
                        ", errorMessage='" + errorMessage + "'" +
                        '}';
            }
        }
    }

    /**
     * 读取当前运行的程序号
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 程序号信息，如果失败则返回 null
     */
    public ProgramNumberInfo readProgramNumber(short handle) {
        // 创建 ODBPRO 结构体实例
        ODBPRO prgnum = new ODBPRO();

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_rdprgnum(handle, prgnum);

        if (result == 0) {
            // 成功：读取结构体数据
            prgnum.read();

            ProgramNumberInfo info = new ProgramNumberInfo();
            info.setRunningProgramNumber(prgnum.data);
            info.setMainProgramNumber(prgnum.mdata);
            info.setSuccess(true);

            // 清理内存
            prgnum.clear();
            return info;
        } else {
            // 失败
            System.err.println("读取程序号失败，错误代码: " + result);
            prgnum.clear();
            return null;
        }
    }

    /**
     * 读取当前运行的程序号（带错误处理）
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 包含程序号信息和错误代码的结果对象
     */
    public ProgramNumberInfo readProgramNumberWithError(short handle) {
        ODBPRO prgnum = null;
        try {
            // 创建 ODBPRO 结构体实例
            prgnum = new ODBPRO();

            // 调用 DLL 函数
            short result = DLibrary.INSTANCE.cnc_rdprgnum(handle, prgnum);

            ProgramNumberInfo info = new ProgramNumberInfo();
            info.setErrorCode(result);

            if (result == 0) {
                // 成功：读取结构体数据
                prgnum.read();
                info.setRunningProgramNumber(prgnum.data);
                info.setMainProgramNumber(prgnum.mdata);
                info.setSuccess(true);
            } else {
                // 失败
                info.setSuccess(false);
                info.setErrorMessage("读取程序号失败，错误代码: " + result);
            }

            return info;
        } catch (Exception e) {
            System.err.println("读取程序号时发生异常: " + e.getMessage());
            e.printStackTrace();
            ProgramNumberInfo info = new ProgramNumberInfo();
            info.setSuccess(false);
            info.setErrorMessage("读取程序号时发生异常: " + e.getMessage());
            return info;
        } finally {
            // 确保清理内存
            if (prgnum != null) {
                try {
                    prgnum.clear();
                } catch (Exception e) {
                    // 忽略清理时的异常
                }
            }
        }
    }

    /**
     * 程序号信息结果类
     */
    public static class ProgramNumberInfo {
        private boolean success;
        private int runningProgramNumber;  // 正在运行的程序号
        private int mainProgramNumber;     // 主程序号
        private short errorCode;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getRunningProgramNumber() {
            return runningProgramNumber;
        }

        public void setRunningProgramNumber(int runningProgramNumber) {
            this.runningProgramNumber = runningProgramNumber;
        }

        public int getMainProgramNumber() {
            return mainProgramNumber;
        }

        public void setMainProgramNumber(int mainProgramNumber) {
            this.mainProgramNumber = mainProgramNumber;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @Override
        public String toString() {
            if (success) {
                return "ProgramNumberInfo{success=true, runningProgramNumber=" + runningProgramNumber +
                        ", mainProgramNumber=" + mainProgramNumber + "}";
            } else {
                return "ProgramNumberInfo{success=false, errorCode=" + errorCode +
                        ", errorMessage='" + errorMessage + "'" +
                        '}';
            }
        }
    }

    /**
     * 读取CNC的报警状态
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 报警信息，如果失败则返回 null
     */
    public AlarmStatusInfo readAlarmStatus(short handle) {
        // 创建输出参数引用
        IntByReference alarmRef = new IntByReference();

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_alarm2(handle, alarmRef);

        if (result == 0) {
            // 成功：获取报警状态
            int alarmStatus = alarmRef.getValue();

            AlarmStatusInfo info = new AlarmStatusInfo();
            info.setAlarmStatus(alarmStatus);
            info.setSuccess(true);

            return info;
        } else {
            // 失败
            System.err.println("读取报警状态失败，错误代码: " + result);
            return null;
        }
    }

    /**
     * 读取CNC的报警状态（带错误处理）
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 包含报警信息和错误代码的结果对象
     */
    public AlarmStatusInfo readAlarmStatusWithError(short handle) {
        IntByReference alarmRef = new IntByReference();

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_alarm2(handle, alarmRef);

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


    public static class AlarmStatusInfo {
        private boolean success;
        private int alarmStatus;  // 报警状态位
        private short errorCode;
        private String errorMessage;


        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public int getAlarmStatus() {
            return alarmStatus;
        }

        public void setAlarmStatus(int alarmStatus) {
            this.alarmStatus = alarmStatus;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }


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

    /**
     * 当前执行程序信息结果类
     * 用于封装从CNC读取的当前执行程序信息
     */
    public static class ProgramNameInfo {
        private boolean success;
        private String programName;  // 程序名称
        private int programNumber;   // 程序号
        private short errorCode;
        private String errorMessage;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getProgramName() {
            return programName;
        }

        public void setProgramName(String programName) {
            this.programName = programName;
        }

        public int getProgramNumber() {
            return programNumber;
        }

        public void setProgramNumber(int programNumber) {
            this.programNumber = programNumber;
        }

        public short getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(short errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

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

    /**
     * 读取CNC当前执行程序的完整路径名
     * 当CNC停止时，获取执行程序的名称
     * 程序名称存储在返回对象的programName字段中
     * <p>
     * O号码程序的情况：
     * 返回对象的programName字段: 'O'和数字以ASCII码形式存储，例如"O123"
     * 返回对象的programNumber字段: O号码以二进制格式存储，例如123
     * <p>
     * 非O号码程序的情况：
     * 返回对象的programName字段: 程序名以ASCII码形式存储，例如"ABC"
     * 返回对象的programNumber字段: 0以二进制格式存储
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 当前执行程序信息，如果失败则返回 null
     */
    public ProgramNameInfo readCurrentExecProgram(short handle) {
        // 创建 ODBEXEPRG 结构体实例
        ODBEXEPRG exeprg = new ODBEXEPRG();

        // 调用 DLL 函数
        short result = DLibrary.INSTANCE.cnc_exeprgname(handle, exeprg);

        if (result == 0) {
            // 成功：读取结构体数据
            exeprg.read();

            ProgramNameInfo info = new ProgramNameInfo();

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

            // 清理内存
            exeprg.clear();
            return info;
        } else {
            // 失败
            System.err.println("读取当前执行程序名称失败，错误代码: " + result);
            exeprg.clear();
            return null;
        }
    }

    /**
     * 读取CNC当前执行程序的完整路径名（带错误处理）
     * 当CNC停止时，获取执行程序的名称
     * 程序名称存储在返回对象的programName字段中
     * <p>
     * O号码程序的情况：
     * 返回对象的programName字段: 'O'和数字以ASCII码形式存储，例如"O123"
     * 返回对象的programNumber字段: O号码以二进制格式存储，例如123
     * <p>
     * 非O号码程序的情况：
     * 返回对象的programName字段: 程序名以ASCII码形式存储，例如"ABC"
     * 返回对象的programNumber字段: 0以二进制格式存储
     *
     * @param handle 库句柄（通过 cnc_allclibhndl3 获取）
     * @return 包含当前执行程序信息和错误代码的结果对象
     */
    public ProgramNameInfo readCurrentExecProgramWithError(short handle) {
        ODBEXEPRG exeprg = null;
        try {
            // 创建 ODBEXEPRG 结构体实例
            exeprg = new ODBEXEPRG();

            // 调用 DLL 函数
            short result = DLibrary.INSTANCE.cnc_exeprgname(handle, exeprg);

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

    public String getIpCityInfo1(String ip) {
        log.info("===> Login IP : {}", ip);
        String dbPath = FanucReadDevices.class.getClassLoader().getResource("").getPath() + "ip2region\\ip2region.db";
        log.info("===> dbPath  : {}", dbPath);
        File file = new File(dbPath);
        if (!file.exists()) {
            return null;
        }
        try {
            DbConfig config = new DbConfig();
            DbSearcher searcher = new DbSearcher(config, dbPath);
            Method method = searcher.getClass().getMethod("btreeSearch", String.class);
            if (!Util.isIpAddress(ip)) {
                return null;
            }
            DataBlock dataBlock = (DataBlock) method.invoke(searcher, ip);
            return dataBlock.getRegion();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
