package com.hjq.CodeLess;

import com.hjq.CodeLess.exception.BusinessException;
import com.hjq.CodeLess.model.entity.User;
import com.hjq.CodeLess.service.UserAvatarService;
import com.hjq.CodeLess.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAvatarServiceTest {

    private static final String BASE_URL = "http://example.test/api/user/avatar";
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/lXcAAAAASUVORK5CYII=");

    @TempDir
    Path tempDir;

    @Test
    void uploadSavesAvatarAndServesImage() throws Exception {
        UserService userService = mock(UserService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        User loginUser = new User();
        loginUser.setId(123L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.updateById(any(User.class))).thenReturn(true);
        UserAvatarService service = new UserAvatarService(userService, tempDir.toString(), BASE_URL);

        String url = service.uploadAndSave(new MockMultipartFile("file", "avatar.png", "image/png", PNG), request);
        String fileName = url.substring((BASE_URL + "/").length());

        assertTrue(fileName.matches("[0-9a-f]{32}\\.png"));
        assertTrue(Files.exists(tempDir.resolve(fileName)));
        assertEquals(MediaType.IMAGE_PNG, service.getAvatar(fileName).getHeaders().getContentType());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        assertEquals(url, userCaptor.getValue().getUserAvatar());
        assertEquals(123L, userCaptor.getValue().getId());
    }

    @Test
    void rejectsNonImageContentAndUnsafeFileName() {
        UserService userService = mock(UserService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        User loginUser = new User();
        loginUser.setId(123L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        UserAvatarService service = new UserAvatarService(userService, tempDir.toString(), BASE_URL);

        assertThrows(BusinessException.class, () -> service.uploadAndSave(
                new MockMultipartFile("file", "avatar.png", "image/png", "not an image".getBytes()), request));
        assertFalse(Files.exists(tempDir.resolve("avatar.png")));
        assertEquals(404, service.getAvatar("../avatar.png").getStatusCode().value());
    }

    @Test
    void acceptsJpgUploadEvenWithImageJpgMimeType() throws Exception {
        UserService userService = mock(UserService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        User loginUser = new User();
        loginUser.setId(123L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.updateById(any(User.class))).thenReturn(true);
        UserAvatarService service = new UserAvatarService(userService, tempDir.toString(), BASE_URL);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg", output));
        byte[] jpg = output.toByteArray();

        String url = service.uploadAndSave(new MockMultipartFile("file", "photo.JPG", "image/jpg", jpg), request);
        String fileName = url.substring((BASE_URL + "/").length());

        assertTrue(fileName.matches("[0-9a-f]{32}\\.jpg"));
        assertEquals(MediaType.IMAGE_JPEG, service.getAvatar(fileName).getHeaders().getContentType());
        assertTrue(Files.exists(tempDir.resolve(fileName)));
    }
}
