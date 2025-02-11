package cn.hfbin.seckill.mq;

import cn.hfbin.seckill.bo.GoodsBo;
import cn.hfbin.seckill.entity.OrderInfo;
import cn.hfbin.seckill.entity.SeckillOrder;
import cn.hfbin.seckill.entity.User;
import cn.hfbin.seckill.enums.OrderStatusEnum;
import cn.hfbin.seckill.redis.*;
import cn.hfbin.seckill.service.OrderService;
import cn.hfbin.seckill.service.SeckillGoodsService;
import cn.hfbin.seckill.service.SeckillOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MQReceiver {

    private static final Logger log = LoggerFactory.getLogger(MQReceiver.class);

    private static final Integer MAX_RETRY_COUNT = 3;
    private static final Integer RETRY_INTERVAL = 10;

    @Autowired
    RedisService redisService;

    @Autowired
    SeckillGoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    SeckillOrderService seckillOrderService;



    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void receive(String message) {
        // todo 如果这里出现异常可以进行补偿，重试，重新执行此逻辑，如果超过一定次数还是失败可以将此秒杀置为无效，恢复redis库存
        log.info("receive message:" + message);
        SeckillMessage mm = RedisService.stringToBean(message, SeckillMessage.class);
        User user = mm.getUser();
        long goodsId = mm.getGoodsId();

        GoodsBo goods = goodsService.getseckillGoodsBoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if (stock <= 0) {
            return;
        }
        //判断是否已经秒杀到了,幂等性判断
//        消费端幂等性判断两种方式：
//        1.阻止第二次消息的消费，redis+消息id或者其它方式
//        2.重复消费一遍对结果没影响，这里数据库对UserId和goodsId做了唯一性约束，就算再执行一遍也无所谓
        SeckillOrder order = seckillOrderService.getSeckillOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            return;
        }

        //减库存 下订单 写入秒杀订单
        seckillOrderService.insert(user, goods);
    }

    @RabbitListener(queues = MQConfig.MIAOSHA_DEAD_LETTER_QUEUE)
    public void receiveDeadLetter(String message) {
        log.debug("死信队列收到了消息{}, message:{}", Thread.currentThread().getName(), message);
        SeckillMessage mm = RedisService.stringToBean(message, SeckillMessage.class);
        try {
            retryProcess(mm);
        } catch (Exception e) {
            log.error("Failed to process dead letter message: " + message, e);
//            1.出现异常说明此用户秒杀商品失败，将该次秒杀设置为无效，并回复redis库存
            invalidSeckill(mm);
        }

    }

    private void retryProcess(SeckillMessage mm) {
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                // 处理逻辑
                processMessage(mm);
                return; // 如果成功则跳出
            } catch (Exception e) {
                retryCount++;
                log.warn("Retrying processing message, attempt {}/{}", retryCount, MAX_RETRY_COUNT);
                if (retryCount == MAX_RETRY_COUNT) {
                    log.error("Exceeded maximum retries, marking message as invalid.");
                    invalidSeckill(mm);
                }
                try {
                    Thread.sleep(RETRY_INTERVAL); // 等待一段时间再重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processMessage(SeckillMessage mm) {
        User user = mm.getUser();
        long goodsId = mm.getGoodsId();
        GoodsBo goods = goodsService.getseckillGoodsBoByGoodsId(goodsId);
        //减库存 下订单 写入秒杀订单
        seckillOrderService.insert(user, goods);
    }

    @RabbitListener(queues = MQConfig.SECKILL_CANCLE_ORDER_QUEUE)
    public void receiveCancleOrderDeadLetter(String message) {
        log.debug("死信队列收到了取消订单的消息{}, message:{}", Thread.currentThread().getName(), message);
        CancleSeckillOrderMessage mm = RedisService.stringToBean(message, CancleSeckillOrderMessage.class);

        long userId = mm.getUserId();
        long goodsId = mm.getGoodsId();
        String path = mm.getPath();
//        先获取订单，如果订单已经支付了，就不取消了
//        OrderInfo orderInfo = orderService.getOrderInfo(userId, goodsId);
//        if (!orderInfo.getStatus().equals(0)) {
//            return;
//        }
        //        1.去数据库取消订单//        幂等性判断：具备天然的幂等性

        int row = orderService.deleteSeckillOrder(userId, goodsId, OrderStatusEnum.NO_PAY.getCode());
        if (row == 0) {
//            可能用户已经支付了
            log.debug("mq收到取消订单的消息，但是用户极限支付了");
            return;
        }
        seckillOrderService.delete(userId, goodsId);
//        2.使这次秒杀无效化
        User user = new User();
        user.setId((int) userId);
        invalidSeckill(new SeckillMessage(user, goodsId, path));
    }

    private void invalidSeckill(SeckillMessage mm) {
        User user = mm.getUser();
        long goodsId = mm.getGoodsId();
        String oldPath = mm.getPath();


        if (oldPath != null && !oldPath.isEmpty()) {
//        删除该用户的秒杀凭证，该用户可以再次秒杀，这里需要判断删除的用户凭证是否是这一次生成的，
//            String path = redisService.get(SeckillKey.getSeckillPath, "" + user.getId() + "_" + goodsId, String.class);
//            if (oldPath.equals(path)) {
            redisService.del(SeckillKey.getSeckillPath, "" + user.getId() + "_" + goodsId);
        }
//        恢复库存  需要保证幂等性,设置10分钟，防止同一登录凭证重复投喂
        boolean lock = redisService.getLock(StoreStockKey.storeStockKey, user.getId() + "_"
                + goodsId + "_" + oldPath, "1", 10, TimeUnit.MINUTES);
        if (lock) {
            long stock = redisService.incr(GoodsKey.getSeckillGoodsStock, String.valueOf(goodsId));
        }

        //      最后删除用户的分布式锁,使用户可以再次秒杀
        redisService.deleteLock(OrderPrefix.OrderPrefix, user.getId() + "_" + goodsId);

    }


}
