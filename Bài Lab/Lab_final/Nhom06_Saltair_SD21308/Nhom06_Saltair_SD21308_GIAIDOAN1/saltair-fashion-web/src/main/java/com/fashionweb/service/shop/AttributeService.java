package com.fashionweb.service.shop;

import com.fashionweb.dao.shop.ColorDAO;
import com.fashionweb.dao.shop.SizeDAO;
import com.fashionweb.exception.ValidationException;
import com.fashionweb.model.Color;
import com.fashionweb.model.Size;
import com.fashionweb.util.SlugUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Màu và size.
 *
 * <p>Luồng gọi: Servlet -> Service -> DAO -> MySQL.
 * <p>Quy ước: không cho xóa mềm màu hoặc size đang có trong biến thể của sản phẩm chưa xóa (báo rõ số sản phẩm).
 */
public class AttributeService {

    private static final Pattern HEX = Pattern.compile("#[0-9A-Fa-f]{6}");

    private final ColorDAO colorDAO = new ColorDAO();
    private final SizeDAO sizeDAO = new SizeDAO();

    /**
     * Màu chưa xóa mềm.
     */
    public List<Color> findColors() {
        return colorDAO.findAll();
    }

    /**
     * Size chưa xóa mềm, sắp theo sortOrder.
     */
    public List<Size> findSizes() {
        return sizeDAO.findAll();
    }

    /**
     * Màu đã xóa mềm (Thùng rác).
     */
    public List<Color> findDeletedColors() {
        return colorDAO.findDeleted();
    }

    /**
     * Size đã xóa mềm (Thùng rác).
     */
    public List<Size> findDeletedSizes() {
        return sizeDAO.findDeleted();
    }

    /**
     * Khôi phục màu từ Thùng rác.
     */
    public void restoreColor(int id) {
        Color c = colorDAO.findById(id);
        if (c == null || !c.isDeleted()) {
            throw new ValidationException(null, "Màu không có trong Thùng rác.");
        }
        colorDAO.setDeleted(id, false);
    }

    /**
     * Khôi phục size từ Thùng rác.
     */
    public void restoreSize(int id) {
        Size s = sizeDAO.findById(id);
        if (s == null || !s.isDeleted()) {
            throw new ValidationException(null, "Size không có trong Thùng rác.");
        }
        sizeDAO.setDeleted(id, false);
    }

    /**
     * id = 0 thêm, id > 0 sửa. code duy nhất, hexCode dạng #RRGGBB. Sai thì ném ValidationException (name, code, hexCode).
     */
    public void saveColor(Color c) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = c.getName() == null ? "" : c.getName().trim();
        String code = c.getCode() == null ? "" : c.getCode().trim().toLowerCase(Locale.ROOT);
        if (code.isEmpty()) {
            code = SlugUtil.toSlug(name);
        }
        String hex = c.getHexCode() == null ? "" : c.getHexCode().trim();

        if (name.isEmpty() || name.length() > 50) {
            errors.put("name", "Tên màu từ 1 đến 50 ký tự.");
        }
        if (code.isEmpty() || code.length() > 30 || !SlugUtil.isValid(code)) {
            errors.put("code", "Mã màu chỉ gồm chữ thường không dấu, số và dấu gạch ngang (tối đa 30 ký tự).");
        } else if (colorDAO.existsCode(code, c.getId())) {
            errors.put("code", "Mã màu \"" + code + "\" đã được dùng (có thể ở màu trong Thùng rác).");
        }
        if (!HEX.matcher(hex).matches()) {
            errors.put("hexCode", "Mã màu hex phải có dạng #RRGGBB.");
        }
        if (c.getId() > 0) {
            Color existing = colorDAO.findById(c.getId());
            if (existing == null || existing.isDeleted()) {
                errors.put("name", "Màu không tồn tại hoặc đã bị xóa.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        c.setName(name);
        c.setCode(code);
        c.setHexCode(hex.toUpperCase(Locale.ROOT));
        if (c.getId() > 0) {
            colorDAO.update(c);
        } else {
            c.setId(colorDAO.insert(c));
        }
    }

    /**
     * Xóa mềm. Chặn nếu màu đang gắn biến thể của sản phẩm chưa xóa.
     */
    public void deleteColor(int id) {
        Color c = colorDAO.findById(id);
        if (c == null || c.isDeleted()) {
            throw new ValidationException(null, "Màu không tồn tại hoặc đã nằm trong Thùng rác.");
        }
        int used = colorDAO.countProductsUsing(id);
        if (used > 0) {
            throw new ValidationException(null, "Không thể xóa màu \"" + c.getName() + "\" vì đang có trong biến thể của "
                    + used + " sản phẩm.");
        }
        colorDAO.setDeleted(id, true);
    }

    /**
     * name duy nhất, sortOrder >= 0. Sai thì ném ValidationException (name, sortOrder).
     */
    public void saveSize(Size s) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = s.getName() == null ? "" : s.getName().trim().toUpperCase(Locale.ROOT);

        if (name.isEmpty() || name.length() > 20) {
            errors.put("name", "Tên size từ 1 đến 20 ký tự.");
        } else if (sizeDAO.existsName(name, s.getId())) {
            errors.put("name", "Size \"" + name + "\" đã tồn tại (có thể ở size trong Thùng rác).");
        }
        if (s.getSortOrder() < 0 || s.getSortOrder() > 99) {
            errors.put("sortOrder", "Thứ tự là số nguyên từ 0 đến 99.");
        }
        if (s.getId() > 0) {
            Size existing = sizeDAO.findById(s.getId());
            if (existing == null || existing.isDeleted()) {
                errors.put("name", "Size không tồn tại hoặc đã bị xóa.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        s.setName(name);
        if (s.getId() > 0) {
            sizeDAO.update(s);
        } else {
            s.setId(sizeDAO.insert(s));
        }
    }

    /**
     * Xóa mềm, tương tự màu.
     */
    public void deleteSize(int id) {
        Size s = sizeDAO.findById(id);
        if (s == null || s.isDeleted()) {
            throw new ValidationException(null, "Size không tồn tại hoặc đã nằm trong Thùng rác.");
        }
        int used = sizeDAO.countProductsUsing(id);
        if (used > 0) {
            throw new ValidationException(null, "Không thể xóa size \"" + s.getName() + "\" vì đang có trong biến thể của "
                    + used + " sản phẩm.");
        }
        sizeDAO.setDeleted(id, true);
    }
}
