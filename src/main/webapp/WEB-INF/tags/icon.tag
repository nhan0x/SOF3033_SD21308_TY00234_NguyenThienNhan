<%@ tag description="Icon SVG từ bộ sprite (WEB-INF/views/common/icons.jspf)" pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="name" required="true" description="search, user, bag, heart, star, chevron-down..." %>
<%@ attribute name="size" required="false" description="sm | md | lg (mặc định 16px)" %>
<%@ attribute name="cls" required="false" description="class CSS thêm" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<svg class="icon${size == 'sm' ? ' icon--sm' : ''}${size == 'md' ? ' icon--md' : ''}${size == 'lg' ? ' icon--lg' : ''}${empty cls ? '' : ' '}${cls}" aria-hidden="true" focusable="false"><use href="#i-${name}"></use></svg>
