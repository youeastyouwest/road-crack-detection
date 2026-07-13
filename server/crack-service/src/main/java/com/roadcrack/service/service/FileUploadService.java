package com.roadcrack.service.service;

import com.roadcrack.api.response.file.FileUploadResponse;

public interface FileUploadService {
    FileUploadResponse upload(byte[] fileBytes, String filename, String contentType);
}