package com.android.bignerdranch.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    // khai báo các biến điều khiển các Widget
    MaterialEditText mUserName, mEmail, mPassword;
    Button mRegisterBtn;
    Toolbar mToolbar;

    // khai báo các biến để thao tác với dữ liệu trên Firebase
    FirebaseAuth mAuth;
    DatabaseReference mReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // thiết lập Toolbar thành App bar và hiển thị tiêu đề của Activity hiện tại
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.register);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Liên kết các Widget với các biến
        mUserName = (MaterialEditText) findViewById(R.id.username);
        mPassword = (MaterialEditText) findViewById(R.id.password);
        mEmail = (MaterialEditText) findViewById(R.id.email);
        mRegisterBtn = (Button) findViewById(R.id.btn_register);

        // lấy đối tượng cho việc đăng nhập, đăng kí
        mAuth = FirebaseAuth.getInstance();

        mRegisterBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                // lấy các dữ liệu được người dùng nhập
                String txt_username = mUserName.getText().toString();
                String txt_email = mEmail.getText().toString();
                String txt_password = mPassword.getText().toString();

                // thông báo các lỗi lên màn hình hoặc thực hiện đăng ký
                if (TextUtils.isEmpty(txt_username) || TextUtils.isEmpty(txt_password) || TextUtils.isEmpty(txt_email)){
                        Toast.makeText(RegisterActivity.this, R.string.allfieldarerequired, Toast.LENGTH_SHORT).show();
                }
                else if (txt_password.length() < 6){
                    Toast.makeText(RegisterActivity.this, R.string.passwordmustbeatleast6character, Toast.LENGTH_SHORT).show();
                }
                else {
                    register(txt_username, txt_email, txt_password);
                }
            }
        });
    }

    // hàm thực hiện đăng ký tài khoản
    private void register(final String UserName, String Email, String Password){

        // tạo tài khoản email và mật khẩu trên FireBase như tên gọi
        mAuth.createUserWithEmailAndPassword(Email, Password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        String username = UserName;
                        if (task.isSuccessful()){

                            // lấy dữ liệu người dùng hiện tại
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            assert firebaseUser != null;

                            // tạo Id cho người dùng hiện tại
                            String userId = firebaseUser.getUid();

                            // tạo đối tượng cho việc truy cập cơ sở dữ liệu, đối tượng này sẽ tham chiếu đến trường dữ liệu tên "Users" theo Id người dùng
                            mReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

                            // lưu các thông tin của người dùng hiện tại vào biến hashMap, hashMap là kiểu dữ liệu có dạng Key-Value
                            HashMap<String, String> hashMap = new HashMap<>();
                            hashMap.put("id", userId);
                            hashMap.put("username", username);
                            hashMap.put("imageURL", "default");
                            hashMap.put("status","offline");
                            hashMap.put("search", username.toLowerCase());

                            // lưu thông tin người dùng vào trường dữ liệu tên "Users"
                            mReference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){

                                        // chuyển từ RegisterActivity sang MainActivity bằng Intent
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                    else {
                                        Toast.makeText(RegisterActivity.this, "Wrong Email or Password", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    }
                });
    }
}