package com.poly.sof309.controllers.support;

import jakarta.servlet.http.HttpServlet;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dựng một Servlet với các Service được thay bằng đối tượng giả, mà không cần sửa mã nguồn ứng dụng.
 *
 * <p>Servlet của dự án tự tạo Service bằng {@code new XxxService()} (không có DI). {@link Mockito#mockConstruction}
 * chặn lời gọi {@code new} đó trong lúc Servlet được tạo, nên Servlet nhận về đối tượng giả. Nhờ vậy test Controller chạy
 * độc lập với MySQL, và chỉ kiểm tra logic của chính Controller (đọc tham số, kiểm tra dữ liệu, redirect, flash...).
 *
 * <ul>
 *   <li>{@link #mocked}: Service giả hoàn toàn, mọi hàm trả giá trị mặc định (null, 0, danh sách rỗng) cho tới khi stub.</li>
 *   <li>{@link #partial}: Service giả nhưng gọi hàm thật khi chưa stub. Dùng cho hàm thuần (chuẩn hóa email, tính giảm giá)
 *       và chỉ stub những hàm chạm database.</li>
 * </ul>
 * Phải {@link #close()} sau mỗi test (dùng {@code @AfterEach}); {@code mockConstruction} gắn với luồng hiện tại.
 */
public final class Harness<T extends HttpServlet> implements AutoCloseable {

    public final T servlet;
    private final List<MockedConstruction<?>> constructions;
    private final Map<Class<?>, Object> services = new HashMap<>();

    private Harness(T servlet, List<MockedConstruction<?>> constructions) {
        this.servlet = servlet;
        this.constructions = constructions;
    }

    /** Tạo Servlet; mọi lớp trong {@code mocked} bị thay bằng đối tượng giả hoàn toàn. */
    public static <T extends HttpServlet> Harness<T> mocked(Supplier<T> factory, Class<?>... mocked) {
        return build(factory, mocked, new Class<?>[0]);
    }

    /** Như {@link #mocked} nhưng thêm các lớp {@code partial} gọi hàm thật khi chưa stub. */
    public static <T extends HttpServlet> Harness<T> partial(Supplier<T> factory, Class<?>[] mocked, Class<?>[] partial) {
        return build(factory, mocked, partial);
    }

    private static <T extends HttpServlet> Harness<T> build(Supplier<T> factory, Class<?>[] mocked, Class<?>[] partial) {
        Map<Class<?>, MockedConstruction<?>> opened = new LinkedHashMap<>();
        for (Class<?> type : mocked) {
            opened.put(type, Mockito.mockConstruction(type));
        }
        for (Class<?> type : partial) {
            opened.put(type, Mockito.mockConstruction(type,
                    Mockito.withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS)));
        }
        T servlet = factory.get();
        Harness<T> h = new Harness<>(servlet, new ArrayList<>(opened.values()));
        opened.forEach((type, c) -> {
            assertEquals(1, c.constructed().size(), "Servlet phải tạo đúng 1 đối tượng " + type.getSimpleName());
            h.services.put(type, c.constructed().get(0));
        });
        return h;
    }

    /** Đối tượng giả mà Servlet đang dùng cho kiểu {@code type}. */
    public <S> S service(Class<S> type) {
        return type.cast(services.get(type));
    }

    @Override
    public void close() {
        constructions.forEach(MockedConstruction::close);
    }
}
