package com.roadcrack.api.response.map;

public class MapDamageTypeRatioResponse {
    private String damageType;
    private long count;
    private double ratio;
    public String getDamageType() { return damageType; }
    public void setDamageType(String v) { this.damageType = v; }
    public long getCount() { return count; }
    public void setCount(long v) { this.count = v; }
    public double getRatio() { return ratio; }
    public void setRatio(double v) { this.ratio = v; }
}