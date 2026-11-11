package com.fashionweb.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit test cho SlugUtil: tạo slug không dấu và kiểm tra slug hợp lệ. */
class SlugUtilTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @DisplayName("toSlug: chuyển tên tiếng Việt thành slug")
    @CsvSource({
            "'Áo thun và Polo', ao-thun-va-polo",
            "'Đầm đi biển', dam-di-bien",
            "'  Quần   Jogger  ', quan-jogger",
            "'Hoodie (Zip) 2026!', hoodie-zip-2026",
            "'---Áo khoác---', ao-khoac",
            "'ĐĐĐ', ddd"
    })
    void toSlug_convertsVietnameseText(String input, String expected) {
        assertEquals(expected, SlugUtil.toSlug(input));
    }

    @Test
    @DisplayName("toSlug: null trả về chuỗi rỗng")
    void toSlug_null_returnsEmpty() {
        assertEquals("", SlugUtil.toSlug(null));
    }

    @ParameterizedTest
    @DisplayName("isValid: slug đúng định dạng")
    @ValueSource(strings = {"ao-thun", "a", "quan-jogger-2026", "123"})
    void isValid_acceptsWellFormedSlug(String slug) {
        assertTrue(SlugUtil.isValid(slug));
    }

    @ParameterizedTest
    @DisplayName("isValid: slug sai định dạng")
    @NullAndEmptySource
    @ValueSource(strings = {"Ao-thun", "ao--thun", "-ao", "ao-", "ao thun", "áo-thun", "ao_thun"})
    void isValid_rejectsMalformedSlug(String slug) {
        assertFalse(SlugUtil.isValid(slug));
    }
}
