package com.example.wifipasswordviewer;

public class WifiInfo {
    private String ssid;      // WiFi名称
    private String password;  // WiFi密码
    
    public WifiInfo(String ssid, String password) {
        this.ssid = ssid;
        this.password = password;
    }
    
    public String getSsid() {
        return ssid;
    }
    
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}