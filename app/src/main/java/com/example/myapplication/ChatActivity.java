package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;
    private ChatRecyclerAdapter adapter;
    private DatabaseReference ref;
    private StorageReference storageReference;
    private ChildEventListener eventListener;
    private String reciverId = "";
    private String firebasePrivateNode = "";
    private Uri imagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        reciverId = getIntent().getStringExtra("id");
        firebasePrivateNode = getChatNode(FirebaseAuth.getInstance().getUid(), reciverId);

        adapter = new ChatRecyclerAdapter(FirebaseAuth.getInstance().getUid());
        ref = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        // Set up RecyclerView and other views
        binding.chatRecycler.setAdapter(adapter);

        // Fetch and display user information
        loadUserInfo(reciverId);

        binding.btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(binding.editMessage.getText().toString(), MessageType.TEXT);
            }
        });

        binding.btnSendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToGallery();
            }
        });

        getMessages();
    }

    private String getChatNode(String userId1, String userId2) {
        List<String> users = Arrays.asList(userId1, userId2);
        Collections.sort(users); // Sort user IDs to ensure a consistent chat node
        return users.get(0) + "_" + users.get(1); // Unique chat node ID
    }

    private void goToGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, 1);
    }

    private void sendMessage(String message, MessageType type) {
        ref.child("chats")
                .child(firebasePrivateNode)
                .push()
                .setValue(new ModelChat(message, type.name(), FirebaseAuth.getInstance().getUid()))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        binding.editMessage.setText("");
                    }
                });
    }

    private void getMessages() {
        eventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                ModelChat chat = dataSnapshot.getValue(ModelChat.class);
                adapter.addItem(chat);
                if (adapter.getItemCount() > 0) {
                    binding.chatRecycler.smoothScrollToPosition(adapter.getItemCount() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        ref.child("chats").child(firebasePrivateNode).addChildEventListener(eventListener);
    }

    private void uploadImage(Uri imageUri) {
        StorageReference reference = storageReference.child("chatImages")
                .child(firebasePrivateNode)
                .child(System.currentTimeMillis() + "_" + FirebaseAuth.getInstance().getUid());

        reference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                reference.getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                sendMessage(uri.toString(), MessageType.IMAGE);
                            }
                        });
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            imagePath = data.getData();
            uploadImage(imagePath);
        }
    }

    private void loadUserInfo(String userId) {
        ref.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ModelUserModel user = dataSnapshot.getValue(ModelUserModel.class);
                if (user != null) {
                    binding.textUserName.setText(user.getName());
                    binding.textUserStatus.setText(user.getPhone());

                    // تحميل صورة المستخدم من Firebase Storage
                    if (user.getImageURL() != null && !user.getImageURL().isEmpty()) {
                        Glide.with(ChatActivity.this)
                                .load(user.getImageURL())
                                .placeholder(R.drawable.ic_user) // Placeholder image
                                .into(binding.imageUser);
                    } else {
                        // إذا لم تكن صورة المستخدم متاحة، يمكنك وضع صورة افتراضية
                        binding.imageUser.setImageResource(R.drawable.ic_user);
                    }

                    // إذا كنت ترغب في عرض رقم الهاتف أيضًا
                    if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                        binding.textUserStatus.setText(user.getPhone());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // التعامل مع الأخطاء هنا
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventListener != null) {
            ref.child("chats").removeEventListener(eventListener);
        }
        eventListener = null;
        binding = null;
    }
}
