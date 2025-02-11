package cn.hfbin.seckill.redis;

public class OrderPrefix extends BasePrefix{

	private OrderPrefix(String prefix) {
		super(prefix);
	}
	
	public static OrderPrefix OrderPrefix = new OrderPrefix("order:");

}
