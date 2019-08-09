package com.mine.musharing.utils;

/**
 * <h1>小工具库</h1>
 * 
 * 一些小的工具
 */
public class Utility {

	/**
	 * 把 {@code byte[]} 转换成 <em>十六进制</em> 表示<br/>
	 * 
	 * 用来支持 encryptPassword 时，java 获取的是一个 byte[], 把它转换成后端需要的 十六进制 表示。
	 * @param bytes 待转化的byte[]
	 * @return 转化出的十六进制字符串表示
	 */
	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = "0123456789abcdef".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0, v; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * <h2>格式化文本</h2>
	 * 
	 * 用来解决显示的在对话气泡中的文本太多可能超出屏幕的问题<br/>
	 * 实现方式为在s中每singleLineLong个字符后插入一个"\n"
	 * @param s 待格式化的字符串
	 * @param singleLineLong 最大单行长度
	 * @return 格式化后的字符串
	 */
	public static String formatText(String s, int singleLineLong) {
		StringBuilder stringBuilder = new StringBuilder();

		int i;
		for (i = 0; i + singleLineLong < s.length(); i += singleLineLong) {
			stringBuilder.append(s.substring(i, i + singleLineLong));
			stringBuilder.append("\n");
		}
		stringBuilder.append(s.substring(i));

		return stringBuilder.toString();
	}

	/**
	 * <h2>格式化歌曲进度</h2>
	 *
	 * 把从 MediaPlayer 获取的 progress in milliseconds 格式化成可读的时间(e.g. 03:11)
	 *
	 * @param progress playing progress in milliseconds
	 * @return formated time string
	 */
	public static String formatMusicProgress(int progress) {
		String sign = "";
		if (progress < 0) {
			sign = "-";
			progress = -progress;
		}
		long t = progress / 1000;
		long m = t / 60;
		long s = t % 60;
		StringBuilder minute = new StringBuilder();
		if (m < 10) {
			minute.append("0");
		}
		minute.append(m);
		StringBuilder second = new StringBuilder();
		if (s < 10) {
			second.append("0");
		}
		second.append(s);
		return sign + minute.toString() + ":" + second.toString();
	}

}