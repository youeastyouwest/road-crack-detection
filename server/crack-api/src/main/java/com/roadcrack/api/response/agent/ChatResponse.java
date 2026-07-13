package com.roadcrack.api.response.agent;


public class ChatResponse {

    private String sessionId;

    private String question;

    private String answer;

    private String dataSource;

    private long timestamp;

    public ChatResponse() {}

    public ChatResponse(String sessionId, String question, String answer, String dataSource, long timestamp) {
        this.sessionId = sessionId;
        this.question = question;
        this.answer = answer;
        this.dataSource = dataSource;
        this.timestamp = timestamp;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}