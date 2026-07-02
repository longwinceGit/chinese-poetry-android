package com.poetry.util;

import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.ArrayList;
import java.util.List;

/**
 * 拼音工具类（基于 pinyin4j），提供汉字到拼音的转换功能。
 * 支持带声调的拼音输出，标点符号返回空字符串。
 */
public class PinyinHelper {

    private static final HanyuPinyinOutputFormat FORMAT;

    static {
        FORMAT = new HanyuPinyinOutputFormat();
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
    }

    /**
     * 获取单个汉字的带音调拼音（小写），如 '床' → "chuáng"。
     *
     * @param c 待转换的汉字字符
     * @return 带声调的拼音字符串；若字符非汉字则返回字符本身
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
     * 获取字符串中每个字符的拼音列表（标点返回空串，非汉字返回字符本身）。
     *
     * @param chinese 待转换的中文字符串
     * @return 每个字符对应的拼音列表，索引与原字符串一一对应
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
     * 获取句子中每个字符的拼音，空格分隔，如 "床前明" → "chuáng qián míng"。
     *
     * @param chinese 待转换的中文字符串
     * @return 空格分隔的拼音字符串
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

    /**
     * 判断字符是否为标点符号（包含中英文标点）。
     *
     * @param c 待判断的字符
     * @return true 表示该字符是标点符号
     */
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
