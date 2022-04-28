package com.droidsee.speechrecodemo.Utils;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Utils {

	public static String htmlDecode(String htmlData)
	{
		String ret = htmlData;
		String regex = "&#\\d*;";
		
		while(true)
		{
			Matcher m = Pattern.compile(regex).matcher(ret);
			if(m.find())
			{
				String temp = m.group();
				String str = temp.replaceAll("(&#)|;", "");
				str = Character.toString((char)Integer.parseInt(str));
				ret = ret.replaceAll(temp, str);
			}
			else
			{
				break;
			}
		}
		
		return ret;
	}
	
	public static long Date2TimeStamp(String aDate, String aFormat) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(aFormat);

		long ret = 0;
		try {
			Date date = simpleDateFormat.parse(aDate);
			ret = date.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static void LOG(String aContent)
	{
		System.out.println("[" + getHourOfDay() + ":" + getMinuteOfDay() + "] " + aContent);
	}

	public static int getWeekOfDate() {
		Date date = new Date();
		SimpleDateFormat dateFm = new SimpleDateFormat("EEEE");
		dateFm.format(date);
		// String[] strWeekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五",
		// "星期六"};
		// int[] weekDays = {0,1,2,3,4,5,6};
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
		if (w < 0)
			w = 0;

		return w;
	}
	
	public static String getDateSec(String aDate)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		return sdf.format(new Date(Long.parseLong(aDate) * 1000));
	}
	
	public static String getCurrentTime()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		return sdf.format(new Date());
	}

	public static int getHourOfDay() {
		Calendar now = Calendar.getInstance();
		return now.get(Calendar.HOUR_OF_DAY);
	}

	public static int getMinuteOfDay() {
		Calendar now = Calendar.getInstance();
		return now.get(Calendar.MINUTE);
	}

	public static String spliteAppName(String aAppName) {
		String ret = aAppName;
		try {
			ret = aAppName.replace("-", "-").replace("—", "-").replace(" ", "");
			String[] tt = ret.split("-");
			ret = tt[0].trim();

			if (ret.length() > 5) {
				ret = ret.substring(0, 5);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}

	public static String deCodeUrl(String s) {
		String ret = null;
		try {
			ret = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Log.e("parse", ret);
		return ret;
	}

    //Files.readAllBytes(Path path) - Java 7 and above

    @TargetApi(Build.VERSION_CODES.O)
    public static String readAllBytesJava7(String filePath)
    {
        String content = "";
        try
        {
            content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return content;
    }

	public static String readFileToString(String filePath)
	{
		StringBuilder contentBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath)))
		{

			String sCurrentLine;
			while ((sCurrentLine = br.readLine()) != null)
			{
				contentBuilder.append(sCurrentLine).append("\n");
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return contentBuilder.toString();
	}

    private static String ReaderJson(String filePath)throws IOException{
        //对一串字符进行操作
        StringBuffer fileData = new StringBuffer();
        //
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        //缓冲区使用完必须关掉
        reader.close();
        return fileData.toString();
    }

    //String -> File
    public static void string2File(String str, File file) throws IOException {
        //String -> InputStream
        ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
        //InputStream -> File
        OutputStream os = new FileOutputStream(file);
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        while ((bytesRead = stream.read(buffer, 0, 8192)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.close();
        stream.close();
    }
}
