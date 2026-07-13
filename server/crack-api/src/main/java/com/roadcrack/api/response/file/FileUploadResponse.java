package com.roadcrack.api.response.file;

public record FileUploadResponse(
        String url,
        String originalName,
        long size,
        String contentType
) {
}