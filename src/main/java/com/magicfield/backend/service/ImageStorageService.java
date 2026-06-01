package com.magicfield.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Blob;
import com.google.firebase.cloud.StorageClient;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    public String upload(UUID productId, MultipartFile file) throws IOException {

        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.create(
                "products/" + productId + "/" + fileName,
                file.getBytes(),
                file.getContentType()
        );

        return blob.getMediaLink();
    }

    public String uploadBanner(Long bannerId, MultipartFile file) throws IOException {

        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.create(
                "banners/" + bannerId + "/" + fileName,
                file.getBytes(),
                file.getContentType()
        );

        return blob.getMediaLink();
    }

    public void deleteByUrl(String url) {

        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
        int startIndex = decoded.indexOf("/o/");
        int endIndex   = decoded.indexOf("?");

        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex + 3) {
            log.warn("[Storage] URL con formato inesperado, no se puede eliminar: {}", url);
            return;
        }

        String path = decoded.substring(startIndex + 3, endIndex);

        Bucket bucket = StorageClient.getInstance().bucket();
        var blob = bucket.get(path);
        if (blob != null) {
            blob.delete();
        }
    }
}