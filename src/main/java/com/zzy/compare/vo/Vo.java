package com.zzy.compare.vo;

/*
* 这是批注的属性
* key : 有三个值  新增，修改，删除
* value ： 新增了啥，修改了啥，删除了啥。
* */
public class Vo {
    private String key;

    private String value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
