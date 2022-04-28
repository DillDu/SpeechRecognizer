package com.droidsee.speechrecodemo.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HttpGet {

	private static HttpGet mInstance = null;

	private boolean proxySet = false;
	private String proxyHost = null;
	private int proxyPort = -1;
	
	public static HttpGet getInstance()
	{
		if(null == mInstance)
		{
			mInstance = new HttpGet();
		}
		return mInstance;
	}
	
	public void setProxyEnable(boolean aEnable)
	{
		proxySet = aEnable;
	}
	public String getData(String aUrl, HashMap<String, String> aRequestProperty)
	{
		String ret = "";
		try {
			HttpURLConnection httpURLConnection = null;
			URL url = new URL(aUrl);
			if (url != null) {
				if(proxySet){
	                @SuppressWarnings("static-access")
					Proxy proxy = new Proxy(Proxy.Type.DIRECT.HTTP, new InetSocketAddress(proxyHost, proxyPort));
	                httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
	            }else{
					httpURLConnection = (HttpURLConnection) url.openConnection();
	            }
				
				if(aRequestProperty != null && aRequestProperty.size() > 0)
				{
					for (Map.Entry<String, String> entry : aRequestProperty.entrySet()) {

						httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());

					}
				}
				
				httpURLConnection.setConnectTimeout(10000);
				httpURLConnection.setDoInput(true);
				
				httpURLConnection.setRequestMethod("GET");
				
				int status = httpURLConnection.getResponseCode();
				
//				System.out.println("getResponseCode:" + status);
				
				if (status == HttpURLConnection.HTTP_OK) {
					
					InputStream inputStream = httpURLConnection.getInputStream();

					try {
						if (inputStream != null) {
							
							BufferedReader in = new BufferedReader(new InputStreamReader(inputStream,"utf-8"));
				            String line;
				            while ((line = in.readLine()) != null) {
				                ret += line;
				            }
						}
						else
						{
							System.out.println("Response Error!!!");
						}
					} catch (Exception e) {
						
						e.printStackTrace();
					} finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (IOException e) {
								
								e.printStackTrace();
							}
						}
					}
				}
				else
				{
					System.out.println("getResponseCode:" + status);
				}
			}
		} catch (MalformedURLException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public String getData(String aUrl)
	{
		return getData(aUrl, null);
	}
}