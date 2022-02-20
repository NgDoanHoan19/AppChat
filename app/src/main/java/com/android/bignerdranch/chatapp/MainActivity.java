package com.android.bignerdranch.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.bignerdranch.chatapp.Fragments.ChatsFragment;
import com.android.bignerdranch.chatapp.Fragments.ProfileFragment;
import com.android.bignerdranch.chatapp.Fragments.UsersFragment;
import com.android.bignerdranch.chatapp.Model.Chat;
import com.android.bignerdranch.chatapp.Model.User;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    // khai báo các biến điều khiển các Widget
    CircleImageView profile_image;
    TextView username;

    // khai báo các biến để thao tác với dữ liệu trên Firebase
    FirebaseUser firebaseUser;
    DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // thiết lập toolbar thành app bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        // liên kết biến với Widget
        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);

        // khởi tạo đối tượng, lấy dữ liệu người dùng hiện tại
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // khởi tạo đối tượng, tham chiếu đến trường dữ liệu "Users" theo ID người dùng
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        // hàm thực hiện khi có sự thay đổi dữ liệu
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // lấy dữ liệu từ dataSnapshot và sắp xếp thành dữ liệu của class User
                User user = dataSnapshot.getValue(User.class);

                // hiện tên người dùng từ dữ liệu trong user
                username.setText(user.getUsername());

                if (user.getImageURL().equals("default")) {
                    // cài ảnh đại diện mặc định
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else {
                    // Tải lên ảnh đại diện bằng thư viện Glide
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(profile_image);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // liên kết biến với Widget
        final TabLayout tabLayout = findViewById(R.id.tab_layout);
        final ViewPager viewPager = findViewById(R.id.view_pager);

        // khởi tạo đối tượng, lấy tham chiếu đến trường dữ liệu "Chats"
        reference = FirebaseDatabase.getInstance().getReference("Chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

                int unread = 0;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                    Chat chat = snapshot.getValue(Chat.class);

                    if (chat.getReceiver().equals(firebaseUser.getUid()) && !chat.isIsseen()){
                        unread++;
                    }
                }

                if (unread == 0){
                    String chats = getResources().getString(R.string.chats);
                    viewPagerAdapter.addFragment(new ChatsFragment(), chats);
                } else {
                    String chats = getResources().getString(R.string.chats);
                    viewPagerAdapter.addFragment(new ChatsFragment(), "("+unread+") "+chats);
                }
                String users = getResources().getString(R.string.users);
                String profile = getResources().getString(R.string.profile);
                viewPagerAdapter.addFragment(new UsersFragment(), users);
                viewPagerAdapter.addFragment(new ProfileFragment(), profile);

                viewPager.setAdapter(viewPagerAdapter);

                tabLayout.setupWithViewPager(viewPager);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // tạo đối tượng MenuInflater, inflate menu.xml vào menu
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){

            // thực hiện đăng xuất khỏi APP
            case  R.id.logout:
                FirebaseAuth.getInstance().signOut();
                // chuyển từ MainActivity về StartActivity
                startActivity(new Intent(MainActivity.this, StartActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
        }
        return false;
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {

        // khai báo biến chứa các fragment và title trong Tablayout
        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;

        // khởi tạo đối tượng
        ViewPagerAdapter(FragmentManager fm){
            super(fm);
            this.fragments = new ArrayList<>();
            this.titles = new ArrayList<>();
        }

        @Override
        public Fragment getItem(int position) {
            // lấy các Fragment theo vị trí
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            // lấy kích thước Fragment
            return fragments.size();
        }

        public void addFragment(Fragment fragment, String title){
            // thêm fragment và title vào mảng fragment
            fragments.add(fragment);
            titles.add(title);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            // lấy các title theo vị trí
            return titles.get(position);
        }
    }

    private void status(String status){

        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        // cập nhật status cho người dùng
        reference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }

}