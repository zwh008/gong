package com.example.wifipasswordviewer;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.WifiViewHolder> {

    private Context context;
    private List<WifiInfo> wifiList;

    public WifiAdapter(Context context, List<WifiInfo> wifiList) {
        this.context = context;
        this.wifiList = wifiList;
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.wifi_item, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder holder, int position) {
        WifiInfo wifiInfo = wifiList.get(position);
        holder.tvSsid.setText(wifiInfo.getSsid());
        holder.tvPassword.setText(context.getString(R.string.password_label) + wifiInfo.getPassword());

        // 复制按钮点击事件
        holder.btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = ClipData.newPlainText("WiFi Password", wifiInfo.getPassword());
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(context, R.string.copy_success, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return wifiList != null ? wifiList.size() : 0;
    }

    public static class WifiViewHolder extends RecyclerView.ViewHolder {
        ImageView wifiIcon;
        TextView tvSsid;
        TextView tvPassword;
        Button btnCopy;

        public WifiViewHolder(@NonNull View itemView) {
            super(itemView);
            wifiIcon = itemView.findViewById(R.id.wifi_icon);
            tvSsid = itemView.findViewById(R.id.tv_ssid);
            tvPassword = itemView.findViewById(R.id.tv_password);
            btnCopy = itemView.findViewById(R.id.btn_copy);
        }
    }
}