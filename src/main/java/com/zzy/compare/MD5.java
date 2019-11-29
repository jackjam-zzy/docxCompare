package com.zzy.compare;

import com.zzy.compare.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 获取文件的md5值，从而比对两个文件是否相等
 * @Description: TODO
 * @author zzy
 * @date 2019.11.21
 * @version V1.0
 */
public class MD5 {
    public static String getFileMD5(String filePath) throws Exception{
        File file = new File(filePath);
        InputStream in = new FileInputStream(file);
        MessageDigest digest = MessageDigest.getInstance("MD5");  ;
        byte buffer[] = new byte[1024];
        int len;
        while((len = in.read(buffer))!=-1){
            digest.update(buffer, 0, len);
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    public static void main(String[] args) throws Throwable{
        String  path1 = "F:/test/1.docx";
        String  path2 = "F:/test/2.docx";
        String out = "F:/test/out.docx";
        Util util = new Util();

        long startTime = System.currentTimeMillis();    //获取开始时间
        util.compareDocx(path1,path2,out);
        long endTime = System.currentTimeMillis();    //获取结束时间
        System.out.println("程序运行时间：" + (endTime - startTime) + "ms");    //输出程序运行时间


    }
}
