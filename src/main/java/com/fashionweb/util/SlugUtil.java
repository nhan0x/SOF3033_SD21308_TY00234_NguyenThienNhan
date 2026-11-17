package com.fashionweb.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Tạo slug không dấu cho đường dẫn, ví dụ "Áo thun và Polo" -> "ao-thun-va-polo". */
public final class SlugUtil {

    /** Chữ thường, số, các cụm nối bằng một dấu gạch ngang. */
    public static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private SlugUtil() {
    }

    public static String toSlug(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim().replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = DIACRITICS.matcher(s).replaceAll("");
        s = s.toLowerCase(Locale.ROOT);
        s = NON_ALNUM.matcher(s).replaceAll("-");
        s = s.replaceAll("^-+|-+$", "");
        return s;
    }

    public static boolean isValid(String slug) {
        return slug != null && SLUG_PATTERN.matcher(slug).matches();
    }
}
