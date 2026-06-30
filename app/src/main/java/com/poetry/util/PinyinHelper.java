package com.poetry.util;

import com.github.promeg.pinyinhelper.Pinyin;

public class PinyinHelper {

    public static String toPinyin(String chinese) {
        if (chinese == null || chinese.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chinese.length(); i++) {
            char c = chinese.charAt(i);
            if (isPunctuation(c)) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            } else if (c == ' ') {
                sb.append(' ');
            } else {
                String p = Pinyin.toPinyin(c);
                if (p != null) {
                    sb.append(p).append(' ');
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString().trim();
    }

    public static String getLinesPinyin(String[] lines) {
        if (lines == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(toPinyin(line)).append('\n');
        }
        return sb.toString().trim();
    }

    private static boolean isPunctuation(char c) {
        int type = Character.getType(c);
        return type == Character.DASH_PUNCTUATION
            || type == Character.START_PUNCTUATION
            || type == Character.END_PUNCTUATION
            || type == Character.CONNECTOR_PUNCTUATION
            || type == Character.OTHER_PUNCTUATION
            || type == Character.INITIAL_QUOTE_PUNCTUATION
            || type == Character.FINAL_QUOTE_PUNCTUATION
            || c == ','
            || c == '.'
            || c == '!'
            || c == '?'
            || c == ';'
            || c == ':'
            || c == '\''
            || c == '"'
            || c == ' ';
    }
}
