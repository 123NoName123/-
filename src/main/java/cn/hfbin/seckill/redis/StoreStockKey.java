package cn.hfbin.seckill.redis;

public class StoreStockKey extends BasePrefix{

	private StoreStockKey(String prefix) {
		super(prefix);
	}
	
	public static StoreStockKey storeStockKey = new StoreStockKey("store_stock");

}
