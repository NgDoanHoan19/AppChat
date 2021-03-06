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

        // li??n k???t widget
        image_profile = view.findViewById(R.id.profile_image);
        username = view.findViewById(R.id.username);

        // kh???i t???o ?????i t?????ng, l???y d??? li???u tham chi???u t??? tr?????ng "uploads" trong Storage
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        // l???y d??? li???u
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isAdded()) {
                    User user = dataSnapshot.getValue(User.class);
                    username.setText(user.getUsername());
                    if (user.getImageURL().equals("default")) {
                        // t???i ???nh m???c ?????nh
                        image_profile.setImageResource(R.mipmap.ic_launcher);
                    } else {
                        // t???i ???nh t??? link
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

    // m??? th?? m???c ????? ch???n ???nh
    private void openImage() {
        Intent intent = new Intent();
        // c??i ?????t ki???u cho ???nh
        intent.setType("image/*");
        // c??i ?????t c??ch th???c hi???n l?? l???y m???t d??? li???u c??? th???
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // g???i Activity con v?? y??u c???u Activity con tr??? v??? m???t k???t qu???
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    // l???y ph???n m??? r???ng c???a file (??u??i ?????nh d???ng file)
    private String getFileExtension(Uri uri){

        // l???y context, kh???i t???o ?????i t?????ng
        ContentResolver contentResolver = getContext().getContentResolver();
        // l???y ?????i t?????ng Singleton
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        // contentResolver l???y ki???u MIME t??? URI sau ???? mimeTypeMap tr??? v??? ph???n m??? r???ng cho ki???u MIME n??y.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage(){

        // kh???i t???o ?????i t?????ng
        final ProgressDialog pd = new ProgressDialog(getContext());
        String uploading = getResources().getString(R.string.uploading);

        // hi???n th??? text khi ??ang trong qu?? tr??nh load ???nh
        pd.setMessage(uploading);
        pd.show();

        if (imageUri != null){
            // t???o file ???nh, g??n d??? li???u cho fileReference
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    +"."+getFileExtension(imageUri));

            // t???i ???nh l??n StorageReference
            uploadTask = fileReference.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw  task.getException();
                    }
                    // truy xu???t ?????n url ???nh
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
    // h??m x??? l?? k???t qu??? tr??? v??? t??? Activity con sau khi g???i startActivityForResult
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null){
            // l???y link ???nh
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()){
                Toast.makeText(getContext(), R.string.uploadinprogress, Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }

}