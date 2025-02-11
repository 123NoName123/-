package cn.hfbin.seckill.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class RedisGoodsBo {

    private BigDecimal seckillPrice;

    private Date startDate;

    private Date endDate;
}
