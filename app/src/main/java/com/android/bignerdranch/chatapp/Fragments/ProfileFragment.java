package com.android.bignerdranch.chatapp.Fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import com.android.bignerdranch.chatapp.Model.User;
import com.android.bignerdranch.chatapp.R;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    CircleImageView image_profile;
    TextView username;

    DatabaseReference reference;
    FirebaseUser fuser;

    StorageReference storageReference;

    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // liên kết widget
        image_profile = view.findViewById(R.id.profile_image);
        username = view.findViewById(R.id.username);

        // khởi tạo đối tượng, lấy dữ liệu tham chiếu từ trường "uploads" trong Storage
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        // lấy dữ liệu
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isAdded()) {
                    User user = dataSnapshot.getValue(User.class);
                    username.setText(user.getUsername());
                    if (user.getImageURL().equals("default")) {
                        // tải ảnh mặc định
                        image_profile.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        // tải ảnh từ link
                        Glide.with(getContext()).load(user.getImageURL()).into(image_profile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        image_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });

        return view;
    }

    // mở thư mục để chọn ảnh
    private void openImage() {
        Intent intent = new Intent();
        // cài đặt kiểu cho ảnh
        intent.setType("image/*");
        // cài đặt cách thực hiện là lấy một dữ liệu cụ thể
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // gọi Activity con và yêu cầu Activity con trả về một kết quả
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    // lấy phần mở rộng của file (đuôi định dạng file)
    private String getFileExtension(Uri uri){

        // lấy context, khởi tạo đối tượng
        ContentResolver contentResolver = getContext().getContentResolver();
        // lấy đối tượng Singleton
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        // contentResolver lấy kiểu MIME từ URI sau đó mimeTypeMap trả về phần mở rộng cho kiểu MIME này.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage(){

        // khởi tạo đối tượng
        final ProgressDialog pd = new ProgressDialog(getContext());
        String uploading = getResources().getString(R.string.uploading);

        // hiện thị text khi đang trong quá trình load ảnh
        pd.setMessage(uploading);
        pd.show();

        if (imageUri != null){
            // tạo file ảnh, gán dữ liệu cho fileReference
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    +"."+getFileExtension(imageUri));

            // tải ảnh lên StorageReference
            uploadTask = fileReference.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw  task.getException();
                    }
                    // truy xuất đến url ảnh
                    return  fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()){

                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();

                        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("imageURL", ""+mUri);
                        reference.updateChildren(map);

                        pd.dismiss();
                    } else {
                        Toast.makeText(getContext(), R.string.failed, Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        } else {
            Toast.makeText(getContext(), R.string.noimageselected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    // hàm xử lý kết quả trả về từ Activity con sau khi gọi startActivityForResult
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null){
            // lấy link ảnh
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()){
                Toast.makeText(getContext(), R.string.uploadinprogress, Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }

}