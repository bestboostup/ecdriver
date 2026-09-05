package com.example.ecdriver;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private LinearProgressIndicator progressBar;
    private RelativeLayout progresslayout;
    private TextView percentageText;
    private Button busnumber1, busnumber2, busnumber3, busnumber4, busnumber5;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        progressBar = findViewById(R.id.progressBar);
        percentageText = findViewById(R.id.percentageText);
        progresslayout = findViewById(R.id.progresslayout);

        busnumber1 = findViewById(R.id.busnumber1);
        busnumber2 = findViewById(R.id.busnumber2);
        busnumber3 = findViewById(R.id.busnumber3);
        busnumber4 = findViewById(R.id.busnumber4);
        busnumber5 = findViewById(R.id.busnumber5);

        // অ্যাপ প্রথমবার ওপেন করলে বা পারমিশন না থাকলে পপআপ ডায়ালগ দেখানো
        checkAndShowPermissionDialog();

        // অ্যাপ স্টارت হওয়ার সময় আগের কোনো ট্রিপ আটকে আছে কিনা চেক করা
        checkActiveTripsOnStartup();

        //=========== Button Click Listeners =========================
        busnumber1.setOnClickListener(view -> openBusControl("bus_01"));
        busnumber2.setOnClickListener(view -> openBusControl("bus_02"));
        busnumber3.setOnClickListener(view -> openBusControl("bus_03"));
        busnumber4.setOnClickListener(view -> openBusControl("bus_04"));
        busnumber5.setOnClickListener(view -> openBusControl("bus_05"));
    }

    // ফায়ারবেস থেকে সব বাসের রিয়েল-টাইম স্ট্যাটাস ট্র্যাক করার মেথড
    private void listenBusStatuses() {
        DatabaseReference busesRef = FirebaseDatabase.getInstance("https://ec-driver-app-a619a-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("buses");

        busesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                    String busId = busSnapshot.getKey();
                    String status = busSnapshot.child("status").getValue(String.class);

                    Button targetButton = null;
                    if ("bus_01".equals(busId)) targetButton = busnumber1;
                    else if ("bus_02".equals(busId)) targetButton = busnumber2;
                    else if ("bus_03".equals(busId)) targetButton = busnumber3;
                    else if ("bus_04".equals(busId)) targetButton = busnumber4;
                    else if ("bus_05".equals(busId)) targetButton = busnumber5;

                    if (targetButton != null) {
                        targetButton.setEnabled(true); // বাটন সবসময় সচল রাখা হলো যাতে ক্লিক করা যায়

                        if ("ACTIVE".equals(status)) {
                            // বাস চালু থাকলে শুধু কালার কালো হবে
                            setButtonStyle(targetButton, Color.BLACK);
                        } else {
                            // বন্ধ থাকলে সবুজ কালার হবে
                            setButtonStyle(targetButton, Color.parseColor("#006C32"));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // বাটনের শেপ ও কালার পরিবর্তনের হেল্পার মেথড
    private void setButtonStyle(Button button, int colorHex) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setColor(colorHex);

        float radiusInPx = getResources().getDisplayMetrics().density * 8; // কর্নার রেডিয়াস
        drawable.setCornerRadius(radiusInPx);

        button.setBackground(drawable);
    }

    private String getBusDisplayName(String busId) {
        if (busId == null) return "বাস";
        String number = busId.replace("bus_", "");
        return "বাস " + number;
    }

    private void checkAndShowPermissionDialog() {
        boolean isLocationGranted = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean isNotificationGranted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotificationGranted = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        if (isLocationGranted && isNotificationGranted) {
            return;
        }

        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isDialogShown = prefs.getBoolean("permission_dialog_shown", false);

        if (isDialogShown) {
            return;
        }

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("প্রয়োজনীয় পারমিশন")
                .setMessage("বাসের লাইভ লোকেশন ট্র্যাক করতে এবং নোটিফিকেশন পাঠাতে এই অ্যাপটির লোকেশন ও নোটিফিকেশন পারমিশন প্রয়োজন। দয়া করে পারমিশন দিন।")
                .setPositiveButton("এলাও করুন", (dialog, which) -> {
                    prefs.edit().putBoolean("permission_dialog_shown", true).apply();
                    requestAllPermissions();
                })
                .setNegativeButton("পরে", (dialog, which) -> {
                    prefs.edit().putBoolean("permission_dialog_shown", true).apply();
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }

    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            }, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    // নির্দিষ্ট busId সহ কন্ট্রোল প্যানেলে নিয়ে যাওয়ার আগে মাল্টি-ট্রিপ রেস্ট্রিকশন চেক করার মেথড
    private void openBusControl(String targetBusId) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "দয়া করে লোকেশন পারমিশন দিন", Toast.LENGTH_SHORT).show();
            checkAndShowPermissionDialog();
            return;
        }

        String currentDeviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        DatabaseReference busesRef = FirebaseDatabase.getInstance("https://ec-driver-app-a619a-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("buses");

        busesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String activeBusFound = null;
                String activeBusOwnerDevice = null;

                // চেক করা বর্তমান ডিভাইস থেকে অন্য কোনো বাসের ট্রিপ একটিভ আছে কিনা
                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                    String busKey = busSnapshot.getKey();
                    String status = busSnapshot.child("status").getValue(String.class);
                    String savedDeviceId = busSnapshot.child("activeDeviceId").getValue(String.class);

                    if ("ACTIVE".equals(status) && currentDeviceId.equals(savedDeviceId)) {
                        activeBusFound = busKey;
                        activeBusOwnerDevice = savedDeviceId;
                        break;
                    }
                }

                // যদি অন্য কোনো বাসের ট্রিপ রানিং থাকে এবং ব্যবহারকারী সম্পূর্ণ ভিন্ন অন্য কোনো বাসে ক্লিক করে
                if (activeBusFound != null && !activeBusFound.equals(targetBusId)) {
                    String busName = getBusDisplayName(activeBusFound);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("সতর্কবার্তা")
                            .setMessage("আপনি ইতিমধ্যে " + busName + " এর যাত্রা চালু করেছেন। আগে সেটার যাত্রা শেষ বা সমস্যা আপডেট করুন।")
                            .setPositiveButton("ঠিক আছে", (dialog, which) -> dialog.dismiss())
                            .setCancelable(false)
                            .show();
                } else {
                    // ড্রাইভার নিজের চালু করা বাসেই ক্লিক করলে অথবা অন্য কোনো বাস চালু না থাকলে কন্ট্রোল প্যানেলে যাবে
                    Intent intent = new Intent(MainActivity.this, DriverControlActivity.class);
                    intent.putExtra("BUS_ID", targetBusId);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Intent intent = new Intent(MainActivity.this, DriverControlActivity.class);
                intent.putExtra("BUS_ID", targetBusId);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkFirebaseForUpdate();
        listenBusStatuses(); // <-- সবচেয়ে গুরুত্বপূর্ণ: এখানে মেথডটি কল করা হয়েছে যাতে রিয়েল-টাইম কাজ করে
    }

    private void checkActiveTripsOnStartup() {
        String currentDeviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        DatabaseReference busesRef = FirebaseDatabase.getInstance("https://ec-driver-app-a619a-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("buses");

        busesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot busSnapshot : snapshot.getChildren()) {
                    String currentBusId = busSnapshot.getKey();
                    String status = busSnapshot.child("status").getValue(String.class);
                    String savedDeviceId = busSnapshot.child("activeDeviceId").getValue(String.class);

                    if ("ACTIVE".equals(status) && currentDeviceId.equals(savedDeviceId)) {
                        showActiveTripDialog(currentBusId);
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showActiveTripDialog(String activeBusId) {
        String busName = getBusDisplayName(activeBusId);

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("চলমান ট্রিপ পাওয়া গেছে")
                .setMessage(busName + " এর ট্রিপটি পূর্বে চালু অবস্থায় রয়েছে। আপনি কি এটি চালিয়ে যেতে চান নাকি শেষ করতে চান?")
                .setPositiveButton("চালিয়ে যান", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, DriverControlActivity.class);
                    intent.putExtra("BUS_ID", activeBusId);
                    startActivity(intent);
                })
                .setNegativeButton("যাত্রা শেষ করুন", (dialog, which) -> {
                    DatabaseReference busRef = FirebaseDatabase.getInstance("https://ec-driver-app-a619a-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("buses").child(activeBusId).child("status");
                    busRef.setValue("IDLE");
                    Toast.makeText(MainActivity.this, "যাত্রা শেষ করা হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    private void checkFirebaseForUpdate() {
        int currentVersionCode = 1;
        try {
            currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }

        final int localVersion = currentVersionCode;

        DatabaseReference databaseReference = FirebaseDatabase.getInstance("https://ec-driver-app-a619a-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("AppUpdate");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long serverVersionLong = snapshot.child("versionCode").getValue(Long.class);
                    String apkDownloadUrl = snapshot.child("apkUrl").getValue(String.class);

                    if (serverVersionLong != null && apkDownloadUrl != null) {
                        int serverVersionCode = serverVersionLong.intValue();

                        if (serverVersionCode > localVersion) {
                            showUpdateDialog(apkDownloadUrl);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Update check failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showUpdateDialog(String apkUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Update Available")
                .setMessage("A new version of the app is available. Would you like to update?")
                .setPositiveButton("Update", (dialog, which) -> {
                    new AppUpdater(MainActivity.this)
                            .setAppUrl(apkUrl)
                            .setFileNameFromUrl()
                            .setOnDownloadListener(new com.example.ecdriver.onDownloadListener() {
                                @Override
                                public void onBuffer(String connectingMsg) {
                                }

                                @Override
                                public void onProgress(int progress) {
                                    progressBar.setVisibility(View.VISIBLE);
                                    percentageText.setVisibility(View.VISIBLE);
                                    progresslayout.setVisibility(View.VISIBLE);
                                    progressBar.setProgress(progress);
                                    percentageText.setText(progress + "%");
                                }

                                @Override
                                public void onprogress(int progress) {

                                }

                                @Override
                                public void onComplete(File path) {
                                    progressBar.setVisibility(View.GONE);
                                    percentageText.setVisibility(View.GONE);
                                    progresslayout.setVisibility(View.GONE);
                                }

                                @Override
                                public void onError(String errorMsg) {
                                    Toast.makeText(MainActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                                }
                            }).start();
                })
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}