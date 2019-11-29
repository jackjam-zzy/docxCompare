package com.zzy.compare.util;

/*
* author : zhengzhiyuan
* date : 2019.11.26
* */

import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ZzySimHash {
    private String tokens; //字符串
    private BigInteger strSimHash;//字符产的hash值
    private int hashbits = 64; // 分词后的hash数;



    /**
     * 这个是对整个字符串进行hash计算
     * @return
     */
//    private BigInteger simHash() {
//        return
//    }


    /*
    * 分词算法1： HanLP的最短路分词
    * */
    private List<Term> shortSegment(String s){
        Segment shortestSegment = new DijkstraSegment()
                .enableCustomDictionary(false)
                .enablePlaceRecognize(true)
                .enableOrganizationRecognize(true);
        List<Term> seg = shortestSegment.seg(s);
        return seg;
    }

    /*
    * 分词后结果进行处理：单词和词性分离
    * */
    private List<String> getSplitWords(List<Term> term) {
        // 标点符号会被单独分为一个Term，去除之
        return term.stream().map(a -> a.word).filter(s -> !" `~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘’；：”“'。，、？".contains(s)).collect(Collectors.toList());
    }

    /*
     * 对分词后的词语数量进行统计
     * */
    private HashMap<String, Integer> getWordAndCount(List<String> wordList){
        if(null == wordList) return null;
        HashMap<String, Integer> wordCount = new HashMap<>();
        for (String word: wordList) {
            if(wordCount.containsKey(word)){  //存在时
                wordCount.put(word,wordCount.get(word)+1);
            }else {  //不存在时
                wordCount.put(word,1);
            }
        }
        return wordCount;
    }




    /*
    * 对单个分词进行hash
    * */
    private BigInteger hash(String source){

        if (source == null || source.length() == 0) {
            return new BigInteger("0");
        } else {
//            while (source.length() < 2) {
//                source = source + source.charAt(0);
//            }
            char[] sourceArray = source.toCharArray();
            BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
            BigInteger m = new BigInteger("1000003");
            BigInteger mask = new BigInteger("2").pow(this.hashbits).subtract(new BigInteger("1"));
            for (char item : sourceArray) {
                BigInteger temp = BigInteger.valueOf((long) item);
                x = x.multiply(m).xor(temp).and(mask);
            }
            x = x.xor(new BigInteger(String.valueOf(source.length())));
            if (x.equals(new BigInteger("-1"))) {
                x = new BigInteger("-2");
            }
            return x;
        }
    }

    /*
     * 对所有分词进行hash
     * */
//    private BigInteger hash(HashMap<String,Integer> map){
//
//        if (map == null || map.size() == 0) {
//            return new BigInteger("0");
//        } else {
//            for (Map.Entry<String, Integer> entry : map.entrySet()) {
//                char[] sourceArray = entry.getKey().toCharArray();
//                BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
//                BigInteger m = new BigInteger("1000003");
//                BigInteger mask = new BigInteger("2").pow(this.hashbits).subtract(new BigInteger("1"));
//                for (char item : sourceArray) {
//                    BigInteger temp = BigInteger.valueOf((long) item);
//                    x = x.multiply(m).xor(temp).and(mask);
//                }
//                x = x.xor(new BigInteger(String.valueOf(entry.getKey().length())));
//                if (x.equals(new BigInteger("-1"))) {
//                    x = new BigInteger("-2");
//                }
//                return x;
//            }
//        }
//    }


//    private BigInteger simHash(HashMap<String,Integer> map) {
//        int[] v = new int[this.hashbits];
//        for (Map.Entry<String, Integer> entry : map.entrySet()) {
//            BigInteger t = this.hash(entry.getKey());
//            for (int i = 0; i < this.hashbits; i++) {
//                BigInteger bitmask = new BigInteger("1").shiftLeft(i);
//                // 3、建立一个长度为64的整数数组(假设要生成64位的数字指纹,也可以是其它数字),
//                // 对每一个分词hash后的数列进行判断,如果是1000...1,那么数组的第一位和末尾一位加1,
//                // 中间的62位减一,也就是说,逢1加1,逢0减1.一直到把所有的分词hash数列全部判断完毕.
//                if (t.and(bitmask).signum() != 0) {
//                    // 这里是计算整个文档的所有特征的向量和
//                    v[i] += entry.getValue();
//                } else {
//                    v[i] -= entry.getValue();
//                }
//            }
//        }
//        BigInteger fingerprint = new BigInteger("0");
//        for (int i = 0; i < this.hashbits; i++) {
//            if (v[i] >= 0) {
//                fingerprint = fingerprint.add(new BigInteger("1").shiftLeft(i));
//            }
//        }
//        return fingerprint;
//    }



    private BigInteger simHash(HashMap<String,Integer> map) {
        //用来保存最终的simhash签名
        int[] v = new int[this.hashbits];
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            //算出每个分词的hash值，64位
            BigInteger t = this.hashUnsigned(entry.getKey());
            //合并
            for (int i = 0; i < this.hashbits; i++) {
                BigInteger bitmask = new BigInteger("1").shiftLeft(i);
                if (t.and(bitmask).signum() != 0) {
                    v[i] += entry.getValue();
                } else {
                    v[i] -= entry.getValue();
                }
            }
        }
        //降维  ：  如果大于0则置1，否则置0
        BigInteger fingerprint = new BigInteger("0");
        for (int i = 0; i < this.hashbits; i++) {
            if (v[i] >= 0) {
                fingerprint = fingerprint.add(new BigInteger("1").shiftLeft(i));
            }
        }
        return fingerprint;
    }




    //=========================================MurmurHash算法开始=============================================

    private Long murmurHash(byte[] key) {
        ByteBuffer buf = ByteBuffer.wrap(key);  
        int seed = 0x1234ABCD;
        ByteOrder byteOrder = buf.order();  
        buf.order(ByteOrder.LITTLE_ENDIAN);
        long m = 0xc6a4a7935bd1e995L;  
        int r = 47;
        long h = seed ^ (buf.remaining() * m);
        long k;  
        while (buf.remaining() >= 8) {  
            k = buf.getLong();
            k *= m;  
            k ^= k >>> r;  
            k *= m;
            h ^= k;  
            h *= m;  
        }
        if (buf.remaining() > 0) {  
            ByteBuffer finish = ByteBuffer.allocate(8).order(  
                                        ByteOrder.LITTLE_ENDIAN);  
            // for big-endian version, do this first:  
            // finish.position(8-buf.remaining());  
            finish.put(buf).rewind();  
            h ^= finish.getLong();  
            h *= m;  
        }
        h ^= h >>> r;  
        h *= m;  
        h ^= h >>> r;
        buf.order(byteOrder);  
        return h;  
    }

    private Long murmurHash(String key) {
        return murmurHash(key.getBytes());
    }

    /** 
     * Long转换成无符号长整型（C中数据类型）
     */
    private BigInteger readUnsignedLong(Long value) {
        if (null != null)
            return new BigInteger(value.toString());
        long lowValue = value & 0x7fffffffffffffffL;  
        return BigInteger.valueOf(lowValue).add(BigInteger.valueOf(Long.MAX_VALUE)).add(BigInteger.valueOf(1));
    }

    /** 
          * 返回无符号murmur hash值 
          */  
    private BigInteger hashUnsigned(String key) {
        return readUnsignedLong(murmurHash(key));
    }  
    private BigInteger hashUnsigned(byte[] key) {
        return readUnsignedLong(murmurHash(key));
    }
    //=========================================MurmurHash算法结束=============================================



    /**
     * 计算海明距离,海明距离越小说明越相似;
     * @param
     * @return
     */
    private int hammingDistance(BigInteger one,BigInteger two) {
        BigInteger m = new BigInteger("1").shiftLeft(this.hashbits).subtract(
                new BigInteger("1"));
        BigInteger x = one.xor(two).and(m);
        int tot = 0;
        while (x.signum() != 0) {
            tot += 1;
            x = x.and(x.subtract(new BigInteger("1")));
        }
        return tot;
    }


    public double getSemblance(BigInteger one,BigInteger two){
        double i = (double) this.hammingDistance(one,two);
        return 1 - i/this.hashbits ;
    }
    


    //=========================整个方法，求相似度==============
    public double similarity(String path1,String path2){
        BigInteger one = simHash(getWordAndCount(getSplitWords(shortSegment(readFileContent(path1)))));
        BigInteger two = simHash(getWordAndCount(getSplitWords(shortSegment(readFileContent(path2)))));
        System.out.println(one.toString(2));
        System.out.println(two.toString(2));
        System.out.println("海明距离："+hammingDistance(one,two));
        return getSemblance(one,two);
    }
    //=========================整个方法，求相似度结束==============



    /*
    * 读取文件内容
    * */
    private String readFileContent(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }



    public static void main(String[] args) {
        ZzySimHash zzy = new ZzySimHash();
        String path1 = "F:/test/first.txt";
        String path2 = "F:/test/second.txt";
        System.out.println(zzy.similarity(path1,path2));


//        String s = "我们了解这个解释";
//        String s1 = "我们了解这个解释";
//        String s2 = "我们了解这个解释 ni";
//        String s3 = "我们了解这个解释 n";
//
//        BigInteger a = zzy.hashUnsigned("你们了解");
//        BigInteger b = zzy.hashUnsigned("chenshuo");
//        String str1 = a.toString(2);
//        String str2 = b.toString(2);
//        int l1 = a.toString(2).length();
//        int l2 = b.toString(2).length();
//
//
//        BigInteger bitmask = new BigInteger("1").shiftLeft(10);
//
//        System.out.println("大整形为:"+ a +"二进制为:"+ str1 + "二进制位数：" + l1);
//        System.out.println("大整形为:"+ b +"二进制为:"+ str2 + "二进制位数：" + l2);
//        System.out.println("额:"+ b +"想与后的二进制:"+ a.and(bitmask).toString(2) + "二进制位数：" + a.and(b).toString(2).length());
//        System.out.println("额:"+ b +"想与后的二进制:"+ b.and(bitmask).toString(2) + "二进制位数：" + a.and(b).toString(2).length());





//        char[] sourceArray = s.toCharArray();
//        BigInteger x = BigInteger.valueOf(((long) sourceArray[0]) << 7);
//        String s4 = x.toString(2);
//        BigInteger m = new BigInteger("1000003");
//        BigInteger mask = new BigInteger("2").pow(64).subtract(new BigInteger("1"));
//        int length = mask.toString(2).length();





//        List<Term> terms = zzy.shortSegment(s1);
//        List<String> splitWords = zzy.getSplitWords(terms);
//        HashMap<String, Integer> wordCount = zzy.getWordAndCount(splitWords);
//        BigInteger one = zzy.simHash(wordCount);
//
//        List<Term> terms1 = zzy.shortSegment(s2);
//        List<String> splitWords1 = zzy.getSplitWords(terms1);
//        HashMap<String, Integer> wordCount1 = zzy.getWordAndCount(splitWords1);
//        BigInteger two = zzy.simHash(wordCount1);
//
//        System.out.println("s1和s2的海明距离"+ zzy.hammingDistance(one,two));
//        System.out.println(zzy.getSemblance(one,two));


//        System.out.println(terms);
//        System.out.println(splitWords);
//        System.out.println(wordCount);
//        System.out.println(bigInteger);
//        System.out.println(bigInteger.length());
//        System.out.println(zzy.hash(s2).toString(2).length());
//        System.out.println(zzy.hash(s3).toString(2));
//        System.out.println(zzy.hash(s3));

    }
}

