package com.zzy.compare.msg;

import java.math.BigInteger;

public class Block {
    private Integer first;
    private Integer last;
    private BigInteger hashCode;

    public Integer getFirst() {
        return first;
    }

    public void setFirst(Integer first) {
        this.first = first;
    }

    public Integer getLast() {
        return last;
    }

    public void setLast(Integer last) {
        this.last = last;
    }

    public BigInteger getHashCode() {
        return hashCode;
    }

    public void setHashCode(BigInteger hashCode) {
        this.hashCode = hashCode;
    }

    @Override
    public String toString() {
        return "Block{" +
                "first=" + first +
                ", last=" + last +
                ", hashCode=" + hashCode +
                '}';
    }
}
