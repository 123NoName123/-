package cn.hfbin.seckill.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancleSeckillOrderMessage {
    private long userId;

    private long goodsId;
    private String path;
}
