package lion.payment.port.yeepay;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import lion.dev.encrypt.HmacMD5;

import org.apache.commons.codec.binary.Hex;

public class YeepayDigestUtil {

	private static String encodingCharset = "UTF-8";

	public static String hmacSign(String aValue, String aKey) {

		byte k_ipad[] = new byte[64];
		byte k_opad[] = new byte[64];
		byte keyb[];
		byte value[];
		try {
			keyb = aKey.getBytes(encodingCharset);
			value = aValue.getBytes(encodingCharset);
		} catch (UnsupportedEncodingException e) {
			keyb = aKey.getBytes();
			value = aValue.getBytes();
		}

		Arrays.fill(k_ipad, keyb.length, 64, (byte) 54);
		Arrays.fill(k_opad, keyb.length, 64, (byte) 92);
		for (int i = 0; i < keyb.length; i++) {
			k_ipad[i] = (byte) (keyb[i] ^ 0x36);
			k_opad[i] = (byte) (keyb[i] ^ 0x5c);
		}

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {

			return null;
		}
		md.update(k_ipad);
		md.update(value);
		byte dg[] = md.digest();
		md.reset();
		md.update(k_opad);
		md.update(dg, 0, 16);
		dg = md.digest();
		return Hex.encodeHexString(dg);
	}

	public static void main(String[] args) {

		System.out.println(hmacSign("Buy10000326625111111111111100.01CNY11011http://localhost:8080/yeepaydemo/yeepayCommon/JAVA/callback.jsp0ICBC-NET1",
				"0acqgug6x57m0wrsiod6clpn1ezh47r2ot5h1zkq5dztiic8y5xkm5g0p0ek"));
		System.out.println(Hex.encodeHex(new HmacMD5().digest("abcdefg".getBytes(), "8UPp0KE8sq73zVP370vko7C39403rtK1YwX40Td6irH216036H27Eb12792t")));
	}
}
