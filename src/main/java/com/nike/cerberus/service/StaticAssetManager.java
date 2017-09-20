package com.nike.cerberus.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.nike.backstopper.exception.ApiException;
import com.nike.cerberus.error.DefaultApiError;
import com.nike.riposte.server.http.RequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Cerberus Dashboard file objects including images, HTML, etc.
 */
public class StaticAssetManager {

    private static final Logger logger = LoggerFactory.getLogger(StaticAssetManager.class);

    private static final String DEFAULT_MIME_TYPE = "text/plain";

    private static final String REQUEST_PATH_FILENAME_SEPARATOR = "dashboard/";

    private static final String DEFAULT_DASHBOARD_ASSET_FILE_NAME = "index.html";

    public static final ImmutableMap<String, String> FILE_EXT_TO_MIME_TYPE_MAP = ImmutableMap.<String, String>builder()
            .put("svg", "image/svg+xml")
            .put("js", "text/javascript")
            .put("ico", "image/x-icon")
            .put("css", "text/css")
            .put("html", "text/html")
            .put("json", "application/json")
            .build();

    private final String resourceFolder;

    private final ImmutableMap<String, AssetResourceFile> assetFileCache;

    public StaticAssetManager(String resourceFolderAbsolutePath) {
        this.resourceFolder = resourceFolderAbsolutePath;
        assetFileCache = init();
    }

    public AssetResourceFile get(RequestInfo<Void> request) {
        String filename = StringUtils.substringAfterLast(request.getPath(), REQUEST_PATH_FILENAME_SEPARATOR);
        logger.info("XXXXX Filename (before empty check) is: '{}'", filename);
        filename = filename.isEmpty() ? DEFAULT_DASHBOARD_ASSET_FILE_NAME : filename;
        logger.info("XXXXX Filename (after empty check) is: '{}'", filename);
        logger.info("XXXXX Map keys: {}",  assetFileCache.keySet());

        if (assetFileCache.containsKey(filename)) {
            logger.info("XXXXX Map contains key");
            return assetFileCache.get(filename);
        } else {
            throw ApiException.newBuilder()
                    .withApiErrors(DefaultApiError.FAILED_TO_READ_DASHBOARD_ASSET_CONTENT)
                    .withExceptionMessage("Could not load dashboard asset: " + filename)
                    .build();
        }
    }

    private ImmutableMap<String, AssetResourceFile> init() {
        return ImmutableMap.<String, AssetResourceFile>builder()
                .putAll(loadFiles(resourceFolder))
                .build();
    }

    private Map<String, AssetResourceFile> loadFiles(String rootFolderAbsolutePath) {
        return loadFiles(Maps.newHashMap(), rootFolderAbsolutePath);
    }

    private Map<String, AssetResourceFile> loadFiles(Map<String, AssetResourceFile> assetFiles, String rootFolder) {

        try (DirectoryStream<Path> dashDir = Files.newDirectoryStream(Paths.get(rootFolder))) {
            dashDir.iterator().forEachRemaining(path -> {
                if (Files.isDirectory(path)) {
                    assetFiles.putAll(
                            loadFiles(assetFiles, path.toString()));
                }
                logger.info("XXXX Found file: name: {}, path: {}", path.getFileName(), path.toString());
                AssetResourceFile resource = create(
                        path.getFileName().toString(),
                        path.toAbsolutePath().toString(),
                        resourceFolder);

                logger.info("XXXX File relative path: {}", resource.getRelativePath());
                assetFiles.put(resource.getRelativePath(), resource);
                logger.info("XXXX dashboardAssets: {}", assetFiles.keySet());
            });
        } catch (IOException ioe) {
            logger.info("Could not create dashboard directory stream");
        }

        return assetFiles;
    }

//    public Optional<AssetResourceFile> get(String absolutePath) {
//        String relativePath = getRelativePath(absolutePath, resourceFolder);
//        return assetFileCache.containsKey(relativePath) ?
//                Optional.of(assetFileCache.get(relativePath)) :
//                Optional.empty();
//    }

    private AssetResourceFile create(String filename, String filePath, String rootDirectoryPath) {
        return new AssetResourceFile(
                filename,
                getRelativePath(filePath, rootDirectoryPath),
                getMimeTypeForFileFromName(filename),
                ImmutableList.<Byte>builder()
                        .addAll(getFileContents(filePath))
                        .build()
        );
    }

    /**
     * @return The contents of the given file in bytes
     */
    private static List<Byte> getFileContents(String filePath) {
        try {
            byte[] contents = filePath.getBytes(Charset.defaultCharset());
//            byte[] contents = Files.readAllBytes(file.toPath());
            return Bytes.asList(contents);
        } catch (NullPointerException ioe) {
            throw new IllegalArgumentException("Could not read contents of file: " + filePath, ioe);
        }
    }

    /**
     * @return The appropriate MIME type for the given file
     */
    private static String getMimeTypeForFileFromName(String fileName) {
        String fileExtension = StringUtils.substringAfterLast(fileName, ".");

        if (FILE_EXT_TO_MIME_TYPE_MAP.containsKey(fileExtension)) {
            return FILE_EXT_TO_MIME_TYPE_MAP.get(fileExtension);
        } else {
            logger.error("Could not determine MIME type for file: {}, using type '{}'", fileName, DEFAULT_MIME_TYPE);
            return DEFAULT_MIME_TYPE;
        }
    }

    /**
     * Returns the relative path of a file from a given root folder context
     * @param filePath  The full path for the file which to get the relative path for
     * @param rootFolderPath    The full path of the root folder, which to remove from the file path
     * @return  The relative path of the file (excluding the given root folder)
     */
    private static String getRelativePath(String filePath, String rootFolderPath) {
        logger.info("ZZZZ Full file path: {}, rootFolderPath file path: {}", filePath, rootFolderPath);
        return StringUtils.substringAfterLast(filePath, rootFolderPath);
    }

    /**
     * Resource file object (e.g. image file, HTML file, JSON, etc.)
     */
    public static class AssetResourceFile {

        private String fileName;

        private String relativePath;

        private String mimeType;

        private ImmutableList<Byte> fileContents;

        public AssetResourceFile(String fileName,
                                 String relativePath,
                                 String mimeType,
                                 ImmutableList<Byte> fileContents) {
            this.fileName = fileName;
            this.relativePath = relativePath;
            this.mimeType = mimeType;
            this.fileContents = fileContents;
        }

        public String getFileName() {
            return fileName;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public String getMimeType() {
            return mimeType;
        }

        public ImmutableList<Byte> getFileContents() {
            return fileContents;
        }
    }
}
