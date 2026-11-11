<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  GIỚI THIỆU  (GET /about -> AboutServlet)
  Thuộc tính request:
    aboutImage   String   (tùy chọn) đường dẫn ảnh cho khối ảnh + chữ; rỗng/null thì hiện khung xám
  Không có form.
--%>
<t:layout title="Giới thiệu" nav="about" description="Saltair làm đồ basic nhẹ, thoáng và dễ phối, lấy cảm hứng từ không khí của biển.">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/catalog.css"></jsp:attribute>
  <jsp:body>

    <%-- 1. Mở đầu --%>
    <header class="about-hero">
      <span class="t-label cat-enter">Về Saltair</span>
      <h1 class="h-display cat-enter" style="--i:1">Basic cho những ngày nắng.</h1>
      <p class="about-hero__lead cat-enter" style="--i:2">Saltair làm những món đồ nhẹ, thoáng và dễ phối, để bạn mặc nhiều hơn là đắn đo mỗi sáng.</p>
    </header>

    <%-- 2. Câu chuyện thương hiệu --%>
    <section class="about-story" aria-labelledby="about-story-title">
      <div class="reveal">
        <span class="t-label">Câu chuyện</span>
        <h2 class="h-1 mt-16" id="about-story-title">Bắt đầu từ một buổi sáng bên biển.</h2>
      </div>
      <div class="about-story__text reveal" style="--i:1">
        <p>Saltair lấy cảm hứng từ không khí mặn mòi của biển: nhẹ, thoáng và tự do. Chúng tôi tin rằng một chiếc áo thun đẹp không cần nhiều chi tiết, chỉ cần vải mềm, form dễ mặc và một màu bạn thấy thoải mái.</p>
        <p>Vì vậy mỗi bộ sưu tập chỉ có một số mẫu chọn lọc, đủ để bạn phối cả tuần mà không phải mở tủ đồ rồi phân vân. Đồ của Saltair là unisex, mặc đi biển, đi làm hay dạo phố đều hợp.</p>
        <p>Saltair là dự án học tập của nhóm sinh viên, nên mọi sản phẩm và đơn hàng trên trang này chỉ để minh họa.</p>
      </div>
    </section>

    <%-- 3. Ba giá trị --%>
    <section class="about-values" data-stagger aria-label="Giá trị của Saltair">
      <div class="about-value reveal">
        <t:icon name="shirt"/>
        <h2 class="h-2">Chất liệu mềm</h2>
        <p>Cotton và nỉ mềm trên da, giặt nhiều vẫn giữ form, mặc cả ngày không thấy nặng.</p>
      </div>
      <div class="about-value reveal">
        <t:icon name="palette"/>
        <h2 class="h-2">Màu dễ phối</h2>
        <p>Trắng ngà, đen, navy, xanh biển nhạt, xám và be cát: món nào cũng mặc chung được với nhau.</p>
      </div>
      <div class="about-value reveal">
        <t:icon name="package"/>
        <h2 class="h-2">Bộ sưu tập nhỏ</h2>
        <p>Ít mẫu nhưng kỹ, chọn lọc theo từng mùa để tủ đồ của bạn gọn mà vẫn đủ dùng.</p>
      </div>
    </section>

    <%-- 4. Khối ảnh và chữ --%>
    <section class="about-split" data-stagger>
      <div class="media media--4x5 reveal"><t:img src="${aboutImage}" alt="Saltair bên bờ biển"/></div>
      <div class="about-split__text reveal">
        <span class="t-label">Cách chúng tôi làm</span>
        <h2 class="h-1">Giữ mọi thứ đơn giản.</h2>
        <p>Mỗi món đồ đều đi qua cùng một câu hỏi: bạn có muốn mặc nó vào buổi sáng nhiều nắng không? Nếu câu trả lời là có, nó mới được vào bộ sưu tập.</p>
        <p>Chúng tôi chọn form rộng vừa phải, đường may gọn và màu trung tính, để bạn tự thêm phong cách của riêng mình.</p>
      </div>
    </section>

    <%-- 5. Kêu gọi hành động --%>
    <section class="dark-text reveal">
      <span class="dark-text__label">Sẵn sàng cho ngày nắng</span>
      <h2 class="h-1 dark-text__title">Chọn cho mình món đồ đầu tiên.</h2>
      <p class="dark-text__body">Xem toàn bộ sản phẩm của Saltair, từ áo thun, hoodie, áo khoác đến quần.</p>
      <a class="btn btn--white mt-32" href="${ctx}/products">Xem sản phẩm <t:icon name="arrow-right"/></a>
    </section>

  </jsp:body>
</t:layout>
