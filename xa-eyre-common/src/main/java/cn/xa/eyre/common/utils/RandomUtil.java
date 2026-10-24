package cn.xa.eyre.common.utils;


import java.security.SecureRandom;

public class RandomUtil {
	/** 复用同一个安全随机数源（原实现每次new Random，既浪费又属弱随机） */
	private static final SecureRandom RANDOM = new SecureRandom();

	public static int randomInt(int seed){
		return RANDOM.nextInt(seed);
	}
	// 返回一个当前时间+随机数
	public static String randomByTime(int seed){
		return DateUtils.dateTimeNow()+ String.valueOf(RANDOM.nextInt(seed));
	}

	/**
	 * 随机获取指定位数纯数字字符串
	 * @return
	 */
	public static String getNumbers(int number){
		StringBuilder str = new StringBuilder(number);
		for(int i=0;i<number;i++){
			str.append(getRadomInt(0, 9));
		}
		return str.toString();
	}
	/**
	 * 获取一个范围内的随机数字
	 * @return
	 */
	public static int getRadomInt(int min,int max){
		return RANDOM.nextInt(max-min+1)+min;
	}


	/**
	 * 生成英文加中文随机数
	 * @param length
	 * @return
	 */
	public static String generateRandomNumber(int length) {
		StringBuilder val = new StringBuilder(length);
		// 参数length，表示生成几位随机数
		for (int i = 0; i < length; i++) {
			String charOrNum = RANDOM.nextInt(2) % 2 == 0 ? "char" : "num";
			// 输出字母还是数字
			if ("char".equalsIgnoreCase(charOrNum)) {
				// 输出是大写字母还是小写字母
				int temp = RANDOM.nextInt(2) % 2 == 0 ? 65 : 97;
				val.append((char) (RANDOM.nextInt(26) + temp));
			} else if ("num".equalsIgnoreCase(charOrNum)) {
				val.append(RANDOM.nextInt(10));
			}
		}
		return val.toString();
	}
}
