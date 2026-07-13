package com.roadcrack.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crack.algorithm")
public class AlgorithmClientProperties {

    private boolean mockEnabled = true;
    private String baseUrl = "http://localhost:8000";
    private String analyzePath = "/detect/base64";
    private String videoPath = "/detect/video";
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 15000;
    private int videoReadTimeoutMillis = 300000;

    public boolean isMockEnabled() {
        return mockEnabled;
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAnalyzePath() {
        return analyzePath;
    }

    public void setAnalyzePath(String analyzePath) {
        this.analyzePath = analyzePath;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public int getVideoReadTimeoutMillis() {
        return videoReadTimeoutMillis;
    }

    public void setVideoReadTimeoutMillis(int videoReadTimeoutMillis) {
        this.videoReadTimeoutMillis = videoReadTimeoutMillis;
    }
}