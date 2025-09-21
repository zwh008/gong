package com.example.wifipasswordviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private Button btnLoadWifi;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private WifiAdapter wifiAdapter;
    private List<WifiInfo> wifiList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化UI组件
        btnLoadWifi = findViewById(R.id.btn_load_wifi);
        recyclerView = findViewById(R.id.recycler_view);
        progressBar = findViewById(R.id.progress_bar);

        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        wifiAdapter = new WifiAdapter(this, wifiList);
        recyclerView.setAdapter(wifiAdapter);

        // 检查并请求权限
        if (!checkPermissions()) {
            requestPermissions();
        }

        // 设置加载按钮点击事件
        btnLoadWifi.setOnClickListener(v -> loadWifiInfo());
    }

    // 检查是否有必要的权限
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int fineLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            int coarseLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            int accessWifiStatePermission = checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE);
            
            return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                   coarseLocationPermission == PackageManager.PERMISSION_GRANTED &&
                   accessWifiStatePermission == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // 请求必要的权限
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_WIFI_STATE
                    },
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 加载WiFi信息
    private void loadWifiInfo() {
        if (!checkPermissions()) {
            Toast.makeText(this, R.string.location_permission_needed, Toast.LENGTH_SHORT).show();
            requestPermissions();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLoadWifi.setEnabled(false);

        new Thread(() -> {
            try {
                wifiList.clear();
                
                // 方法1: 使用WifiManager读取已保存的WiFi配置
                readWifiConfigFromWifiManager();
                
                // 方法2: 尝试从wpa_supplicant.conf文件读取（需要root权限）
                if (wifiList.isEmpty()) {
                    readWifiConfigFromSupplicantFile();
                }
                
                // 在UI线程更新RecyclerView
                runOnUiThread(() -> {
                    if (wifiList.isEmpty()) {
                        Toast.makeText(MainActivity.this, R.string.no_wifi_data, Toast.LENGTH_SHORT).show();
                    }
                    wifiAdapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    btnLoadWifi.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "加载WiFi信息失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnLoadWifi.setEnabled(true);
                });
                e.printStackTrace();
            }
        }).start();
    }

    // 从WifiManager读取已保存的WiFi配置
    private void readWifiConfigFromWifiManager() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            // Android 10及以上版本需要位置权限且启用位置服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // 对于Android 10及以上，由于权限限制，我们无法直接获取密码
                // 这里添加一些模拟数据用于演示
                // 注意：在实际设备上，从Android 10开始，普通应用无法获取保存的WiFi密码
                addDemoWifiData();
            } else {
                // 对于Android 9及以下，可以尝试通过反射获取已保存的WiFi配置
                try {
                    List<WifiConfiguration> configurations = getWifiConfigurations(wifiManager);
                    for (WifiConfiguration config : configurations) {
                        String ssid = config.SSID.replace("\"", ""); // 移除引号
                        String password = getWifiPassword(config);
                        if (ssid != null && !ssid.isEmpty() && password != null) {
                            wifiList.add(new WifiInfo(ssid, password));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 如果反射失败，添加模拟数据
                    addDemoWifiData();
                }
            }
        }
    }

    // 使用反射获取WiFi配置
    private List<WifiConfiguration> getWifiConfigurations(WifiManager wifiManager) throws Exception {
        try {
            return (List<WifiConfiguration>) wifiManager.getClass().getMethod("getConfiguredNetworks").invoke(wifiManager);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 从WifiConfiguration获取密码
    private String getWifiPassword(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
            return "无需密码";
        } else if (config.preSharedKey != null) {
            return config.preSharedKey.replace("\"", "");
        } else if (config.wepKeys[0] != null) {
            return config.wepKeys[0].replace("\"", "");
        }
        return "无法获取密码";
    }

    // 尝试从wpa_supplicant.conf文件读取WiFi配置（需要root权限）
    private void readWifiConfigFromSupplicantFile() {
        try {
            File supplicantFile = new File("/data/misc/wifi/wpa_supplicant.conf");
            if (supplicantFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(supplicantFile));
                String line;
                String ssid = null;
                String password = null;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("ssid=")) {
                        ssid = line.substring(5).replace("\"", "");
                    } else if (line.startsWith("psk=")) {
                        password = line.substring(4).replace("\"", "");
                    } else if (line.equals("}") && ssid != null) {
                        wifiList.add(new WifiInfo(ssid, password != null ? password : "无法获取密码"));
                        ssid = null;
                        password = null;
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 添加模拟WiFi数据用于演示
    private void addDemoWifiData() {
        wifiList.add(new WifiInfo("示例WiFi 1", "12345678"));
        wifiList.add(new WifiInfo("示例WiFi 2", "password123"));
        wifiList.add(new WifiInfo("示例WiFi 3", "87654321"));
        wifiList.add(new WifiInfo("示例WiFi 4", "adminadmin"));
        wifiList.add(new WifiInfo("示例WiFi 5", "11112222"));
    }
}