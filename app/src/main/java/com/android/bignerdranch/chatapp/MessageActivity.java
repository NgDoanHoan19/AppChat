package com.android.bignerdranch.chatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.bignerdranch.chatapp.Adapter.MessageAdapter;
import com.android.bignerdranch.chatapp.Fragments.APIService;
import com.android.bignerdranch.chatapp.Model.Chat;
import com.android.bignerdranch.chatapp.Model.User;
import com.android.bignerdranch.chatapp.Notifications.Client;
import com.android.bignerdranch.chatapp.Notifications.Data;
import com.android.bignerdranch.chatapp.Notifications.MyResponse;
import com.android.bignerdranch.chatapp.Notifications.Sender;
import com.android.bignerdranch.chatapp.Notifications.Token;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MessageActivity extends AppCompatActivity {

    CircleImageView mProfileImage;
    TextView mUserName;

    FirebaseUser mFirebaseUser;
    DatabaseReference mReference;
    Intent mIntent;
    Toolbar mToolbar;

    ImageButton mSendButton;
    EditText mSendText;

    MessageAdapter messageAdapter;
    List<Chat> mChat;

    RecyclerView recyclerView;

    ValueEventListener seenListener;
    String userid;

    APIService apiService;
    boolean notify = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // thiết lập toolbar thành app bar
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MessageActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        // liên kết các biến với các Widget
        mProfileImage = findViewById(R.id.profile_image);
        mUserName = findViewById(R.id.username);
        mSendButton = (ImageButton) findViewById(R.id.btn_send);
        mSendText = (EditText) findViewById(R.id.text_send);

        // lấy Intent của Activity hiện tại
        mIntent = getIntent();

        // lấy nội dung extra
        userid = mIntent.getStringExtra("userid");

        // lấy dữ liệu người dùng hiện tại
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notify = true;

                // lấy nội dung tin nhắn
                String msg = mSendText.getText().toString();
                if(!msg.equals("")){
                    sendMessage(mFirebaseUser.getUid(), userid, msg);
                } else{
                    // báo lỗi
                    String youcantsendemptymessage = getResources().getString(R.string.youcantsendemptymessage);
                    Toast.makeText(MessageActivity.this, youcantsendemptymessage,Toast.LENGTH_SHORT).show();
                }
                // đưa về rỗng
                mSendText.setText("");
            }
        });

        mReference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                mUserName.setText(user.getUsername());
                if (user.getImageURL().equals("default")){
                    // set ảnh mặc định
                    mProfileImage.setImageResource(R.mipmap.ic_launcher);
                } else {
                    // tải ảnh lên theo url
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(mProfileImage);
                }

                readMessages(mFirebaseUser.getUid(), userid, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        seenMessage(userid);
    }

    private void seenMessage(final String userid){

        mReference = FirebaseDatabase.getInstance().getReference("Chats");

        seenListener =mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                    Chat chat = snapshot.getValue(Chat.class);

                    if (chat.getReceiver().equals(mFirebaseUser.getUid()) && chat.getSender().equals(userid)){

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("isseen", true);
                        // lấy tham chiếu đến vị trí dữ liệu, update thuộc tính "isseen" của đối tượng Chat
                        snapshot.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
    private void sendMessage(String sender, String receiver, String message){

        // lấy tham chiếu đến cơ sở dữ liệu
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        // tạo biến lưu trũ dữ liệu
        HashMap<String, Object> hashMap = new HashMap<>();

        // thực hiện lưu dữ liệu vào hashmap
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        hashMap.put("isseen", false);

        // đẩy dữ liệu vào trường dữ liệu "Chats" trong Firebase
        reference.child("Chats").push().setValue(hashMap);

        // khởi tạo đối tượng lấy tham chiếu đến vị trí trường con của "Chatlist"
        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(mFirebaseUser.getUid())
                .child(userid);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    // set giá trị "id" là id người nhận tin nhắn
                    chatRef.child("id").setValue(userid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // khởi tạo đối tượng lấy tham chiếu đến vị trí trường con của "Chatlist"
        final DatabaseReference chatRefReceiver = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(userid)
                .child(mFirebaseUser.getUid());

        // set giá trị "id" là id người dùng hiện tại
        chatRefReceiver.child("id").setValue(mFirebaseUser.getUid());

        final String msg = message;

        // lấy dữ liệu tham chiếu từ trường dữ liệu "Users" theo Id người dùng
        reference = FirebaseDatabase.getInstance().getReference("Users").child(mFirebaseUser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (notify) {
                    sendNotifiaction(receiver, user.getUsername(), msg);
                }
                notify = false;
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // gửi thông báo từ api Service
    private void sendNotifiaction(String receiver, final String username, final String message){

        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference("Tokens");

        // tạo truy vấn theo id người nhận
        Query query = tokens.orderByKey().equalTo(receiver);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                    Token token = snapshot.getValue(Token.class);
                    String newmessage = getResources().getString(R.string.newmessage);

                    Data data = new Data(mFirebaseUser.getUid(), R.mipmap.ic_launcher, username+": "+message, newmessage,
                            userid);

                    Sender sender = new Sender(data, token.getToken());

                    apiService.sendNotification(sender)
                            .enqueue(new Callback<MyResponse>() {
                                @Override
                                public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                    if (response.code() == 200){
                                        if (response.body().success != 1){
                                            String failed = getResources().getString(R.string.failed);
                                            Toast.makeText(MessageActivity.this, failed, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<MyResponse> call, Throwable t) {

                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    // đọc tin nhắn nhận được
    private void readMessages(String myid, String userid, String imageurl) {

        // khởi tạo mảng chứa các tin nhắn
        mChat = new ArrayList<>();

        // lấy dữ liệu tham chiếu từ trường dữ liệu "Chats"
        mReference = FirebaseDatabase.getInstance().getReference("Chats");

        mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mChat.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // lấy dữ liệu từ database, sắp xếp thành dữ liệu của lớp Chat
                    Chat chat = dataSnapshot.getValue(Chat.class);

                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                    chat.getReceiver().equals(userid) && chat.getSender().equals(myid)) {
                        mChat.add(chat);
                    }

                    // khởi tạo Adater
                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl);
                    // cài adapter lên recyclerview
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void currentUser(String userid){
        // tạo editor để sửa file
        SharedPreferences.Editor editor = getSharedPreferences("PREFS", MODE_PRIVATE).edit();
        // thêm id người dùng
        editor.putString("currentuser", userid);
        // xác nhận chỉnh sửa
        editor.apply();
    }

    private void status(String status){
        mReference = FirebaseDatabase.getInstance().getReference("Users").child(mFirebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        mReference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
        currentUser(userid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mReference.removeEventListener(seenListener);
        status("offline");
        currentUser("none");
    }
}