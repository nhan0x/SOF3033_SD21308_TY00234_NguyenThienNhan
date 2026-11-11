package com.fashionweb.service.shop;

import com.fashionweb.repository.shop.ColorRepository;
import com.fashionweb.repository.shop.SizeRepository;
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
 * <p>Luồng gọi: Servlet -> Service -> Repository -> MySQL.
 * <p>Quy ước: không cho xóa mềm màu hoặc size đang có trong biến thể của sản phẩm chưa xóa (báo rõ số sản phẩm).
 */
public class AttributeService {

    private static final Pattern HEX = Pattern.compile("#[0-9A-Fa-f]{6}");

    private final ColorRepository colorRepository = new ColorRepository();
    private final SizeRepository sizeRepository = new SizeRepository();

    /**
     * Màu chưa xóa mềm.
     */
    public List<Color> findColors() {
        return colorRepository.findAll();
    }

    /**
     * Size chưa xóa mềm, sắp theo sortOrder.
     */
    public List<Size> findSizes() {
        return sizeRepository.findAll();
    }

    /**
     * Màu đã xóa mềm (Thùng rác).
     */
    public List<Color> findDeletedColors() {
        return colorRepository.findDeleted();
    }

    /**
     * Size đã xóa mềm (Thùng rác).
     */
    public List<Size> findDeletedSizes() {
        return sizeRepository.findDeleted();
    }

    /**
     * Khôi phục màu từ Thùng rác.
     */
    public void restoreColor(int id) {
        Color c = colorRepository.findById(id);
        if (c == null || !c.isDeleted()) {
            throw new ValidationException(null, "Màu không có trong Thùng rác.");
        }
        colorRepository.setDeleted(id, false);
    }

    /**
     * Khôi phục size từ Thùng rác.
     */
    public void restoreSize(int id) {
        Size s = sizeRepository.findById(id);
        if (s == null || !s.isDeleted()) {
            throw new ValidationException(null, "Size không có trong Thùng rác.");
        }
        sizeRepository.setDeleted(id, false);
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
        } else if (colorRepository.existsCode(code, c.getId())) {
            errors.put("code", "Mã màu \"" + code + "\" đã được dùng (có thể ở màu trong Thùng rác).");
        }
        if (!HEX.matcher(hex).matches()) {
            errors.put("hexCode", "Mã màu hex phải có dạng #RRGGBB.");
        }
        if (c.getId() > 0) {
            Color existing = colorRepository.findById(c.getId());
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
            colorRepository.update(c);
        } else {
            c.setId(colorRepository.insert(c));
        }
    }

    /**
     * Xóa mềm. Chặn nếu màu đang gắn biến thể của sản phẩm chưa xóa.
     */
    public void deleteColor(int id) {
        Color c = colorRepository.findById(id);
        if (c == null || c.isDeleted()) {
            throw new ValidationException(null, "Màu không tồn tại hoặc đã nằm trong Thùng rác.");
        }
        int used = colorRepository.countProductsUsing(id);
        if (used > 0) {
            throw new ValidationException(null, "Không thể xóa màu \"" + c.getName() + "\" vì đang có trong biến thể của "
                    + used + " sản phẩm.");
        }
        colorRepository.setDeleted(id, true);
    }

    /**
     * name duy nhất, sortOrder >= 0. Sai thì ném ValidationException (name, sortOrder).
     */
    public void saveSize(Size s) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = s.getName() == null ? "" : s.getName().trim().toUpperCase(Locale.ROOT);

        if (name.isEmpty() || name.length() > 20) {
            errors.put("name", "Tên size từ 1 đến 20 ký tự.");
        } else if (sizeRepository.existsName(name, s.getId())) {
            errors.put("name", "Size \"" + name + "\" đã tồn tại (có thể ở size trong Thùng rác).");
        }
        if (s.getSortOrder() < 0 || s.getSortOrder() > 99) {
            errors.put("sortOrder", "Thứ tự là số nguyên từ 0 đến 99.");
        }
        if (s.getId() > 0) {
            Size existing = sizeRepository.findById(s.getId());
            if (existing == null || existing.isDeleted()) {
                errors.put("name", "Size không tồn tại hoặc đã bị xóa.");
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        s.setName(name);
        if (s.getId() > 0) {
            sizeRepository.update(s);
        } else {
            s.setId(sizeRepository.insert(s));
        }
    }

    /**
     * Xóa mềm, tương tự màu.
     */
    public void deleteSize(int id) {
        Size s = sizeRepository.findById(id);
        if (s == null || s.isDeleted()) {
            throw new ValidationException(null, "Size không tồn tại hoặc đã nằm trong Thùng rác.");
        }
        int used = sizeRepository.countProductsUsing(id);
        if (used > 0) {
            throw new ValidationException(null, "Không thể xóa size \"" + s.getName() + "\" vì đang có trong biến thể của "
                    + used + " sản phẩm.");
        }
        sizeRepository.setDeleted(id, true);
    }
}
