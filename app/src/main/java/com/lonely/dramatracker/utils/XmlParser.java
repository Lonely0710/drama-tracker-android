package com.lonely.dramatracker.utils;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XML解析工具类
 * 用于解析XML格式的数据，主要用于解析豆瓣和IMDb的API返回
 */
public class XmlParser {
    private static final String TAG = "XmlParser";

    /**
     * 解析XML字符串
     * @param xml XML字符串
     * @param tags 需要提取的标签列表
     * @return 解析结果列表，每个元素是一个包含指定标签值的Map
     */
    public static List<Map<String, String>> parse(String xml, List<String> tags) {
        List<Map<String, String>> results = new ArrayList<>();
        Map<String, String> current = null;

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xml));

            int eventType = parser.getEventType();
            String currentTag = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        if (isItemStart(tagName)) {
                            current = new HashMap<>();
                        } else if (current != null && tags.contains(tagName)) {
                            currentTag = tagName;
                        }
                        break;

                    case XmlPullParser.TEXT:
                        if (current != null && currentTag != null && !parser.isWhitespace()) {
                            current.put(currentTag, parser.getText());
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if (isItemEnd(parser.getName()) && current != null) {
                            results.add(current);
                            current = null;
                        }
                        currentTag = null;
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "解析XML失败: " + e.getMessage());
        }

        return results;
    }

    /**
     * 判断是否是条目开始标签
     * @param tagName 标签名
     * @return 是否是条目开始标签
     */
    private static boolean isItemStart(String tagName) {
        return "item".equals(tagName) || "entry".equals(tagName);
    }

    /**
     * 判断是否是条目结束标签
     * @param tagName 标签名
     * @return 是否是条目结束标签
     */
    private static boolean isItemEnd(String tagName) {
        return "item".equals(tagName) || "entry".equals(tagName);
    }

    /**
     * 从XML中提取单个值
     * @param xml XML字符串
     * @param tag 标签名
     * @return 标签值，如果未找到返回null
     */
    public static String getValue(String xml, String tag) {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xml));

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && tag.equals(parser.getName())) {
                    eventType = parser.next();
                    if (eventType == XmlPullParser.TEXT) {
                        return parser.getText();
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "提取XML值失败: " + e.getMessage());
        }
        return null;
    }
}
