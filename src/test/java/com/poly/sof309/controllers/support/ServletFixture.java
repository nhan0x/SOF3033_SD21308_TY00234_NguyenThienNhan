package com.poly.sof309.controllers.support;

import com.fashionweb.constant.AppConstant;
import com.fashionweb.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Dựng sẵn request, response và session giả cho test Controller (không cần Tomcat, không cần database).
 *
 * <p>Ghi lại những gì Servlet làm: redirect tới đâu, sendError mã nào, forward tới JSP nào, thuộc tính request và session
 * (kể cả thông báo flash). Gọi {@link #run(HttpServlet)} để chạy Servlet qua {@code service()} (công khai), vì
 * {@code doGet/doPost} là {@code protected}.
 */
public final class ServletFixture {

    public static final String CTX = "/saltair";

    public final HttpServletRequest req = mock(HttpServletRequest.class);
    public final HttpServletResponse resp = mock(HttpServletResponse.class);
    public final HttpSession session = mock(HttpSession.class);

    /** Địa chỉ Servlet chuyển hướng tới (đã gồm context path), null nếu không redirect. */
    public String redirectedTo;
    /** Mã lỗi gọi qua sendError, null nếu không có. */
    public Integer errorStatus;
    /** Mã trạng thái gọi qua setStatus, null nếu không đặt. */
    public Integer status;
    /** Các JSP đã forward tới, theo thứ tự. */
    public final List<String> forwards = new ArrayList<>();
    public boolean sessionInvalidated;

    private final Map<String, String> params = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, Object> reqAttrs = new HashMap<>();
    private final Map<String, Object> sessAttrs = new HashMap<>();
    private final StringWriter body = new StringWriter();
    private boolean hasSession = true;

    private ServletFixture(String method, String path) {
        when(req.getMethod()).thenReturn(method);
        when(req.getContextPath()).thenReturn(CTX);
        when(req.getRequestURI()).thenReturn(CTX + path);
        when(req.getServletPath()).thenReturn(path);
        when(req.getServerName()).thenReturn("localhost");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        when(req.getParameter(anyString())).thenAnswer(i -> params.get(i.<String>getArgument(0)));
        when(req.getHeader(anyString())).thenAnswer(i -> headers.get(i.<String>getArgument(0)));
        when(req.getAttribute(anyString())).thenAnswer(i -> reqAttrs.get(i.<String>getArgument(0)));
        doAnswer(i -> reqAttrs.put(i.getArgument(0), i.getArgument(1))).when(req).setAttribute(anyString(), any());
        doAnswer(i -> reqAttrs.remove(i.<String>getArgument(0))).when(req).removeAttribute(anyString());
        when(req.getSession()).thenAnswer(i -> session);
        when(req.getSession(true)).thenAnswer(i -> session);
        when(req.getSession(false)).thenAnswer(i -> hasSession ? session : null);
        when(req.getRequestDispatcher(anyString())).thenAnswer(i -> dispatcher(i.getArgument(0)));

        when(session.getAttribute(anyString())).thenAnswer(i -> sessAttrs.get(i.<String>getArgument(0)));
        doAnswer(i -> sessAttrs.put(i.getArgument(0), i.getArgument(1))).when(session).setAttribute(anyString(), any());
        doAnswer(i -> sessAttrs.remove(i.<String>getArgument(0))).when(session).removeAttribute(anyString());
        doAnswer(i -> {
            sessionInvalidated = true;
            sessAttrs.clear();
            return null;
        }).when(session).invalidate();

        try {
            doAnswer(i -> redirectedTo = i.getArgument(0)).when(resp).sendRedirect(anyString());
            doAnswer(i -> errorStatus = i.getArgument(0)).when(resp).sendError(anyInt());
            doAnswer(i -> errorStatus = i.getArgument(0)).when(resp).sendError(anyInt(), anyString());
            doAnswer(i -> status = i.getArgument(0)).when(resp).setStatus(anyInt());
            when(resp.getWriter()).thenReturn(new PrintWriter(body));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ServletFixture get(String path) {
        return new ServletFixture("GET", path);
    }

    public static ServletFixture post(String path) {
        return new ServletFixture("POST", path);
    }

    public ServletFixture param(String name, String value) {
        params.put(name, value);
        return this;
    }

    public ServletFixture header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public ServletFixture queryString(String q) {
        when(req.getQueryString()).thenReturn(q);
        return this;
    }

    /** Đăng nhập: đặt User vào session như LoginSupport. */
    public ServletFixture loggedIn(User user) {
        sessAttrs.put(AppConstant.SESSION_USER, user);
        return this;
    }

    /** Request chưa có session (getSession(false) trả null). */
    public ServletFixture noSession() {
        hasSession = false;
        return this;
    }

    public ServletFixture sessionAttr(String name, Object value) {
        sessAttrs.put(name, value);
        return this;
    }

    /** Nội dung body (dùng cho webhook). */
    public ServletFixture body(byte[] bytes) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            when(req.getInputStream()).thenReturn(new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return in.read();
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return this;
    }

    public void run(HttpServlet servlet) throws Exception {
        servlet.service(req, resp);
    }

    public void run(jakarta.servlet.Filter filter, FilterChain chain) throws Exception {
        filter.doFilter(req, resp, chain);
    }

    // ---------- đọc kết quả ----------

    public Object requestAttr(String name) {
        return reqAttrs.get(name);
    }

    public Object sessionAttrValue(String name) {
        return sessAttrs.get(name);
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> errors() {
        return (Map<String, String>) reqAttrs.get("errors");
    }

    public String flashMessage() {
        return (String) sessAttrs.get("flashMessage");
    }

    public String flashType() {
        return (String) sessAttrs.get("flashType");
    }

    public String responseBody() {
        return body.toString();
    }

    public String lastForward() {
        return forwards.isEmpty() ? null : forwards.get(forwards.size() - 1);
    }

    private RequestDispatcher dispatcher(String path) {
        RequestDispatcher d = mock(RequestDispatcher.class);
        try {
            doAnswer(i -> forwards.add(path)).when(d).forward(any(), any());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return d;
    }
}
