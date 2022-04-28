package com.droidsee.speechrecodemo.Utils;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlParser
{
    private String filecontent;
    
    public static void main(String[] a)
    {
        XmlParser xml = new XmlParser("./styles.xml");
        System.out.println(xml.getNode("DB"));
    }
    
    public XmlParser(String file)
    {
        this.filecontent = this.readFile(file, "utf-8");
    }
    
    public String getNode(String node, String def)
    {
        String r = this.getNode(node);
        if (r == null || r.equals(""))
        {
            return def;
        }
        else
        {
            return r;
        }
    }
    
    public boolean getNode(String node, boolean def)
    {
        String r = this.getNode(node);
        if (r == null)
        {
            return def;
        }
        else
        {
            if (r.equals("0"))
            {
                return false;
            }
            else
            {
                return true;
            }
        }
    }
    
    public int getNode(String node, int def)
    {
        String r = this.getNode(node);
        if (r == null)
        {
            return def;
        }
        else
        {
            return Integer.parseInt(r);
        }
    }
    
    public float getNode(String node, float def)
    {
        String r = this.getNode(node);
        if (r == null)
        {
            return def;
        }
        else
        {
            return Float.parseFloat(r);
        }
    }
    
    private String getNode(String node)
    {
        String[] nodes = node.split("/");
        Pattern p;
        Matcher matcher;
        
        String content = this.filecontent;
        for (int i = 0; i < nodes.length - 1; i++)
        {
            content = this.getString(nodes[i], content);
            if (content == null)
            {
                return null;
            }
        }
        
        String grep = "<" + nodes[nodes.length - 1] + ">([^<]+)</" + nodes[nodes.length - 1] + ">";
        
        p = Pattern.compile(grep, Pattern.CASE_INSENSITIVE);
        matcher = p.matcher(content);
        while (matcher.find())
        {
            return matcher.group(1);
        }
        return null;
    }
    
    public String getString(String node, String content)
    {
        Pattern p;
        Matcher matcher;
        String grep = "<" + node + ">(.*?)</" + node + ">";
        
        p = Pattern.compile(grep, Pattern.CASE_INSENSITIVE);
        matcher = p.matcher(content);
        
        while (matcher.find())
        {
            
            return matcher.group(1);
        }
        return null;
    }
    
    public String readFile(String file, String charset)
    {
        RandomAccessFile randomFile = null;
        byte[] data = null;
        try
        {
            randomFile = new RandomAccessFile(file, "rw");
            int datalen = (int) randomFile.length();
            data = new byte[datalen];
            int readlen = 0;
            while (readlen < datalen)
            {
                readlen += randomFile.read(data, readlen, datalen - readlen);
            }
            
            randomFile.close();
            randomFile = null;
            
            String a = new String(data, charset);
            a = a.replaceAll("\n", "");
            a = a.replaceAll("\r\n", "");
            a = a.replaceAll("\r", "");
            a = a.replaceAll("\n", "");
            return a;
        }
        catch (IOException e)
        {
            System.out.println("File[" + file + "] Not Found");
        }
        
        randomFile = null;
        if (data != null)
        {
            String a = new String(data);
            a = a.replaceAll("\n", "");
            a = a.replaceAll("\r\n", "");
            a = a.replaceAll("\r", "");
            a = a.replaceAll("\n", "");
            return a;
        }
        else
        {
            return "";
        }
    }
    
}
