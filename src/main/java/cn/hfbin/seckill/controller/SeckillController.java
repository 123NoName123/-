package cn.hfbin.seckill.controller;

import cn.hfbin.seckill.annotations.AccessLimit;
import cn.hfbin.seckill.bo.GoodsBo;
import cn.hfbin.seckill.bo.RedisGoodsBo;
import cn.hfbin.seckill.common.Const;
import cn.hfbin.seckill.entity.OrderInfo;
import cn.hfbin.seckill.entity.SeckillGoods;
import cn.hfbin.seckill.entity.SeckillOrder;
import cn.hfbin.seckill.entity.User;
import cn.hfbin.seckill.mq.MQReceiver;
import cn.hfbin.seckill.mq.MQSender;
import cn.hfbin.seckill.mq.SeckillMessage;
import cn.hfbin.seckill.redis.*;
import cn.hfbin.seckill.result.CodeMsg;
import cn.hfbin.seckill.result.Result;
import cn.hfbin.seckill.service.SeckillGoodsService;
import cn.hfbin.seckill.service.SeckillOrderService;
import cn.hfbin.seckill.util.CookieUtil;
import cn.hfbin.seckill.util.JsonUtil;
import cn.hfbin.seckill.vo.OrderDetailVo;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by: HuangFuBin
 * Date: 2018/7/15
 * Time: 23:55
 * Such description:
 */
@Controller
@RequestMapping("seckill")
public class SeckillController implements InitializingBean {


    private static final Logger log = LoggerFactory.getLogger(SeckillController.class);
    @Autowired
    RedisService redisService;

    @Autowired
    SeckillGoodsService seckillGoodsService;

    @Autowired
    SeckillOrderService seckillOrderService;

    @Autowired
    MQSender mqSender;


    /**
     * 如果是集群情况下，需要达到一定量此缓存才能起到重大作用
     */
    private final HashMap<Long, Boolean> localOverMap = new HashMap<Long, Boolean>();

    /**
     * 将库存初始化到本地缓存及redis缓存，原则上次块应该在创建秒杀活动时候触发的（为了演示，此项目没有创建活动逻辑，所有放在启动项目时候放进内存）
     */
    public void afterPropertiesSet() throws Exception {
        List<GoodsBo> goodsList = seckillGoodsService.getSeckillGoodsList();
        if (goodsList == null) {
            return;
        }
        for (GoodsBo goods : goodsList) {
            long time = new Date().getTime();
//            商品在这个时间段才缓存
            if (goods.getStartDate() != null && goods.getEndDate() != null && time > goods.getStartDate().getTime() && time < goods.getEndDate().getTime()) {
                redisService.set(GoodsKey.getSeckillGoodsStock, String.valueOf(goods.getId()), goods.getStockCount(), Const.RedisCacheExtime.GOODS_LIST);
                log.debug("商品的库存缓存完成");
                localOverMap.put(goods.getId(), false);
                RedisGoodsBo redisGoodsBo = new RedisGoodsBo();
                BeanUtils.copyProperties(goods, redisGoodsBo);
                String jsonStr = JsonUtil.obj2String(redisGoodsBo);

                long ttl = goods.getEndDate().getTime() - time;
                redisService.set(GoodsKey.getSeckillGoodsDetail, String.valueOf(goods.getId()), jsonStr, (int) (ttl / 1000));
            }
        }
    }

    @RequestMapping("/seckill2")
    public String list2(Model model,
                        @RequestParam("goodsId") long goodsId, HttpServletRequest request) {
        String loginToken = CookieUtil.readLoginToken(request);
        User user = redisService.get(UserKey.getByName, loginToken, User.class);
        model.addAttribute("user", user);
        if (user == null) {
            return "login";
        }
        //判断库存
        GoodsBo goods = seckillGoodsService.getseckillGoodsBoByGoodsId(goodsId);
        int stock = goods.getStockCount();
        if (stock <= 0) {
            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
            return "miaosha_fail";
        }
        //判断是否已经秒杀到了
        SeckillOrder order = seckillOrderService.getSeckillOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
            return "miaosha_fail";
        }
        //减库存 下订单 写入秒杀订单
        OrderInfo orderInfo = seckillOrderService.insert(user, goods);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("goods", goods);
        return "order_detail";
    }

    /**
     *
     * 1.隐藏的秒杀接口 2.单一用户多线程用分布式锁 3.多用户同时秒杀用redis的原子性递减判断，
     * @param model
     * @param goodsId
     * @param path
     * @param request
     * @return
     */

    @RequestMapping(value = "/{path}/seckill", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> list(Model model,
                                @RequestParam("goodsId") long goodsId,
                                @PathVariable("path") String path,
                                HttpServletRequest request) {
//d8884f400cc29d9330ae75741f194590
        String loginToken = CookieUtil.readLoginToken(request);
        User user = redisService.get(UserKey.getByName, loginToken, User.class);
        if (user == null) {
            return Result.error(CodeMsg.USER_NO_LOGIN);
        }
        //内存标记，减少redis访问
        boolean over = localOverMap.get(goodsId);
        if (over) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //验证path
        boolean check = seckillOrderService.checkPath(user, goodsId, path);
        if (!check) {
            if (path != null && path.equals(Const.BaseConst.REPEAT_SECKILL_PATH)) {
                return Result.error(CodeMsg.REPEATE_MIAOSHA);
            }
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
//        获取商品信息，并判断是否秒杀过期
        String jsonStr = redisService.get(GoodsKey.getSeckillGoodsDetail, String.valueOf(goodsId), String.class);
        RedisGoodsBo redisGoodsBo = JsonUtil.string2Obj(jsonStr, RedisGoodsBo.class);
        if (redisGoodsBo == null) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        long endTime = redisGoodsBo.getEndDate().getTime();
        long now = new Date().getTime();
        if (now > endTime) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //判断是否已经秒杀到了
//        获取锁，如果获取成功， 说明是从未秒杀过，否则你已经秒杀过了，注意过期时间的设置
        boolean successful = redisService.getLock(OrderPrefix.OrderPrefix, String.valueOf(user.getId())
                + "_" + goodsId, "1", endTime - now, TimeUnit.MILLISECONDS);
        if (!successful) {
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }
        //预减库存，解决超卖问题
        long stock = redisService.decr(GoodsKey.getSeckillGoodsStock, String.valueOf(goodsId));
        if (stock < 0) {
            localOverMap.put(goodsId, true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //入队
        SeckillMessage mm = new SeckillMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        mm.setPath(path);
        mqSender.sendSeckillMessage(mm);
        return Result.success(0);
    }

    /**
     * 客户端轮询查询是否下单成功
     * orderId：成功
     * -1：秒杀失败
     * 0： 排队中
     */
    @RequestMapping(value = "/{path}/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(@RequestParam("goodsId") long goodsId,
                                      @PathVariable("path") String path,
                                      HttpServletRequest request) {
        String loginToken = CookieUtil.readLoginToken(request);
        User user = redisService.get(UserKey.getByName, loginToken, User.class);
        if (user == null) {
            return Result.error(CodeMsg.USER_NO_LOGIN);
        }
        long result = seckillOrderService.getSeckillResult((long) user. getId(), goodsId, path);
        return Result.success(result);
    }


    @AccessLimit(seconds=5, maxCount=5, needLogin=true)
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request, User user,
                                         @RequestParam("goodsId") long goodsId) {
        String loginToken = CookieUtil.readLoginToken(request);
        user = redisService.get(UserKey.getByName, loginToken, User.class);
        if (user == null) {
            return Result.error(CodeMsg.USER_NO_LOGIN);
        }

        String path = seckillOrderService.createMiaoshaPath(user, goodsId);
        return Result.success(path);
    }


    /**
     * 获取验证码接口
     * @param req
     * @param resp
     * @throws IOException
     */
    @GetMapping("/get/captcha")
    public void getCaptcha(HttpServletRequest req, HttpServletResponse resp, HttpSession session) throws IOException {
        int width = 150;
        int height = 40;

        // 创建验证码图片
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);

        // 设置字体
        Font font = new Font("Arial", Font.PLAIN, 30);
        graphics.setFont(font);

        // 随机生成验证码字符
        String captcha = generateCaptchaString();
        session.setAttribute(Const.BaseConst.CAPTCHA_SESSION_KEY, captcha);  // 将验证码存储在 session 中

        // 绘制验证码字符
        graphics.setColor(Color.BLACK);
        graphics.drawString(captcha, 10, 30);

        // 设置响应类型为图片
        resp.setContentType("image/jpeg");

        // 输出图片
        OutputStream os = resp.getOutputStream();
        ImageIO.write(image, "JPEG", os);
        os.close();
    }

    @PostMapping("/captcha/verify")
    @ResponseBody
    public Result<Void> verifyCaptcha(@RequestParam("captchaInput") String captcha, HttpServletRequest req) {
        if (StringUtils.isEmpty(captcha)) {
            return Result.error(CodeMsg.CAPTCHA_ERROR);
        }
        HttpSession session = req.getSession();
        String storedCaptcha = (String) session.getAttribute(Const.BaseConst.CAPTCHA_SESSION_KEY);
        if (!StringUtils.isEmpty(storedCaptcha) && storedCaptcha.equals(captcha)) {
            session.removeAttribute(Const.BaseConst.CAPTCHA_SESSION_KEY);
            return Result.success();
        }
        return Result.error(CodeMsg.CAPTCHA_ERROR);
    }

    // 生成随机的验证码字符串
    private String generateCaptchaString() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(chars.length());
            captcha.append(chars.charAt(index));
        }
        return captcha.toString();
    }


}
