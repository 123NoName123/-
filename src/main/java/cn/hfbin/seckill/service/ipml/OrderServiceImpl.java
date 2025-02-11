package cn.hfbin.seckill.service.ipml;

import cn.hfbin.seckill.dao.OrdeInfoMapper;
import cn.hfbin.seckill.entity.OrderInfo;
import cn.hfbin.seckill.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by: HuangFuBin
 * Date: 2018/7/17
 * Time: 10:50
 * Such description:
 */
@Service("orderService")
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrdeInfoMapper ordeInfoMapper;

    @Override
    public long addOrder(OrderInfo orderInfo) {
        return ordeInfoMapper.insertSelective(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(long orderId) {
        return ordeInfoMapper.selectByPrimaryKey(orderId);
    }


    @Override
    public int deleteSeckillOrder(long userId, long goodsId, Integer code) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId( userId);
        orderInfo.setGoodsId(goodsId);
        orderInfo.setSeckillOrder(1);
        orderInfo.setStatus(code);
        int row = ordeInfoMapper.delete(orderInfo);
        return row;
    }

    @Override
    public OrderInfo getOrderInfo(long userId, long goodsId) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId( userId);
        orderInfo.setGoodsId(goodsId);
        return ordeInfoMapper.select(orderInfo);
    }

    @Override
    public int update(OrderInfo orderInfo, int status) {
        int row = ordeInfoMapper.update(orderInfo, status);
        return row;
    }
}
