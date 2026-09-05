package com.example.ecdriver;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DriverControlActivity extends AppCompatActivity {

    private Button btnStart, btnEnd, btnIssue;
    private TextView tvBusTitle;
    private DatabaseReference busRef;
    private String busId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_control);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // MainActivity থেকে পাঠানো busId রিসিভ করা
        busId = getIntent().getStringExtra("BUS_ID");

        // UI ইনিশিয়ালাইজেশন
        btnStart = findViewById(R.id.btnStart);
        btnEnd = findViewById(R.id.btnEnd);
        btnIssue = findViewById(R.id.btnIssue);
        tvBusTitle = findViewById(R.id.tvBusTitle);

        // busId কে সুন্দর করে দেখানোর জন্য
        String displayBusName = busId;
        if (busId.equals("bus_01")) displayBusName = "বাস নাম্বার : ১";
        else if (busId.equals("bus_02")) displayBusName = "বাস নাম্বার : ২";
        else if (busId.equals("bus_03")) displayBusName = "বাস নাম্বার : ৩";
        else if (busId.equals("bus_04")) displayBusName = "বাস নাম্বার : ৪";
        else if (busId.equals("bus_05")) displayBusName = "বাস নাম্বার : ৫";

        tvBusTitle.setText(displayBusName);

        // Firebase Reference
        busRef = FirebaseDatabase.getInstance("https://ec-driver-app-a619a-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("buses").child(busId);

        // অ্যাক্টিভিটি চালুর সময় ফায়ারবেস থেকে স্ট্যাটাস চেক করে বাটন লক বা আনলক করা
        checkInitialBusStatus();

        // ১. ট্রিপ শুরু (Start) বাটন
        btnStart.setOnClickListener(v -> {
            busRef.child("status").setValue("ACTIVE");

            // বাটন কালো, ডিজেবল এবং লেখা পরিবর্তন করে "বাস চলছে..." করা
            setButtonActiveState(false, Color.BLACK, "বাস চলছে...");

            // ব্যাকগ্রাউন্ড সার্ভিস শুরু করার জন্য
            Intent serviceIntent = new Intent(DriverControlActivity.this, LocationForegroundService.class);
            serviceIntent.putExtra("BUS_ID", busId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Toast.makeText(DriverControlActivity.this, "ট্রিপ শুরু হয়েছে", Toast.LENGTH_SHORT).show();
        });

        // ২. ট্রিপ শেষ (End) বাটন
        btnEnd.setOnClickListener(v -> {
            busRef.child("status").setValue("STOP");
            busRef.child("activeDeviceId").setValue(null); // ডিভাইস বাইন্ডিং ক্লিয়ার করা

            // বাটন আগের অবস্থায় ফিরিয়ে আনা (ডিফল্ট কালার ও "যাত্রা শুরু" লেখা)
            setButtonActiveState(true, Color.parseColor("#006C32"), "যাত্রা শুরু");

            // সার্ভিস বন্ধ করা
            Intent serviceIntent = new Intent(DriverControlActivity.this, LocationForegroundService.class);
            stopService(serviceIntent);

            Toast.makeText(DriverControlActivity.this, "ট্রিপ শেষ হয়েছে", Toast.LENGTH_SHORT).show();
        });

        // ৩. সমস্যা রিপোর্ট (Issue) বাটন
        btnIssue.setOnClickListener(v -> showIssueDialog());
    }

    // ফায়ারবেস থেকে স্ট্যাটাস চেক করে স্টার্ট বাটনের স্টেট ঠিক করা
    private void checkInitialBusStatus() {
        busRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if ("ACTIVE".equals(status)) {
                    // যদি অলরেডি একটিভ থাকে, তবে বাটন কালো, ডিজেবল এবং লেখা "বাস চলছে..." থাকবে
                    setButtonActiveState(false, Color.BLACK, "বাস চলছে...");
                } else {
                    // না থাকলে বাটন নরমাল এবং লেখা "যাত্রা শুরু" থাকবে
                    setButtonActiveState(true, Color.parseColor("#006C32"), "যাত্রা শুরু");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // বাটনের শেপ, কালার এবং টেক্সট ডাইনামিক্যালি পরিবর্তনের হেল্পার মেথড
    private void setButtonActiveState(boolean isEnabled, int colorHex, String buttonText) {
        btnStart.setEnabled(isEnabled);
        btnStart.setText(buttonText);

        // GradientDrawable ব্যবহার করার ফলে শেপ ও কালার হুবহু ঠিক থাকবে
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(colorHex);

        float radiusInPx = getResources().getDisplayMetrics().density * 8; // কর্নার রেডিয়াস
        drawable.setCornerRadius(radiusInPx);

        btnStart.setBackground(drawable);
    }

    // সমস্যা লেখার ডায়ালগ বক্স
    private void showIssueDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DriverControlActivity.this);
        builder.setTitle("বাসে সমস্যার বিবরণ দিন");

        final EditText input = new EditText(DriverControlActivity.this);
        input.setHint("যেমন: ইঞ্জিনে সমস্যা / চাকা পাংচার");
        builder.setView(input);

        builder.setPositiveButton("সাবমিট", (dialog, which) -> {
            String issueText = input.getText().toString().trim();
            if (!issueText.isEmpty()) {
                busRef.child("status").setValue("ISSUE");
                busRef.child("issueMessage").setValue(issueText);

                // সমস্যা রিপোর্ট করলে ট্রিপ স্টপ ধরে বাটন নরমাল ও "যাত্রা শুরু" লেখায় ফিরিয়ে দেওয়া
                setButtonActiveState(true, Color.parseColor("#006C32"), "যাত্রা শুরু");

                Intent serviceIntent = new Intent(DriverControlActivity.this, LocationForegroundService.class);
                stopService(serviceIntent);

                Toast.makeText(DriverControlActivity.this, "সমস্যা রিপোর্ট করা হয়েছে এবং ট্রিপ বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("বাতিল", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}