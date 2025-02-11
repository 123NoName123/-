package cn.hfbin.seckill.service;

import cn.hfbin.seckill.entity.OrderInfo;

/**
 * Created by: HuangFuBin
 * Date: 2018/7/17
 * Time: 10:49
 * Such description:
 */
public interface OrderService {

    long addOrder(OrderInfo orderInfo);

    OrderInfo getOrderInfo(long rderId);


    int deleteSeckillOrder(long userId, long goodsId, Integer code);


    OrderInfo getOrderInfo(long userId, long goodsId);

    int update(OrderInfo orderInfo, int status);
}
