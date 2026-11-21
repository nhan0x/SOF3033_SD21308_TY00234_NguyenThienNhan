package poly.coffee.util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.ServletContext;
import javax.servlet.http.Part;

/** Luu / xoa file upload (hinh do uong) trong thu muc /images cua webapp. */
public class FileUtil {
    private static final String FOLDER = "/images";

    /** Luu file upload, tra ve ten file moi; null neu khong co file. */
    public static String save(ServletContext ctx, Part part) throws IOException {
        if (part == null || part.getSize() == 0) return null;
        String submitted = part.getSubmittedFileName();
        String ext = submitted != null && submitted.contains(".")
                ? submitted.substring(submitted.lastIndexOf('.')).toLowerCase() : "";
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
            throw new IOException("Chi chap nhan file anh (jpg, png, gif, webp)");
        }
        String dir = ctx.getRealPath(FOLDER);
        new File(dir).mkdirs();
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        part.write(dir + File.separator + name);
        return name;
    }

    public static void delete(ServletContext ctx, String fileName) {
        if (fileName == null || fileName.isEmpty() || fileName.contains("..")) return;
        File f = new File(ctx.getRealPath(FOLDER), fileName);
        if (f.isFile()) f.delete();
    }
}
