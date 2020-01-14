package com.example.photobook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddActivity extends AppCompatActivity {
    Bitmap selectImage;
    ImageView imageView;
    EditText photoNameText;
    EditText DetailText;
    EditText DateText;
    Button button;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        imageView=findViewById(R.id.imageView3);
        photoNameText=findViewById(R.id.photoNameText);
        DetailText=findViewById(R.id.DetailText);
        DateText=findViewById(R.id.DateText);
        button=findViewById(R.id.button);
        database=this.openOrCreateDatabase("Photos",MODE_PRIVATE,null);
        Intent intent=getIntent();
        String info=intent.getStringExtra("info");
        if(info.matches("new")){
            photoNameText.setText("");
            DetailText.setText("");
            DateText.setText("");
            button.setVisibility(View.VISIBLE);
            Bitmap selectImage=BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.selectimage);
            imageView.setImageBitmap(selectImage);

        }
        else{
            int photoId=intent.getIntExtra("photoID",1);
            button.setVisibility(View.INVISIBLE);
            try{
                Cursor cursor=database.rawQuery("SELECT*FROM Photos WHERE id=?",new String[]{String.valueOf(photoId)});
                int photoNameIx=cursor.getColumnIndex("photoName");
                int detailIx=cursor.getColumnIndex("detail");
                int dateIx=cursor.getColumnIndex("date");
                int imageIx=cursor.getColumnIndex("image");
                while(cursor.moveToNext()){
                    photoNameText.setText(cursor.getString(photoNameIx));
                    DetailText.setText(cursor.getString(detailIx));
                    DateText.setText(cursor.getString(dateIx));
                    byte[] bytes=cursor.getBlob(imageIx);
                    Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);

                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void selectImage(View view)
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
        {
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
        else{
            Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery,2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==1) {
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery,2);

            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==2&&resultCode==RESULT_OK&&data!=null){
           Uri imageData =data.getData();
            try {
                if(Build.VERSION.SDK_INT>28)
                {
                    ImageDecoder.Source source=ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectImage=ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectImage);
                }
                else
                {   selectImage=MediaStore.Images.Media.getBitmap(getContentResolver(),imageData);
                    imageView.setImageBitmap(selectImage);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    public void save(View view){
        String photoName=photoNameText.getText().toString();
        String date=DateText.getText().toString();
        String details=DetailText.getText().toString();
        Bitmap smallimage = makeSmallerImage(selectImage,300);
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallimage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray=outputStream.toByteArray();
        try{
         database=this.openOrCreateDatabase("Photos",MODE_PRIVATE,null);
        database.execSQL("CREATE TABLE IF NOT EXISTS photos(id INTEGER PRIMARY KEY,photoName VARCHAR,detail VARCHAR,date VARCHAR,image BLOB)");
        String sqlString="INSERT INTO photos(photoName,detail,date,image)VALUES(?,?,?,?)";
        SQLiteStatement sqLiteStatement=database.compileStatement(sqlString);
        sqLiteStatement.bindString(1,photoName);
        sqLiteStatement.bindString(2,details);
        sqLiteStatement.bindString(3,date);
        sqLiteStatement.bindBlob(4,byteArray );
        sqLiteStatement.execute();}
        catch (Exception e) {
        }
        //finish();
        Intent intent=new Intent(AddActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public Bitmap makeSmallerImage(Bitmap image,int maximumSize){
        int widht=image.getWidth();
        int height=image.getHeight();
        float bitmapRatio=(float)widht/(float)height;
        if(bitmapRatio>1){
            widht=maximumSize;
            height=(int) (bitmapRatio/widht);

        }else{
            height=maximumSize;
            widht=(int)(height*bitmapRatio);

        }
        return Bitmap.createScaledBitmap(image,widht,height,true);
    }
}


