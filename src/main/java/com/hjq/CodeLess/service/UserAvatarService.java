package com.hjq.CodeLess.service;

import com.hjq.CodeLess.exception.BusinessException;
import com.hjq.CodeLess.exception.ErrorCode;
import com.hjq.CodeLess.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserAvatarService {

    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("[0-9a-f]{32}\\.(png|jpg|webp)");

    private final UserService userService;
    private final Path storageRoot;
    private final String publicBaseUrl;

    public UserAvatarService(UserService userService,
                             @Value("${app.avatar.storage-dir}") String storageDir,
                             @Value("${app.avatar.public-base-url}") String publicBaseUrl) {
        this.userService = userService;
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.trim().replaceAll("/+$", "");
    }

    public String uploadAndSave(MultipartFile file, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (file == null || file.isEmpty() || file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择不超过 5 MB 的图片");
        }

        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "读取头像文件失败");
        }
        if (bytes.length == 0 || bytes.length > MAX_AVATAR_BYTES) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择不超过 5 MB 的图片");
        }
        String extension = detectExtension(bytes);
        if (extension == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅支持 PNG、JPG/JPEG 或 WebP 图片");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = storageRoot.resolve(fileName);
        String avatarUrl = publicBaseUrl + "/" + fileName;
        boolean saved = false;
        try {
            Files.createDirectories(storageRoot);
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            User update = new User();
            update.setId(loginUser.getId());
            update.setUserAvatar(avatarUrl);
            if (!userService.updateById(update)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存头像失败");
            }
            saved = true;
            deletePreviousAvatar(loginUser.getUserAvatar());
            return avatarUrl;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存头像文件失败");
        } finally {
            if (!saved) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException ignored) {
                    // 写入数据库失败时尽量清理未使用的文件。
                }
            }
        }
    }

    public ResponseEntity<Resource> getAvatar(String fileName) {
        if (fileName == null || !SAFE_FILE_NAME.matcher(fileName).matches()) {
            return ResponseEntity.notFound().build();
        }
        Path file = storageRoot.resolve(fileName);
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = fileName.endsWith(".png") ? MediaType.IMAGE_PNG
                : fileName.endsWith(".jpg") ? MediaType.IMAGE_JPEG
                : MediaType.parseMediaType("image/webp");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(file));
    }

    private void deletePreviousAvatar(String previousUrl) {
        String prefix = publicBaseUrl + "/";
        if (previousUrl == null || !previousUrl.startsWith(prefix)) {
            return;
        }
        String previousFileName = previousUrl.substring(prefix.length());
        if (!SAFE_FILE_NAME.matcher(previousFileName).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(storageRoot.resolve(previousFileName));
        } catch (IOException ignored) {
            // 旧头像清理失败不影响新头像的保存。
        }
    }

    private static String detectExtension(byte[] bytes) {
        if (bytes.length >= 24 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P'
                && bytes[2] == 'N' && bytes[3] == 'G' && bytes[4] == 13
                && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10
                && bytes[12] == 'I' && bytes[13] == 'H'
                && bytes[14] == 'D' && bytes[15] == 'R') {
            return "png";
        }
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
            return "jpg";
        }
        if (bytes.length >= 16 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P'
                && bytes[12] == 'V' && bytes[13] == 'P' && bytes[14] == '8'
                && (bytes[15] == ' ' || bytes[15] == 'L' || bytes[15] == 'X')) {
            return "webp";
        }
        return null;
    }
}
