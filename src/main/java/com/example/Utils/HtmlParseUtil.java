package com.example.Utils;

import com.example.Pojo.Content;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class HtmlParseUtil {

    public static void main(String[] args) throws IOException {
        new HtmlParseUtil().parseJD("java").forEach(System.out::println);
    }

    public ArrayList<Content> parseJD(String keywords) throws IOException {
        //获取请求 https://search.jd.com/Search?keyword=java

        String url = "https://search.jd.com/Search?keyword="+keywords;

        //解析网页
        // Document对象就是网页对象
        Document document = Jsoup.parse(new URL(url), 30000);

        //获取div
        Element j_goodsList = document.getElementById("J_goodsList");

        //获取所有li标签
        Elements li = j_goodsList.getElementsByTag("li");

        ArrayList<Content> goodsList = new ArrayList<>();
        for(Element el: li){
            //对于图片特别多的网站，所有的图片都是延迟加载的，所以需要获取source-data-lazy-img的路径
            String img = el.getElementsByTag("img").eq(0).attr("source-data-lazy-img");
            String price = el.getElementsByClass("p-price").eq(0).text();
            String title = el.getElementsByClass("p-name").eq(0).text();

            Content content = new Content();
            content.setImg(img);
            content.setPrice(price);
            content.setTitle(title);
            goodsList.add(content);

        }
        return goodsList;
    }
}
