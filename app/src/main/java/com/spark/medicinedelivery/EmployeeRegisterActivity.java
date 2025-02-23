package com.spark.medicinedelivery;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
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
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class EmployeeRegisterActivity extends AppCompatActivity {

    private EditText emp_id_inp, emp_phone_no_inp;
    private Button employee_acc_finish_btn, upload_img_btn;
    private CircleImageView EmployeeImage;

    FirebaseAuth mAuth;
    private ProgressDialog mProgressDialog;
    private FirebaseUser mCurrentUser;
    private StorageReference mImageStorage;
    private DatabaseReference mUserDatabase;
    private ProgressDialog UploadProgress, mRegProgress;

    private final static int GALLERY_PICK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_register);

        emp_id_inp = (EditText)findViewById(R.id.emp_id_inp);
        emp_phone_no_inp = (EditText)findViewById(R.id.emp_phone_inp);
        employee_acc_finish_btn = (Button)findViewById(R.id.employee_finish_acc_btn);
        upload_img_btn = (Button)findViewById(R.id.employee_image_upload_btn);
        EmployeeImage = (CircleImageView)findViewById(R.id.buyer_mainactivity_dp_imageview);

        mAuth = FirebaseAuth.getInstance();
        mImageStorage = FirebaseStorage.getInstance().getReference();
        mRegProgress = new ProgressDialog(this);
        UploadProgress = new ProgressDialog(this);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        String UID = mCurrentUser.getUid();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Employee").child(UID);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final String image = dataSnapshot.child("image").getValue().toString();

                if(!image.equalsIgnoreCase("default"))
                {
                    Picasso.get().load(image).placeholder(R.drawable.employee_avatar).into(EmployeeImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        upload_img_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);
            }
        });

        employee_acc_finish_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phone = emp_phone_no_inp.getText().toString();
                String employee_id = emp_id_inp.getText().toString();

                if (employee_id.trim().equalsIgnoreCase(""))
                    emp_id_inp.setError("Field cannot be empty");
                else if (phone.trim().equalsIgnoreCase(""))
                    emp_phone_no_inp.setError("Field Cannot be blank");
                else
                {
                    Map more_info = new HashMap<String, String>();
                    more_info.put("Phone", phone);
                    more_info.put("Employee_ID", employee_id);

                    mRegProgress.setTitle("Updating details");
                    mRegProgress.setMessage("Please wait while we add your details to our database");
                    mRegProgress.setCanceledOnTouchOutside(false);
                    mRegProgress.show();

                    mUserDatabase.updateChildren(more_info).addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if(task.isSuccessful())
                            {
                                mRegProgress.dismiss();
                                Intent EmployeeMainIntent = new Intent(EmployeeRegisterActivity.this, EmployeeMainActivity.class);
                                EmployeeMainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(EmployeeMainIntent);
                                finish();
                            }
                            else
                                Toast.makeText(EmployeeRegisterActivity.this, "Network error. Try again", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_PICK && resultCode == RESULT_OK)
        {
            Uri imageUri = data.getData();
            CropImage.activity(imageUri).setAspectRatio(1, 1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK)
            {

                UploadProgress = new ProgressDialog(EmployeeRegisterActivity.this);
                UploadProgress.setTitle("Uploading Image...");
                UploadProgress.setMessage("Please wait while we upload and process the image");
                UploadProgress.setCanceledOnTouchOutside(false);
                UploadProgress.show();

                Uri resultUri = result.getUri();

                final File thumb_filePath = new File(resultUri.getPath());
                String current_user_id = mCurrentUser.getUid();

                Bitmap thumb_bitmap = null;

                try {
                    thumb_bitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(100)
                            .compressToBitmap(thumb_filePath);


                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                final byte[] thumb_byte = baos.toByteArray();

                StorageReference filepath = mImageStorage.child("profile_images").child(current_user_id + ".jpg");
                final StorageReference thumb_filepath = mImageStorage.child("profile_images").child("thumbs").child(current_user_id +".jpg");

                filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful())
                        {
                            final String download_url = task.getResult().getDownloadUrl().toString();
                            UploadTask uploadTask = thumb_filepath.putBytes(thumb_byte);
                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {

                                    String thumb_downloadURL = thumb_task.getResult().getDownloadUrl().toString();
                                    if(thumb_task.isSuccessful())
                                    {
                                        Map update_hashMap = new HashMap<>();
                                        update_hashMap.put("image", download_url);
                                        update_hashMap.put("thumb_image", thumb_downloadURL);

                                        mUserDatabase.updateChildren(update_hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful())
                                                {
                                                    UploadProgress.dismiss();
                                                }
                                            }
                                        });

                                    }
                                    else
                                    {
                                        Toast.makeText(EmployeeRegisterActivity.this, "Error while uploading thumbnail", Toast.LENGTH_SHORT).show();
                                        UploadProgress.dismiss();
                                    }
                                }
                            });


                        }
                        else
                        {
                            Toast.makeText(EmployeeRegisterActivity.this, "Error encountered while updating profile picture", Toast.LENGTH_SHORT).show();
                            mProgressDialog.dismiss();
                        }


                    }
                });
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Exception error = result.getError();
            }
        }
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(10);
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }
}
