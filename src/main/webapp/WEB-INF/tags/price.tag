<%@ tag description="Hiển thị giá VNĐ: 199.000đ" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN"/><fmt:formatNumber value="${value}" type="number" groupingUsed="true" maxFractionDigits="0"/>đ
