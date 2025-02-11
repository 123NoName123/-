package cn.hfbin.seckill.redis;

public class PathKey extends BasePrefix{

	private PathKey(String prefix) {
		super(prefix);
	}
	
	public static PathKey pathKey = new PathKey("path");

}
