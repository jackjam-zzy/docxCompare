package com.zzy.compare;

import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        HashMap<String, String> map = new HashMap<>();
        map.put("1","zheng");
        map.put("2","huang");
//        map.put("3","nihao");
//        map.put("4","yuan");
        System.out.println(map);
        System.out.println(map.size());
        map.remove("1");
        map.remove("2");
        System.out.println(map);
        System.out.println(map.size());

    }
}
