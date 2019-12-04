package com.zzy.compare.util;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Dijkstra.DijkstraSegment;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.suggest.Suggester;
import com.zzy.compare.msg.Block;
import com.zzy.compare.vo.Vo;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.CommentsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

public class Util {
    private static BigInteger commentId = BigInteger.valueOf(1);

    public void compareDocx(String oldPath, String newPath, String out) throws Docx4JException, Exception{

        if (getFileMD5(oldPath).equals(getFileMD5(newPath))){
            System.out.println("文件根本就没修改过");
            return;
        }
        ZzySimHash zzy = new ZzySimHash();

        System.out.println(zzy.similarity(oldPath,newPath));





        //读取旧文档内容
        WordprocessingMLPackage wordMLPackageOld = WordprocessingMLPackage.load(new File(oldPath));
        MainDocumentPart mainDocumentPartOld = wordMLPackageOld.getMainDocumentPart();
        ObjectFactory oldFactory = Context.getWmlObjectFactory();
        List<Object> listOld = mainDocumentPartOld.getContent();

        //cyb
        AddComment addComment=new AddComment();
        WordprocessingMLPackage wordprocessingMLPackage_bin =addComment.createWordprocessingMLPackage();
        MainDocumentPart mainDocumentPart_bin = wordprocessingMLPackage_bin.getMainDocumentPart();  // mp9,获得这个文档的document与标签对象
        ObjectFactory factory_bin = Context.getWmlObjectFactory();     //工厂类，可创建各种标签对象

        P p_bin = factory_bin.createP();
        BigInteger commentId_bin = BigInteger.valueOf(0); //从0开始

        CommentsPart commentsPart_bin = new CommentsPart();
        wordprocessingMLPackage_bin.getMainDocumentPart().addTargetPart(commentsPart_bin);  //wordMLPackage9 加上 cp
        Comments comments_bin = factory_bin.createComments();
        commentsPart_bin.setJaxbElement(comments_bin);
        //cyb


//        P p_bin = oldFactory.createP();
//        BigInteger commentId_bin = BigInteger.valueOf(0); //从0开始
//
//        CommentsPart commentsPart_bin = new CommentsPart();
//        wordMLPackageOld.getMainDocumentPart().addTargetPart(commentsPart_bin);  //wordMLPackage9 加上 cp
//        Comments comments_bin = oldFactory.createComments();
//        commentsPart_bin.setJaxbElement(comments_bin);


        //读取新文档的内容
        WordprocessingMLPackage wordMLPackageNew = WordprocessingMLPackage.load(new File(newPath));
        MainDocumentPart mainDocumentPartNew = wordMLPackageNew.getMainDocumentPart();
        ObjectFactory newFactory = Context.getWmlObjectFactory();
        List<Object> listNew = mainDocumentPartNew.getContent();



        //==========================对新旧文档内容进行对比======================

        //匹配规则
        Diff_match_patch dmp = new Diff_match_patch();


        //获取分块的信息
        Map<Integer, Block> oldBlockMap = getBlock(listOld);
        Map<Integer, Block> newBlockMap = getBlock(listNew);

//        System.out.println(oldBlockMap.get(1));

        int count = -1;
        int i = 0;
        int j = 0;
        int oldCount = oldBlockMap.size();
        int newCount = newBlockMap.size();
        if(oldCount >= newCount){
            count = newCount;
        }else{
            count = oldCount;
        }
        //用来保存中间结果
        HashMap<Integer, Integer> midResult = new HashMap<>();
        while (i<oldCount){
            //把一个块转化为字符串，并去除空格
            String oldBlockString = blockToBigString(oldBlockMap.get(i+1), listOld);
            //算出当前编号字符串的hash值
            BigInteger old64Hash = hashUnsigned(oldBlockString);
            while (j < newCount){
                String newBlockString = blockToBigString(newBlockMap.get(j+1), listNew);
                //算出当前编号字符串的hash值
                BigInteger new64Hash = hashUnsigned(newBlockString);
                if(old64Hash.equals(new64Hash)){  //如果两个块完全相同
                    //把结果先保存在中间结果map里面先
                    midResult.put(j+1,i+1);

                    Diff_match_patch.Diff mid =
                            new Diff_match_patch
                                    .Diff(Diff_match_patch.Operation.EQUAL,
                                    blockToBigStringHaving(oldBlockMap.get(i+1), listOld));

                    LinkedList<Diff_match_patch.Diff> hh = new LinkedList<>();
                    hh.add(mid);
                    System.out.println(hh);
                    addComment.initSameParagraph(mid.text,j+1,factory_bin,p_bin,mainDocumentPart_bin);
//                    commentId_bin=addComment.createComment(hh,
//                            wordprocessingMLPackage_bin,
//                            mainDocumentPart_bin,
//                            factory_bin,
//                            p_bin,
//                            commentId_bin,
//                            comments_bin,j+1);

                    count--;
                    j=0;
                    break;

                }else{  //不相同
                    j++;
                }
            }
            i++;
        }

        System.out.println(midResult);

        //把块Map里相同的块去掉
        if(0 != midResult.size()){
            for (Map.Entry<Integer,Integer> m: midResult.entrySet()) {
//                Diff_match_patch.Diff diff = new Diff_match_patch.Diff(Diff_match_patch.Operation.EQUAL,m.);
//                LinkedList<Diff_match_patch.Diff> objects = new LinkedList<>();
//                objects.add(diff);
//

                Integer key = m.getKey();  //key是新的
                Integer value = m.getValue();  //value是旧

                oldBlockMap.remove(m.getValue());
                newBlockMap.remove(m.getKey());
            }
        }

        if(oldBlockMap.size()==0 || newBlockMap.size()==0){
            if(oldBlockMap.size()==0) {
                System.out.println("用户提交的文档增加剩余的块");
            }else{
                System.out.println("用户提交的文档删除了剩余的块");
            }
        }else{


            //在两个文件剩下的块之中筛选出要对比的两块,并把块编号映射保存到map中
            //左边为旧文档对应的编号，右边为新的对应的编号
            HashMap<Integer, Integer> numTonum = new HashMap<>();
            Suggester suggester = new Suggester();
            //把块数多的放入suggester
            if((newBlockMap.size() >= oldBlockMap.size() ? newBlockMap : oldBlockMap) == newBlockMap){
                for (Map.Entry<Integer,Block> map: newBlockMap.entrySet()) {
                    String block = map.getValue().getKey()+blockToBigString(map.getValue(),listNew);
                    suggester.addSentence(block);
                }
                //拿块数少的来和多的比较
                for (Map.Entry<Integer,Block> map:oldBlockMap.entrySet()) {
                    String block = blockToBigString(map.getValue(), listOld);
                    List<String> suggest = suggester.suggest(block, 1);
                    numTonum.put(Integer.parseInt(String.valueOf(suggest.get(0).charAt(0))),map.getValue().getKey());
                }
            }else {
                for (Map.Entry<Integer,Block> map: oldBlockMap.entrySet()) {
                    String block = map.getValue().getKey()+blockToBigString(map.getValue(),listOld);
                    suggester.addSentence(block);
                }
                //拿块数少的来和多的比较
                for (Map.Entry<Integer,Block> map:newBlockMap.entrySet()) {
                    String block = blockToBigString(map.getValue(), listNew);
                    List<String> suggest = suggester.suggest(block, 1);
                    numTonum.put(map.getValue().getKey(),Integer.parseInt(String.valueOf(suggest.get(0).charAt(0))));
                }
            }
            System.out.println(numTonum);
            for (Map.Entry<Integer,Integer> entry:numTonum.entrySet()) {
                Integer newOrder = entry.getKey();
                Integer oldOrder = entry.getValue();
                String s1 = blockToBigString(oldBlockMap.get(oldOrder), listOld);
                String s2 = blockToBigString(newBlockMap.get(newOrder), listNew);
                LinkedList<Diff_match_patch.Diff> t = dmp.diff_main(s1,s2);
                t.get(0).setOldOrder(oldOrder);
                t.get(0).setNewOrder(newOrder);
                System.out.println("====================================================");
                System.out.println("新文档第"+ newOrder +"块"+"和旧文档第"+ oldOrder + "块比较");
                System.out.println(t);
                System.out.println("============================================");

                //cyb
//                commentId_bin=addComment.createComment(t,
//                        wordprocessingMLPackage_bin,
//                        mainDocumentPart_bin,
//                        factory_bin,
//                        p_bin,
//                        commentId_bin,
//                        comments_bin,newOrder);
//cyb

            }
            if(oldBlockMap.size() >= newBlockMap.size()){  //新文档较源文档删除了原文档那些块
                if(oldBlockMap.size() == newBlockMap.size()){

                }else {
                    for (Map.Entry<Integer, Block> entry : oldBlockMap.entrySet()) {
                        if (!numTonum.containsKey(entry.getKey())) {

                            System.out.println("新文档较旧文档删除了原文的第" + entry.getKey() + "块");

                            String oldstr = blockToBigString(entry.getValue(), listOld);
                            Diff_match_patch.Diff diff1 = new Diff_match_patch.Diff(Diff_match_patch.Operation.OLDSURPLUS,oldstr);
                            LinkedList<Diff_match_patch.Diff> dd = new LinkedList<>();
                            dd.add(diff1);
                            System.out.println(dd);
                            System.out.println(">>>>>>>>>>>>>>>>>我是分割线>>>>>>>>>>>>");

//                            commentId_bin=addComment.createComment(dd,wordprocessingMLPackage_bin,
//                                    mainDocumentPart_bin,factory_bin,p_bin,commentId_bin,comments_bin,entry.getKey());

                        }
                    }
                }
            } else{   //新文档较源文档增加了那些块

                for (Map.Entry<Integer, Block> entry : newBlockMap.entrySet()) {
                    Integer key = entry.getKey();
                    int tag = -1;
                    for (Map.Entry<Integer,Integer> num:numTonum.entrySet()) {
                        if(num.getValue().equals(key)){
                            tag = 0;
                        }
                    }
                    if(tag == -1){

                        System.out.println("新文档较旧文档增加了第" + key + "块");
                        String newStr = blockToBigString(entry.getValue(), listNew);
                        LinkedList<Diff_match_patch.Diff> uu = new LinkedList<>();
                        Diff_match_patch.Diff diff2 = new Diff_match_patch.Diff(Diff_match_patch.Operation.NEWSURPLUS,newStr);
                        uu.add(diff2);
                        System.out.println(diff2);
                        System.out.println(uu);
                        System.out.println(">>>>>>>>>>>>>>>>>我是分割线>>>>>>>>>>>>");
//                        commentId_bin=addComment.createComment(uu,
//                                wordprocessingMLPackage_bin,
//                                mainDocumentPart_bin,factory_bin,p_bin,commentId_bin,comments_bin,key);

                    }
                }
            }



//            //找到两个文本之间的差异
//            int a = oldBlockMap.size()>=newBlockMap.size() ? newBlockMap.size():oldBlockMap.size();
//            for(int b = 0;b<a;b++){
//                String s1 = blockToBigString(oldBlockMap.get(b+1), listOld);
//                String s2 = blockToBigString(newBlockMap.get(b+1), listNew);
//                LinkedList<Diff_match_patch.Diff> t = dmp.diff_main(s1,s2);
//
//
//                for (Diff_match_patch.Diff diff : t) {
//                    Vo v = new Vo();
//                    v.setKey(diff.operation.toString());
//                    v.setValue(diff.text);
//                    diffList.add(v);
//                    System.out.println("first:key="+diff.operation.toString() + ",value="+diff.text);
//                }
//            }
        }


        //cyb
        addComment.saveComment(wordprocessingMLPackage_bin,out);
        //cyb



//        //保存文档
//        try {
//            writeDocxToStream(wordMLPackageOld, out);
//            System.out.println("已经把结果存到新文件了");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


//        //遍历旧的每一个块并转化为字符串
//        for (Map.Entry<Integer, Block> entry : oldBlockMap.entrySet()) {
//            //当前字符串所属于的编号
//            int oldBlockId = entry.getKey();
//            //把一个块转化为字符串，并去除空格
//            String oldBlockString = blockToBigString(entry.getValue(), listOld);
//            //算出当前编号字符串的hash值
//            BigInteger old64Hash = hashUnsigned(oldBlockString);
//
//        }
//        //遍历新的每一个块并转化为字符串
//        for (Map.Entry<Integer, Block> entry : newBlockMap.entrySet()) {
//            //当前字符串所属于的编号
//            int newBlockId = entry.getKey();
//            String newBlockString = blockToBigString(entry.getValue(), listNew);
//            //算出当前编号字符串的hash值
//            BigInteger new64Hash = hashUnsigned(newBlockString);
//        }

    }



    //在两个文件剩下的块之中筛选出要对比的两块,并把块编号映射保存到map中
    private HashMap<Integer,Integer> selectToCompare(Map<Integer,Block> newBlockMap,Map<Integer,Block> oldBlockMap,List listNew,List listOld){
        HashMap<Integer,Integer> numTonum = new HashMap<>();

        Suggester suggester = new Suggester();

        //把块数多的放入suggester
        if((newBlockMap.size() >= oldBlockMap.size() ? newBlockMap : oldBlockMap) == newBlockMap){
            for (Map.Entry<Integer,Block> map: newBlockMap.entrySet()) {
                String block = map.getValue().getKey()+blockToBigString(map.getValue(),listNew);
                suggester.addSentence(block);
            }
            //拿块数少的来和多的比较
            for (Map.Entry<Integer,Block> map:oldBlockMap.entrySet()) {
                String block = blockToBigString(map.getValue(), listOld);
                List<String> suggest = suggester.suggest(block, 1);
                numTonum.put(map.getValue().getKey(),Integer.parseInt(String.valueOf(suggest.get(0).charAt(0))));
            }
        }else {
            for (Map.Entry<Integer,Block> map: oldBlockMap.entrySet()) {
                String block = map.getValue().getKey()+blockToBigString(map.getValue(),listOld);
                suggester.addSentence(block);
            }
            //拿块数少的来和多的比较
            for (Map.Entry<Integer,Block> map:newBlockMap.entrySet()) {
                String block = blockToBigString(map.getValue(), listNew);
                List<String> suggest = suggester.suggest(block, 1);
                numTonum.put(map.getValue().getKey(),Integer.parseInt(String.valueOf(suggest.get(0).charAt(0))));
            }
        }

        return numTonum;
    }


    /*
    * 移除Hash相同的块，返回新旧
    * */


//    private void getContent(String path1, String path2) throws Docx4JException {
//
//        String out = "F:/test/out.docx";
//
//        //读取旧文档的内容
//        WordprocessingMLPackage wordMLPackageOld = WordprocessingMLPackage.load(new File(path1));
//        MainDocumentPart mainDocumentPartOld = wordMLPackageOld.getMainDocumentPart();
//        List<Object> listOld = mainDocumentPartOld.getContent();
//        System.out.println(getLineNumber(listOld));
//        Map<Integer, Block> blockMap1 = getBlock(listOld);  //获取块
//        String string1 = blockToBigString(blockMap1.get(2), listOld);
//
////        for(Object o:listOld){
////
////                System.out.println(o);
////
////        }
////        Map<Integer, Block> result = getBlock(listOld);
////        for (Map.Entry<Integer, Block> entry : result.entrySet()) {
////            //Map.entry<Integer,String> 映射项（键-值对）  有几个方法：用上面的名字entry
////            //entry.getKey() ;entry.getValue(); entry.setValue();
////            //map.entrySet()  返回此映射中包含的映射关系的 Set视图。
////            System.out.println("key= " + entry.getKey() + " and value= "
////                    + entry.getValue());
////        }
//
//        //读取新文档的内容
//        WordprocessingMLPackage wordMLPackageNew = WordprocessingMLPackage.load(new File(path2));
//        MainDocumentPart mainDocumentPartNew = wordMLPackageNew.getMainDocumentPart();
//        List<Object> listNew = mainDocumentPartNew.getContent();
//        System.out.println(getLineNumber(listNew));
//        Map<Integer, Block> blockMap2 = getBlock(listNew);  //获取块
//        String string2 = blockToBigString(blockMap2.get(2), listNew);
//
//
//        //=========================比较相似度start===================================
////
////        double similarity = SimilarityUtil.getSimilarity(string1, string2);
////        System.out.println(similarity);
//
//        //============================相似度比较end================================
//
//
//        //===============================匹配规则===============================
//
//        //匹配规则
//        Diff_match_patch dmp = new Diff_match_patch();
//        //找到两个文本之间的差异
//        LinkedList<Diff_match_patch.Diff> t = dmp.diff_main(string1.toString(), string2.toString());
//
//        System.out.println(t);
//
//        //================================匹配end=============================
//
//        //===========================测试代码=============
////        P p0 = (P) list.get(1);
////        List<Object> content = p0.getContent();
////        R r = (R) content.get(0);
////        int pContentSize = content.size();
////        RPr fontRPr = new RPr();
////        fontRPr = r.getRPr();
////        System.out.println(fontRPr);
//
//
////        遍历每一行的内容，去掉每行为空的行
////        for(Object o:listOld){
////            if(!"".equals(o.toString().trim())){
////                P p0 = (P) o;
////                String s0 = p0.toString();//把一个段落对象变成字符串
////                //去除字符串里面的空格
////                s0 = s0.replace(" ", "");
////                System.out.println(s0);
////                //匹配规则
//////                Diff_match_patch dmp = new Diff_match_patch();
//////                LinkedList<Diff> t = dmp.diff_main(s0,s1);
////            }
////        }
//
//        //==============================end==============
//
//
//        //保存文档
//        try {
//            writeDocxToStream(wordMLPackageOld, out);
//            System.out.println("已经把结果存到新文件了");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


    //把结果输出到新文档
    private void writeDocxToStream(WordprocessingMLPackage wordMLPackageOld,
                                   String target) throws IOException, Docx4JException {
        File f = new File(target);
        wordMLPackageOld.save(f);
    }


//    /*
//    * 去除文档的空行
//    * */
//    private void removeNullLine(List list){
//        //遍历每一行的内容，去掉每行为空的行
//        for(Object o:list){
//            if(!"".equals(o.toString().trim())){  //不是空行
//                System.out.println(o);
//            }
//        }
//
//    }



    //返回有内容的行数
    private int getLineNumber(List list) {
        int count = list.size();
        //遍历每一行的内容，去掉每行为空的行
        for (Object o : list) {
            if ("".equals(o.toString().trim())) {
                count -= 1;
            }
        }
        return count;
    }

    //分块
    private Map<Integer, Block> getBlock(List list) {
        if (null == list) return null;

        int i = 0;
        int key = 1;

        int tag = -1;  //标志位，为1时则还在标记，为-1时则标记完成

        Map<Integer, Block> map = new HashMap<Integer, Block>();
        Block block = new Block();
        int size = list.size();
        while (i < size) {
            if (tag == -1 && null == block) block = new Block();
            if ("".equals(list.get(i).toString().trim())) {  //如果这行为空行
                if (tag == 1) {
                    tag = -1;
                    block.setLast(i);
                    block.setKey(key);
                    map.put(key, block);
                    block = null;
                    key++;
                    i++;
                } else {
                    i++;
                }
            } else { //如果这行不为空行
                if (tag == 1) {  //正在标记
                    if (i == size - 1) {  //如果此时为最后一行，则标记结束
                        tag = -1;
                        block.setLast(i + 1);
                        block.setKey(key);
                        map.put(key, block);
                        block = null;
                        key++;
                        i++;
                    } else {
                        i++;
                    }
                } else {  //没有开始标记
                    if (i == size - 1) {
                        block.setFirst(i + 1);
                        block.setLast(i + 1);
                        block.setKey(key);
                        map.put(key, block);
                        block = null;
                        key++;
                        i++;
                    } else {
                        tag = 1;
                        block.setFirst(i + 1);
                        i++;
                    }
                }
            }
        }

//        while(i < size){
//            if(tag == -1) block = new Block();
//
//            if("".equals(list.get(i).toString().trim()) && tag == -1){  //如果这行为空行
//                i++;
//            }
//            if(!"".equals(list.get(i).toString().trim()) && tag == -1){  //如果这行不为空,同时还未开始标记
//                tag = 1;
//                block.setFirst(i+1);
//                i++;
//            }
//            if(!"".equals(list.get(i).toString().trim()) && tag == 1) {  //如果这行不为空,同时还在标记
//                if(i == size-1){  //如果此时为最后一行，则标记结束
//                    tag = -1;
//                    block.setLast(i+1);
//                    map.put(key+1,block);
//                    block = null;
//                    i++;
//                }else {
//                    i++;
//                }
//            }
//            if("".equals(list.get(i).toString().trim()) && tag == 1){  //如果这行为空行,且还在标记
//                tag = -1;
//                block.setLast(i);
//                map.put(key+1,block);
//                block = null;
//                i++;
//            }
//
//        }
        if (null != block) {
            block = null;
        }
        return map;
    }


//    //统计每行有多少空格，放进list
//    public static List<Integer> rememberSpacing(String str){
//        List<Integer> list = new ArrayList<Integer>();
//        for(int i = 0;i < str.length();i++){
//            if(' '==str.charAt(i)){
//                list.add(i);
//            }
//        }
//        return list;
//    }


    //hanpl最短路分词
    private List<Term> shortSegment(String text) {
        Segment shortestSegment = new DijkstraSegment().enableCustomDictionary(false).enablePlaceRecognize(true).enableOrganizationRecognize(true);
        return shortestSegment.seg(text);
    }

    //把标点去除，并且
    private List<String> getSplitWords(String sentence) {
        // 标点符号会被单独分为一个Term，去除之
        return HanLP.segment(sentence).stream().map(a -> a.word).filter(s -> !"`~!@#$^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘’；：”“'。，、？".contains(s)).collect(Collectors.toList());
    }

    //统计词频

    //求simhash

    //加权
    //合并


    /*
     * 把一个块转化为字符串，保留空格
     * */
    private String blockToBigStringHaving(Block block, List list) {
        Integer a = block.getFirst();
        Integer b = block.getLast();
        if (null == a || null == b) return null;
        int first = a.intValue();
        int last = b.intValue();
        StringBuffer sBuffer = new StringBuffer();
        while (first <= last) {
            if (first == last) {
                sBuffer.append(list.get(first - 1).toString());
                first++;
            } else {
                sBuffer.append(list.get(first - 1).toString());
                first++;
            }
        }
        return sBuffer.toString();
    }


    /*
     * 把一个块转化为字符串，并去除空格
     * */
    private String blockToBigString(Block block, List list) {
        Integer a = block.getFirst();
        Integer b = block.getLast();
        if (null == a || null == b) return null;
        int first = a.intValue();
        int last = b.intValue();
        StringBuffer sBuffer = new StringBuffer();
        while (first <= last) {
            if (first == last) {
                sBuffer.append(list.get(first - 1).toString().replace(" ", ""));
                first++;
            } else {
                sBuffer.append(list.get(first - 1).toString().replace(" ", ""));
                first++;
            }
        }
        return sBuffer.toString();
    }

    /*
     * 把一个块转化为字符串数字，块内的每一行是数组的其中一个元素
     * */
    private String[] blockToArray(Block block, List list) {
        Integer a = block.getFirst();
        Integer b = block.getLast();
        if (null == a || null == b) return null;
        int first = a.intValue();
        int last = b.intValue();
        int lenth = last - first + 1;
        int i = 0;
        String[] strings = new String[lenth];
        while (first <= last) {
            if (first == last) {
                strings[i] = list.get(first - 1).toString();
                first++;
            } else {
                strings[i] = list.get(first - 1).toString();
//                if(first == last) strings[last] = list.get(last).toString();
                i++;
                first++;
            }
        }
        return strings;
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


    //========================批准相关===================================
    public void createComment
    (WordprocessingMLPackage wordMLPackage,
     MainDocumentPart t, ObjectFactory
             factory,P p,RPr fontRPr,
     List<Vo> list,
     Comments comments)
            throws Exception {
//      setParagraphSpacing(factory, p, true, "0", "0", true, null, "100", true, "240", STLineSpacingRule.AUTO);
//      t.addObject(p);
//      RPr fontRPr = getRPrStyle(factory, "微软雅黑", "000000", "20", STHint.EAST_ASIA, false, false, false, true,
//              UnderlineEnumeration.SINGLE, "B61CD2", true, "darkYellow", false, null, null, null);
        RPr commentRPr = getRPrStyle(factory, "微软雅黑", "41A62D", "18", STHint.EAST_ASIA, true, true, false, false, null,
                null, false, null, false, null, null, null);
//      Comments comments = addDocumentCommentsPart(wordMLPackage, factory);

        createCommentRound(factory, p, fontRPr, comments, list);
    }

    // 创建批注(选定范围)
    public void createCommentRound(ObjectFactory factory, P p, RPr fontRPr,
                                   Comments comments, List<Vo> list) throws Exception {
        for(int i=0;i<list.size();i++) {  //遍历批注
            Vo v = list.get(i);  //得到每一个批注
            if("EQUAL".equals(v.getKey())){
                R run = factory.createR();
                Text txt = factory.createText();
                txt.setValue(v.getValue());
                run.getContent().add(txt);
                run.setRPr(fontRPr);
                p.getContent().add(run);
            }else if("INSERT".equals(v.getKey())){
                CommentRangeStart startComment = factory.createCommentRangeStart();
                startComment.setId(commentId);
                p.getContent().add(startComment);
                R run = factory.createR();
                Text txt = factory.createText();
                txt.setValue(v.getValue());
                run.getContent().add(txt);
                run.setRPr(fontRPr);
                p.getContent().add(run);
                CommentRangeEnd endComment = factory.createCommentRangeEnd();
                endComment.setId(commentId);
                p.getContent().add(endComment);
                RPr commentRPr = getRPrStyle(factory, "微软雅黑", "41A62D", "18", STHint.EAST_ASIA, true, true, false, false, null,
                        null, false, null, false, null, null, null);
                Comments.Comment commentOne = createComment(factory, commentId, "系统管理员", new Date(), "新增："+v.getValue(), commentRPr);
                comments.getComment().add(commentOne);
                p.getContent().add(createRunCommentReference(factory, commentId));
            }else if("DELETE".equals(v.getKey()) && ((list.size()-1)==i ||"EQUAL".equals(list.get(i+1).getKey()))){
                CommentRangeStart startComment = factory.createCommentRangeStart();
                startComment.setId(commentId);
                p.getContent().add(startComment);
                R run = factory.createR();
                Text txt = factory.createText();
                txt.setValue(v.getValue());
                run.getContent().add(txt);
                run.setRPr(fontRPr);
                p.getContent().add(run);
                CommentRangeEnd endComment = factory.createCommentRangeEnd();
                endComment.setId(commentId);
                p.getContent().add(endComment);
                RPr commentRPr = getRPrStyle(factory, "微软雅黑", "FF0000", "18", STHint.EAST_ASIA, true, true, false, false, null,
                        null, false, null, false, null, null, null);
                Comments.Comment commentOne = createComment(factory, commentId, "系统管理员", new Date(), "删除："+v.getValue(), commentRPr);
                comments.getComment().add(commentOne);
                p.getContent().add(createRunCommentReference(factory, commentId));
            }else if("DELETE".equals(v.getKey()) && ((list.size()-1)==i || "INSERT".equals(list.get(i+1).getKey()))){
                CommentRangeStart startComment = factory.createCommentRangeStart();
                startComment.setId(commentId);
                p.getContent().add(startComment);
                R run = factory.createR();
                Text txt = factory.createText();
                txt.setValue(v.getValue());
                run.getContent().add(txt);
                run.setRPr(fontRPr);
                p.getContent().add(run);
                CommentRangeEnd endComment = factory.createCommentRangeEnd();
                endComment.setId(commentId);
                p.getContent().add(endComment);
                RPr commentRPr = getRPrStyle(factory, "微软雅黑", "9932CC", "18", STHint.EAST_ASIA, true, true, false, false, null,
                        null, false, null, false, null, null, null);
                Comments.Comment commentOne = createComment(factory, commentId, "系统管理员", new Date(), "替换："+list.get(i+1).getValue(), commentRPr);
                comments.getComment().add(commentOne);
                p.getContent().add(createRunCommentReference(factory, commentId));
                i++;
            }
            if(!"EQUAL".equals(v.getKey())){
                commentId = commentId.add(BigInteger.ONE);
            }
        }
    }

    private Comments addDocumentCommentsPart(WordprocessingMLPackage wordMLPackage, ObjectFactory factory)
            throws Exception {
        CommentsPart cp = new CommentsPart();
        wordMLPackage.getMainDocumentPart().addTargetPart(cp);
        Comments comments = factory.createComments();
        cp.setJaxbElement(comments);
        return comments;
    }

    public Comments.Comment createComment(ObjectFactory factory, BigInteger commentId, String author, Date date,
                                          String commentContent, RPr commentRPr) throws Exception {
        Comments.Comment comment = factory.createCommentsComment();
        comment.setId(commentId);
        if (author != null) {
            comment.setAuthor(author);
        }
        if (date != null) {
            comment.setDate(toXMLCalendar(date));
        }
        P commentP = factory.createP();
        comment.getEGBlockLevelElts().add(commentP);
        R commentR = factory.createR();
        commentP.getContent().add(commentR);
        Text commentText = factory.createText();
        commentR.getContent().add(commentText);
        commentR.setRPr(commentRPr);
        commentText.setValue(commentContent);
        return comment;
    }

    public R createRunCommentReference(ObjectFactory factory, BigInteger commentId) {
        R run = factory.createR();
        R.CommentReference commentRef = factory.createRCommentReference();
        run.getContent().add(commentRef);
        commentRef.setId(commentId);
        return run;
    }

    public XMLGregorianCalendar toXMLCalendar(Date d) throws Exception {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(d);
        XMLGregorianCalendar xml = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        xml.setYear(gc.get(Calendar.YEAR));
        xml.setMonth(gc.get(Calendar.MONTH) + 1);
        xml.setDay(gc.get(Calendar.DAY_OF_MONTH));
        xml.setHour(gc.get(Calendar.HOUR_OF_DAY));
        xml.setMinute(gc.get(Calendar.MINUTE));
        xml.setSecond(gc.get(Calendar.SECOND));
        return xml;
    }

    // 字体样式
    public static RPr getRPrStyle(ObjectFactory factory, String fontFamily, String colorVal, String fontSize, STHint sTHint,
                                  boolean isBlod, boolean isItalic, boolean isStrike, boolean isUnderLine,
                                  UnderlineEnumeration underLineStyle, String underLineColor, boolean isHightLight, String hightLightValue,
                                  boolean isShd, STShd shdValue, String shdColor, CTVerticalAlignRun stRunEnum) {
        RPr rPr = factory.createRPr();
        RFonts rf = new RFonts();
        if (sTHint != null) {
            rf.setHint(sTHint);
        }
        if (fontFamily != null) {
            rf.setAscii(fontFamily);
            rf.setEastAsia(fontFamily);
            rf.setHAnsi(fontFamily);
        }
        rPr.setRFonts(rf);
        if (colorVal != null) {
            Color color = new Color();
            color.setVal(colorVal);
            rPr.setColor(color);
        }
        if (fontSize != null) {
            HpsMeasure sz = new HpsMeasure();
            sz.setVal(new BigInteger(fontSize));
            rPr.setSz(sz);
            rPr.setSzCs(sz);
        }

        BooleanDefaultTrue bdt = factory.createBooleanDefaultTrue();
        if (isBlod) {
            rPr.setB(bdt);
        }
        if (isItalic) {
            rPr.setI(bdt);
        }
        if (isStrike) {
            rPr.setStrike(bdt);
        }
        if (isUnderLine) {
            U underline = new U();
            if (underLineStyle != null) {
                underline.setVal(underLineStyle);
            }
            if (underLineColor != null) {
                underline.setColor(underLineColor);
            }
            rPr.setU(underline);
        }
        if (isHightLight) {
            Highlight hight = new Highlight();
            hight.setVal(hightLightValue);
            rPr.setHighlight(hight);
        }
        if (isShd) {
            CTShd shd = new CTShd();
            if (shdColor != null) {
                shd.setColor(shdColor);
            }
            if (shdValue != null) {
                shd.setVal(shdValue);
            }
            rPr.setShd(shd);
        }
        if (stRunEnum != null) {
            rPr.setVertAlign(stRunEnum);
        }
        return rPr;
    }

    private String getFileMD5(String filePath) throws Exception{
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


}
