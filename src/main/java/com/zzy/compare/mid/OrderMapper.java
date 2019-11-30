package com.zzy.compare.mid;

public class OrderMapper {
    /*
    * 旧文档对应的编号
    * */
    private Integer oldOrder;


    /*
    * 新文档对应的编号
    * */
    private Integer newOrder;

    public OrderMapper(Integer oldOrder, Integer newOrder) {
        this.oldOrder = oldOrder;
        this.newOrder = newOrder;
    }

    public Integer getOldOrder() {
        return oldOrder;
    }

    public void setOldOrder(Integer oldOrder) {
        this.oldOrder = oldOrder;
    }

    public Integer getNewOrder() {
        return newOrder;
    }

    public void setNewOrder(Integer newOrder) {
        this.newOrder = newOrder;
    }

    @Override
    public String toString() {
        return "OrderMapper{" +
                "oldOrder=" + oldOrder +
                ", newOrder=" + newOrder +
                '}';
    }
}
