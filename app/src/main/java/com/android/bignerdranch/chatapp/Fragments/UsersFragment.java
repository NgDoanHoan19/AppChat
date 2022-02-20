package com.android.bignerdranch.chatapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.android.bignerdranch.chatapp.Adapter.UserAdapter;
import com.android.bignerdranch.chatapp.Model.User;
import com.android.bignerdranch.chatapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {

    // khai báo các biến điều khiển các Widget
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    // tạo mảng User
    private List<User> mUsers;
    EditText search_users;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_users, container, false);

        // liên kết biến với Widget,
        recyclerView = v.findViewById(R.id.recycler_view);
        // adapter thay đổi không ảnh hưởng đến kích thước RecyclerView
        recyclerView.setHasFixedSize(true);
        // cài kiểu hiển thị RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mUsers = new ArrayList<>();

        readUsers();

        search_users = v.findViewById(R.id.search_users);

        // gọi phương thức bên trong khi text thay đổi
        search_users.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                searchUsers(charSequence.toString().toLowerCase());
            }
            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        return v;
    }


    private void searchUsers(String s) {
        final FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        // tạo query mới được sắp xếp theo "search" bắt đầu từ s kết thúc tại s+"\uf8ff" (truy vấn được mọi chuỗi bắt đầu từ s)
        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("search")
                .startAt(s)
                .endAt(s+"\uf8ff");

        // tạo listener thực hiện khi có sự thay đổi dữ liệu, snapshot lưu trữ dữ liệu
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUsers.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    // lấy dữ liệu trong database, sắp xếp thành dữ liệu của user
                    User user = snapshot.getValue(User.class);
                    assert user != null;
                    assert fuser != null;
                    // nếu khác người dùng hiện tại thì thêm vào mUsers để hiển thị lên màn hình
                    if (!user.getId().equals(fuser.getUid())){
                        mUsers.add(user);
                    }
                }

                // khởi tạo Adater
                userAdapter = new UserAdapter(getContext(), mUsers, false);
                // cài adapter vào recyclerview
                recyclerView.setAdapter(userAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    private void readUsers() {

        // khởi tạo đối tượng, lấy dữ liệu người dùng hiện tại
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        // khởi tạo đối tượng, lấy dữ liệu tham chiếu từ trường dữ liệu "Users"
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");

        // tạo listener thực hiện khi có sự thay đổi dữ liệu, snapshot lưu trữ dữ liệu
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (search_users.getText().toString().equals("")) {

                    mUsers.clear();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                        //
                        User user = snapshot.getValue(User.class);
                        if (!user.getId().equals(firebaseUser.getUid())) {
                            mUsers.add(user);
                        }

                    }

                    userAdapter = new UserAdapter(getContext(), mUsers, false);
                    recyclerView.setAdapter(userAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}