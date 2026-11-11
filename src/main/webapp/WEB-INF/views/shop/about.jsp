<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%--
  GIỚI THIỆU  (GET /about -> AboutServlet)  [#91: bố cục theo trang "Who we are" của trang tham chiếu]
  Thứ tự khối như mẫu: ảnh bìa cao 720px dưới header trong suốt, dải chữ chạy, ảnh vuông + chữ, ảnh phủ màu có câu lớn,
  ảnh vuông + chữ, dòng chữ đậm, khối kêu gọi.
  Thuộc tính request (rỗng/null thì hiện khung xám):
    aboutCover   String   ảnh bìa đầu trang
    aboutImage   String   ảnh của khối câu lớn và khối ảnh + chữ thứ hai
  Không có form.
--%>
<t:layout title="Giới thiệu" nav="about" overlay="true" bodyClass="about-page" description="Saltair làm đồ basic nhẹ, thoáng và dễ phối, lấy cảm hứng từ không khí của biển.">
  <jsp:attribute name="head"><link rel="stylesheet" href="${ctx}/assets/css/pages/catalog.css?v=6"></jsp:attribute>
  <jsp:body>

    <%-- 1. Ảnh bìa --%>
    <header class="about-cover">
      <div class="about-cover__media"><t:img src="${aboutCover}" alt="Saltair bên bờ biển" dark="true" eager="true"/></div>
      <h1 class="sr-only">Giới thiệu Saltair</h1>
    </header>

    <%-- 2. Dải chữ chạy (lặp lại để nối liền, phần lặp ẩn với trình đọc màn hình) --%>
    <div class="marquee" aria-label="Bắt đầu từ một buổi sáng bên biển">
      <div class="marquee__track" aria-hidden="true">
        <c:forEach begin="1" end="6"><span class="marquee__item">Bắt đầu từ một buổi sáng bên biển</span></c:forEach>
      </div>
    </div>

    <%-- 3. Ảnh vuông + chữ: ba điều Saltair theo đuổi --%>
    <section class="about-tm" aria-labelledby="about-make-title">
      <div class="about-tm__media reveal">
        <div class="about-tm__brand"><span><img src="${ctx}/assets/brand/saltair-logo.png" alt="Saltair"></span></div>
      </div>
      <div class="about-tm__text reveal" style="--i:1">
        <h2 class="about-tm__title" id="about-make-title">Chúng tôi làm đồ basic nhẹ, thoáng và dễ phối theo mọi cách có thể.</h2>
        <p><strong>Ở chất liệu.</strong><br>Cotton và nỉ mềm trên da, giặt nhiều vẫn giữ form, mặc cả ngày không thấy nặng.</p>
        <p><strong>Ở màu sắc.</strong><br>Trắng ngà, đen, navy, xanh biển nhạt, xám và be cát: món nào cũng mặc chung được với nhau.</p>
        <p><strong>Ở số lượng.</strong><br>Ít mẫu nhưng kỹ, chọn lọc theo từng mùa để tủ đồ của bạn gọn mà vẫn đủ dùng.</p>
      </div>
    </section>

    <%-- 4. Ảnh phủ màu có câu lớn --%>
    <section class="about-statement">
      <div class="about-statement__media"><t:img src="${aboutImage}" alt="" dark="true"/></div>
      <p class="about-statement__text reveal">Thứ đầu tiên bạn mặc vào mỗi sáng nên thật dễ chịu.</p>
    </section>

    <%-- 5. Ảnh vuông + chữ: câu chuyện --%>
    <section class="about-tm" aria-labelledby="about-story-title">
      <div class="about-tm__media reveal"><div class="media"><t:img src="${aboutImage}" alt="Đồ basic của Saltair"/></div></div>
      <div class="about-tm__text reveal" style="--i:1">
        <h2 class="about-tm__title" id="about-story-title">Giữ mọi thứ đơn giản.</h2>
        <p>Saltair lấy cảm hứng từ không khí mặn mòi của biển: nhẹ, thoáng và tự do. Một chiếc áo thun đẹp không cần nhiều chi tiết, chỉ cần vải mềm, form dễ mặc và một màu bạn thấy thoải mái.</p>
        <p>Mỗi món đồ đều đi qua cùng một câu hỏi: bạn có muốn mặc nó vào buổi sáng nhiều nắng không? Nếu câu trả lời là có, nó mới được vào bộ sưu tập.</p>
        <p>Saltair là dự án học tập của nhóm sinh viên, nên mọi sản phẩm và đơn hàng trên trang này chỉ để minh họa.</p>
      </div>
    </section>

    <%-- 6. Dòng chữ đậm --%>
    <p class="about-line reveal">Nếu một món đồ không đủ mềm, đủ thoáng và đủ dễ phối, chúng tôi không làm.</p>

    <%-- 7. Kêu gọi hành động --%>
    <section class="about-statement about-statement--solid">
      <p class="about-statement__text reveal">Chọn cho mình món đồ đầu tiên.</p>
      <a class="btn btn--white reveal" href="${ctx}/products">Xem sản phẩm</a>
    </section>

  </jsp:body>
</t:layout>
