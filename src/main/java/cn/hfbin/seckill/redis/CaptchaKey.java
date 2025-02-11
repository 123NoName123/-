package cn.hfbin.seckill.redis;

public class CaptchaKey extends BasePrefix{

	private CaptchaKey(String prefix) {
		super(prefix);
	}
	
	public static CaptchaKey captchaKey = new CaptchaKey("captcha");

}
