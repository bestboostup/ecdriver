package com.example.ecdriver;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
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
    private TextView percentageText;

    Button busnumber1, busnumber2, busnumber3, busnumber4, busnumber5;
    private long downloadId;

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

        //=========== Button Introduce ======================

        busnumber1 = findViewById(R.id.busnumber1);
        busnumber2 = findViewById(R.id.busnumber2);
        busnumber3 = findViewById(R.id.busnumber3);
        busnumber4 = findViewById(R.id.busnumber4);
        busnumber5 = findViewById(R.id.busnumber5);

        //=========== Button Work =========================

        busnumber1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent busoneintent = new Intent(MainActivity.this, BusNumberOne.class);
                startActivity(busoneintent);

            }
        });


        busnumber2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent busoneintent = new Intent(MainActivity.this, BusNumberTwo.class);
                startActivity(busoneintent);

            }
        });


        busnumber3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent busoneintent = new Intent(MainActivity.this, BusNumberThree.class);
                startActivity(busoneintent);

            }
        });


        busnumber4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent busoneintent = new Intent(MainActivity.this, BusNumberFour.class);
                startActivity(busoneintent);

            }
        });

        busnumber5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent busoneintent = new Intent(MainActivity.this, BusNumberFive.class);
                startActivity(busoneintent);

            }
        });


    } //----------- On Create End ----------------------

















    //--------- In App Update Code ---------------------


    @Override
    protected void onResume() {
        super.onResume();
        // অ্যাপ চালু বা রিজিউম হলেই ফায়ারবেস থেকে আপডেট চেক করবে
        checkFirebaseForUpdate();
    }

    // --- ফায়ারবেস থেকে ভার্সন ও লিংক চেক করার মেথড ---
    private void checkFirebaseForUpdate() {
        int currentVersionCode = 1;
        try {
            currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }

        final int localVersion = currentVersionCode;

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("AppUpdate");
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long serverVersionLong = snapshot.child("versionCode").getValue(Long.class);
                    String apkDownloadUrl = snapshot.child("apkUrl").getValue(String.class);

                    if (serverVersionLong != null && apkDownloadUrl != null) {
                        int serverVersionCode = serverVersionLong.intValue();

                        // ফায়ারবেসের ভার্সন লোকাল ভার্সন থেকে বেশি হলে আপডেট ডায়লগ দেখাবে
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

    // --- আপডেট ডায়লগ ----
    private void showUpdateDialog(String apkUrl) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Available")
                .setMessage("A new version of the app is available. Would you like to update?")
                .setPositiveButton("Update", (dialog, which) -> {
                    // কাস্টম AppUpdater ব্যবহার করে ফায়ারবেস থেকে পাওয়া লিংকের মাধ্যমে ডাউনলোড শুরু হবে
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
                                    progressBar.setProgress(progress);
                                    percentageText.setText(progress + "%");
                                }

                                @Override
                                public void onComplete(File path) {
                                    progressBar.setVisibility(View.GONE);
                                    percentageText.setVisibility(View.GONE);
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


