package com.busgallery.busgallery.service.storage;

import java.io.InputStream;

public interface StorageService {

    StorageObject upload(String objectName, InputStream inputStream, long size, String contentType);

    void remove(String objectUrl);

    String buildUrl(String objectName);
}