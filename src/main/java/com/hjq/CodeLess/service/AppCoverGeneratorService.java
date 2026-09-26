package com.hjq.CodeLess.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hjq.CodeLess.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 使用本机 Chrome / Edge 的无头模式为已生成的应用截取首页封面。
 */
@Service
@Slf4j
public class AppCoverGeneratorService {

    private static final String COVER_FILE_NAME = "cover.png";

    @Value("${app.cover.enabled:true}")
    private boolean enabled;

    @Value("${app.preview.base-url}")
    private String previewBaseUrl;

    @Value("${app.cover.browser-path:}")
    private String configuredBrowserPath;

    @Value("${app.cover.width:1200}")
    private int coverWidth;

    @Value("${app.cover.height:675}")
    private int coverHeight;

    @Value("${app.cover.timeout-seconds:20}")
    private long timeoutSeconds;

    @Value("${app.cover.render-wait-millis:3000}")
    private long renderWaitMillis;

    /**
     * 截取应用预览页，并返回可以直接保存到 app.cover 的公开地址。
     * 截图失败时返回 empty，避免影响已经成功生成的应用代码。
     */
    public Optional<String> generateCover(Long appId, String codeGenType) {
        if (!enabled) {
            log.debug("应用封面自动截图已关闭");
            return Optional.empty();
        }
        if (appId == null || StrUtil.isBlank(codeGenType)) {
            log.warn("跳过应用封面截图：应用 ID 或生成类型为空");
            return Optional.empty();
        }

        Optional<Path> browserPath = findBrowserExecutable();
        if (browserPath.isEmpty()) {
            log.warn("未找到 Chrome 或 Edge，无法为应用 {} 自动生成封面", appId);
            return Optional.empty();
        }

        String staticKey = codeGenType + "_" + appId;
        Path appDir = Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, staticKey).toAbsolutePath().normalize();
        if (!Files.isDirectory(appDir)) {
            log.warn("应用 {} 的生成目录不存在，无法生成封面：{}", appId, appDir);
            return Optional.empty();
        }

        Path temporaryCover = appDir.resolve("cover-" + UUID.randomUUID() + ".png");
        Path finalCover = appDir.resolve(COVER_FILE_NAME);
        Path temporaryProfile = Path.of(System.getProperty("java.io.tmpdir"), "codeless-cover-" + UUID.randomUUID());
        String previewUrl = stripTrailingSlash(previewBaseUrl) + "/" + staticKey + "/";

        Process process = null;
        try {
            Files.createDirectories(temporaryProfile);
            List<String> command = buildScreenshotCommand(
                    browserPath.get(), temporaryCover, temporaryProfile, previewUrl);
            process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();

            boolean finished = process.waitFor(Math.max(timeoutSeconds, 1), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("应用 {} 封面截图超时（{} 秒）", appId, timeoutSeconds);
                return Optional.empty();
            }
            if (process.exitValue() != 0 || !Files.isRegularFile(temporaryCover)
                    || Files.size(temporaryCover) == 0) {
                log.warn("应用 {} 封面截图失败，浏览器退出码：{}", appId, process.exitValue());
                return Optional.empty();
            }

            Files.move(temporaryCover, finalCover, StandardCopyOption.REPLACE_EXISTING);
            String coverUrl = previewUrl + COVER_FILE_NAME + "?v=" + System.currentTimeMillis();
            log.info("应用 {} 封面生成成功：{}", appId, finalCover);
            return Optional.of(coverUrl);
        } catch (Exception e) {
            log.warn("应用 {} 封面截图失败：{}", appId, e.getMessage(), e);
            return Optional.empty();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            FileUtil.del(temporaryCover.toFile());
            FileUtil.del(temporaryProfile.toFile());
        }
    }

    private List<String> buildScreenshotCommand(Path browserPath, Path outputPath,
                                                 Path profilePath, String previewUrl) {
        List<String> command = new ArrayList<>();
        command.add(browserPath.toString());
        command.add("--headless=new");
        command.add("--disable-gpu");
        command.add("--disable-extensions");
        command.add("--disable-sync");
        command.add("--hide-scrollbars");
        command.add("--no-first-run");
        command.add("--no-default-browser-check");
        command.add("--run-all-compositor-stages-before-draw");
        command.add("--window-size=" + Math.max(coverWidth, 320) + "," + Math.max(coverHeight, 180));
        command.add("--virtual-time-budget=" + Math.max(renderWaitMillis, 0));
        command.add("--user-data-dir=" + profilePath);
        command.add("--screenshot=" + outputPath);
        command.add(previewUrl);
        return command;
    }

    private Optional<Path> findBrowserExecutable() {
        if (StrUtil.isNotBlank(configuredBrowserPath)) {
            Path configuredPath = Path.of(configuredBrowserPath.trim());
            if (Files.isRegularFile(configuredPath)) {
                return Optional.of(configuredPath);
            }
            log.warn("配置的浏览器不存在：{}", configuredPath);
        }

        List<String> candidates = List.of(
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                System.getProperty("user.home") + "\\AppData\\Local\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "/usr/bin/google-chrome",
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
        );
        return candidates.stream()
                .map(Path::of)
                .filter(Files::isRegularFile)
                .findFirst();
    }

    private String stripTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
