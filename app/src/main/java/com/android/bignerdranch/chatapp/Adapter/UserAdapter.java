package com.android.bignerdranch.chatapp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.bignerdranch.chatapp.MessageActivity;
import com.android.bignerdranch.chatapp.Model.Chat;
import com.android.bignerdranch.chatapp.Model.User;
import com.android.bignerdranch.chatapp.R;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    // khai báo các biến
    private Context mContext;
    private List<User> mUsers;
    private boolean ischat;
    String theLastMessage;

    // hàm khởi tạo
    public UserAdapter(Context mContext, List<User> mUsers, boolean isChat) {
        this.mContext = mContext;
        this.mUsers = mUsers;
        this.ischat = isChat;
    }

    @NonNull
    @Override
    // hàm tạo ViewHolder, Adapter dùng để tạo ra ViewHolder
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.user_item, parent, false);
        return new UserAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        // lấy dữ liệu người dùng theo vị trí
        User user = mUsers.get(position);
        // hiển thị tên người dùng
        holder.username.setText(user.getUsername());

        if (user.getImageURL().equals("default")){
            // cài ảnh đại diện mặc định
            holder.profile_image.setImageResource(R.mipmap.ic_launcher);
        } else {
            // tải ảnh đại diện từ link
            Glide.with(mContext).load(user.getImageURL()).into(holder.profile_image);
        }

        if (ischat){
            lastMessage(user.getId(), holder.last_msg);
        } else {
            holder.last_msg.setVisibility(View.GONE);
        }


        if (ischat){
            if (user.getStatus().equals("online")){
                // bật hiển thị Widget status
                holder.img_on.setVisibility(View.VISIBLE);
                // tắt hiển thị widget status
                holder.img_off.setVisibility(View.GONE);
            } else {
                holder.img_on.setVisibility(View.GONE);
                holder.img_off.setVisibility(View.VISIBLE);
            }
        } else {
            holder.img_on.setVisibility(View.GONE);
            holder.img_off.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MessageActivity.class);

                // thêm extra vào intent
                intent.putExtra("userid", user.getId());

                // hiển thị lỗi
                System.err.println("userid: " + user.getId());

                // chuyển đến MessageActivity
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        // trả về kích thước mảng User
        return mUsers.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // khai báo các biến điều khiển các Widget
        public TextView username;
        public ImageView profile_image;
        private ImageView img_on;
        private ImageView img_off;
        private TextView last_msg;

        // ViewHolder thông qua itemView để điều khiển View
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // liên kết các biến với Widget
            username = itemView.findViewById(R.id.username);
            profile_image = itemView.findViewById(R.id.profile_image);
            img_on = itemView.findViewById(R.id.img_on);
            img_off = itemView.findViewById(R.id.img_off);
            last_msg = itemView.findViewById(R.id.last_msg);
        }
    }

    private void lastMessage(final String userid, final TextView last_msg){

        theLastMessage = "default";
        final FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");

        // tạo listener thực hiện khi có sự thay đổi dữ liệu, snapshot lưu trữ dữ liệu
        reference.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){

                    Chat chat = snapshot.getValue(Chat.class);
                    if (firebaseUser != null && chat != null) {

                        if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userid) ||
                                chat.getReceiver().equals(userid) && chat.getSender().equals(firebaseUser.getUid())) {
                            theLastMessage = chat.getMessage();
                        }
                    }
                }

                switch (theLastMessage){
                    case  "default":

                        last_msg.setText("No Message");
                        break;
                    default:
                        last_msg.setText(theLastMessage);
                        break;
                }

                theLastMessage = "default";
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }
}
