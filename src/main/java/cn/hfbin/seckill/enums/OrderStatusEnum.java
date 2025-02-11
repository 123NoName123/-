package cn.hfbin.seckill.enums;

public enum OrderStatusEnum {
//订单状态：0 未支付，1已支付，2 已发货，3 已收货，4 已退款，‘5 已完成
    NO_PAY(0, "未支付"),
    HAVED_PAIED(1, "已支付"),
    HAVED_SENT(2, "已发货"),
    HAVED_RECEIVED(3, "已收货"),
    HAVED_REFUNDED(4, "已退款"),
    HAVED_FINISHED(5, "已完成");

    private int code;
    private String desc;
    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public String getDesc() {
        return desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }

}
