package com.poetry.util;

import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.List;

public class PinyinHelper {

    private static final HanyuPinyinOutputFormat FORMAT;

    static {
        FORMAT = new HanyuPinyinOutputFormat();
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
    }

    /**
     * 获取单个汉字的带音调拼音（小写），如 '床' → "chuáng"
     */
    public static String toTonePinyin(char c) {
        try {
            String[] arr = net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
            if (arr != null && arr.length > 0) {
                return arr[0];
            }
        } catch (BadHanyuPinyinOutputFormatCombination ignored) {
        }
        return String.valueOf(c);
    }

    /**
     * 获取字符串中每个字符的拼音列表（标点返回空串）
     */
    public static List<String> toPinyinList(String chinese) {
        List<String> list = new ArrayList<>();
        if (chinese == null || chinese.isEmpty()) return list;
        for (int i = 0; i < chinese.length(); i++) {
            char c = chinese.charAt(i);
            if (isPunctuation(c) || c == ' ') {
                list.add("");
            } else {
                try {
                    String[] arr = net.sourceforge.pinyin4j.PinyinHelper.toHanyuPinyinStringArray(c, FORMAT);
                    if (arr != null && arr.length > 0) {
                        list.add(arr[0]);
                    } else {
                        list.add(String.valueOf(c));
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    list.add(String.valueOf(c));
                }
            }
        }
        return list;
    }

    /**
     * 获取句子中每个字符的拼音，空格分隔，如 "床前明" → "chuáng qián míng"
     */
    public static String toPinyin(String chinese) {
        List<String> list = toPinyinList(chinese);
        StringBuilder sb = new StringBuilder();
        for (String p : list) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(p);
        }
        return sb.toString();
    }

    private static boolean isPunctuation(char c) {
        // 中文标点
        if (c == '，' || c == '。' || c == '、' || c == '；' || c == '：'
            || c == '？' || c == '！' || c == '（' || c == '）'
            || c == '"' || c == '"' || c == '\'' || c == '\'') {
            return true;
        }
        // 英文标点
        int type = Character.getType(c);
        return type == Character.DASH_PUNCTUATION
            || type == Character.START_PUNCTUATION
            || type == Character.END_PUNCTUATION
            || type == Character.CONNECTOR_PUNCTUATION
            || type == Character.OTHER_PUNCTUATION
            || type == Character.INITIAL_QUOTE_PUNCTUATION
            || type == Character.FINAL_QUOTE_PUNCTUATION
            || c == ',' || c == '.' || c == '!' || c == '?'
            || c == ';' || c == ':';
    }
}
