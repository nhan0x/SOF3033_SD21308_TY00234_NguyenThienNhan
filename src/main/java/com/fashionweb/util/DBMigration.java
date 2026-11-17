package com.fashionweb.util;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * Tự tạo và cập nhật bảng bằng Flyway khi Tomcat khởi động (AppStartupListener gọi migrate()).
 *
 * <p>Cách hoạt động: Flyway đọc các file src/main/resources/db/migration/V&lt;số&gt;__&lt;tên&gt;.sql theo thứ tự số,
 * chạy những file chưa chạy và ghi lại vào bảng flyway_schema_history. File đã chạy thì không chạy lại.
 * <p>Muốn đổi bảng: thêm file mới (V3__them_cot_x.sql). Không sửa file đã chạy, Flyway sẽ báo lỗi checksum.
 */
public final class DBMigration {

    private DBMigration() {
    }

    /** Chạy các migration còn thiếu. Trả số file vừa chạy. Lỗi SQL thì ném FlywayException (ứng dụng dừng khởi động). */
    public static int migrate() {
        Flyway flyway = Flyway.configure(DBMigration.class.getClassLoader())
                .dataSource(DBContext.url(), DBContext.username(), DBContext.password())
                .locations("classpath:db/migration")
                .load();
        MigrateResult result = flyway.migrate();
        return result.migrationsExecuted;
    }
}
