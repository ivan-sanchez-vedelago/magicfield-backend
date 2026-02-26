package com.magicfield.backend.service;

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

    public String upload(Long productId, MultipartFile file) throws IOException {

        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        Bucket bucket = StorageClient.getInstance().bucket();

        Blob blob = bucket.create(
                "products/" + productId + "/" + fileName,
                file.getBytes(),
                file.getContentType()
        );

        return blob.getMediaLink();
    }

    public void deleteByUrl(String url) {

        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8);
        String path = decoded.substring(
                decoded.indexOf("/o/") + 3,
                decoded.indexOf("?")
        );

        Bucket bucket = StorageClient.getInstance().bucket();
        var blob = bucket.get(path);
        if (blob != null) {
            blob.delete();
        }
    }
}