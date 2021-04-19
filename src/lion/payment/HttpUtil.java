package lion.payment;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import lion.dev.io.FileUtil;
import lion.dev.lang.MapJ;

public class HttpUtil {

	public String get(String url) {

		URL request;
		String reuslt = "";
		try {
			request = new URL(url);
			HttpURLConnection urlConnection = (HttpURLConnection) request.openConnection();
			reuslt = FileUtil.read(urlConnection.getInputStream());

			urlConnection.disconnect();
		} catch (Exception e) {
		}

		return reuslt;
	}

	public String post(String url, MapJ param) {

		URL request;
		StringBuffer buf = new StringBuffer();
		String content = "";

		StringBuffer tmpBuf = new StringBuffer();
		if (param != null && !param.isEmpty()) {
			for (String key : param.keySet()) {
				try {
					tmpBuf.append("&" + key + "=" + URLEncoder.encode(param.getString(key), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
		if (tmpBuf.length() > 0) {
			content = tmpBuf.substring(1);
		}

		try {

			request = new URL(url);
			HttpURLConnection urlConnection = (HttpURLConnection) request.openConnection();
			urlConnection.setDoInput(true);
			urlConnection.setDoOutput(true);
			urlConnection.setRequestMethod("POST");
			urlConnection.setUseCaches(false);
			urlConnection.setInstanceFollowRedirects(true);
			urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			urlConnection.connect();

			if (content.length() > 0) {
				DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());

				out.writeBytes(content);
				out.flush();
				out.close();
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String tmp = "";
			while ((tmp = reader.readLine()) != null) {
				buf.append(tmp).append("\n");
			}

			urlConnection.disconnect();
		} catch (Exception e) {
		}
		return buf.toString();
	}
}
