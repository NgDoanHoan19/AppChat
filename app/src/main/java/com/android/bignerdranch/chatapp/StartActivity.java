package com.android.bignerdranch.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StartActivity extends AppCompatActivity {

    // khai báo các biến điều khiển Widget
    Button mLogin;
    Button mRegister;

    // khai báo biến thao tác dữ liệu người dùng
    FirebaseUser mFirebaseUser;

    @Override
    protected void onStart() {
        super.onStart();

        // khởi tạo đối tượng, lấy dữ liệu người dùng hiện tại
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // trường hợp người dùng chưa đăng xuất thì chuyển từ StartActivity vào MainActivity ngay khi bắt đầu App
        if (mFirebaseUser != null){
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // liên kết biến với Widget
        mLogin = (Button) findViewById(R.id.login);
        mRegister = (Button) findViewById(R.id.register);

        // chuyển từ StartActivity sang LoginActivity bằng Intent
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartActivity.this, LoginActivity.class));
            }
        });

        // chuyển từ StartActivity sang RegisterActivity bằng Intent
        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(StartActivity.this, RegisterActivity.class));
            }
        });

    }
}