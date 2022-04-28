package com.droidsee.speechrecodemo.Utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

public class HttpPost {

	private static HttpPost mInstance = null;

	private boolean proxySet = false;
	private String proxyHost = null;
	private int proxyPort = -1;
	
	public static HttpPost getInstance()
	{
		if(null == mInstance)
		{
			mInstance = new HttpPost();
		}
		return mInstance;
	}
	
	public void setProxyEnable(boolean aEnable)
	{
		proxySet = aEnable;
	}
	public String postData(String aUrl, String aPostData, HashMap<String, String> aRequestProperty)
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
				httpURLConnection.setReadTimeout(10000);
				httpURLConnection.setDoInput(true);
				httpURLConnection.setDoOutput(true);
				httpURLConnection.setUseCaches(false);

                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.setRequestProperty("Charsert", "UTF-8");

                httpURLConnection.setRequestMethod("POST");

				String encoding = httpURLConnection.getRequestProperty("Transfer-Encoding");
				if(encoding != null && encoding.equals("chunked")) {
                    httpURLConnection.setChunkedStreamingMode(20*1024);
                }


				if(aPostData != null && !aPostData.isEmpty()) {
                    DataOutputStream dos=new DataOutputStream(httpURLConnection.getOutputStream());
                    dos.writeBytes(aPostData);
//				System.out.println("postData:" + aPostData);
                    dos.flush();
                    dos.close();
                }

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
			}
		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		return ret;
	}

    public String postData(String aUrl, File aPostData, HashMap<String, String> aRequestProperty)
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
                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setUseCaches(false);

                httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                httpURLConnection.setRequestProperty("Charsert", "UTF-8");

                httpURLConnection.setRequestMethod("POST");

                String encoding = httpURLConnection.getRequestProperty("Transfer-Encoding");
                if(encoding != null && encoding.equals("chunked")) {
                    httpURLConnection.setChunkedStreamingMode(20*1024);
                }


                if(aPostData != null) {
                    DataOutputStream dos=new DataOutputStream(httpURLConnection.getOutputStream());
//                    dos.writeBytes(aPostData);
////				System.out.println("postData:" + aPostData);
//                    dos.flush();
//                    dos.close();
                    InputStream is = new FileInputStream(aPostData);

                    byte[] buffer = new byte[1024*1024];
                    int len = 0;
                    while ((len = is.read(buffer)) != -1) {
                        dos.write(buffer, 0, len);
                    }
                    dos.flush();
                    dos.close();
                }

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
            }
        } catch (MalformedURLException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }

        return ret;
    }
	
	public String postData(String aUrl,String aPostData)
	{
		return postData(aUrl, aPostData, null);
	}
    public String postData(String aUrl,File aPostData)
    {
        return postData(aUrl, aPostData, null);
    }
}