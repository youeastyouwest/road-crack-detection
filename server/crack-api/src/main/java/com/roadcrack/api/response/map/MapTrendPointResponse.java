package com.roadcrack.api.response.map;

import java.time.LocalDate;

public class MapTrendPointResponse {
    private LocalDate date;
    private long count;
    private long repairedCount;
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public long getRepairedCount() { return repairedCount; }
    public void setRepairedCount(long v) { this.repairedCount = v; }
}