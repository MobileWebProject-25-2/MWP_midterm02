package com.example.imageviewdemo;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.imageviewdemo.databinding.ActivityImageDetailBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageDetailActivity extends AppCompatActivity {

    private ActivityImageDetailBinding binding;
    private Bitmap currentBitmap;
    private static final int REQUEST_WRITE_STORAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Intent에서 데이터 가져오기
        String title = getIntent().getStringExtra("title");
        String text = getIntent().getStringExtra("text");
        String imageUrl = getIntent().getStringExtra("imageUrl");

        // 제목 설정
        if (title != null && !title.isEmpty()) {
            binding.detailTitle.setText(title);
        } else {
            binding.detailTitle.setText("제목 없음");
        }

        // 본문 내용 설정
        if (text != null && !text.isEmpty()) {
            binding.detailText.setText(text);
        } else {
            binding.detailText.setText("내용이 없습니다.");
        }

        // 이미지 로드
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            new LoadImageTask().execute(imageUrl);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "이미지를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        // 닫기 버튼
        binding.closeButton.setOnClickListener(v -> finish());

        // 저장 버튼
        binding.saveButton.setOnClickListener(v -> {
            if (currentBitmap != null) {
                saveImageToGallery();
            } else {
                Toast.makeText(ImageDetailActivity.this, "이미지를 먼저 로드해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 이미지 클릭 시 확대/축소 (선택적)
        binding.detailImage.setOnClickListener(v -> {
            // 이미지 클릭 시 전체화면으로 볼 수 있는 기능 추가 가능
            Toast.makeText(ImageDetailActivity.this, "이미지를 확대하여 보고 있습니다", Toast.LENGTH_SHORT).show();
        });
    }

    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected void onPreExecute() {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.detailImage.setVisibility(View.GONE);
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                String imageUrl = urls[0];
                if (imageUrl == null || imageUrl.isEmpty() || imageUrl.equals("null")) {
                    return null;
                }
                
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    connection.disconnect();
                    return null;
                }
                
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                connection.disconnect();
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            binding.progressBar.setVisibility(View.GONE);
            if (result != null) {
                currentBitmap = result;
                binding.detailImage.setVisibility(View.VISIBLE);
                binding.detailImage.setImageBitmap(result);
            } else {
                Toast.makeText(ImageDetailActivity.this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+)
            saveImageToGalleryModern();
        } else {
            // Android 9 이하
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE);
            } else {
                saveImageToGalleryLegacy();
            }
        }
    }

    private void saveImageToGalleryModern() {
        try {
            String title = binding.detailTitle.getText().toString();
            if (title == null || title.isEmpty()) {
                title = "PhotoView_" + System.currentTimeMillis();
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, title + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                    Toast.makeText(this, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageToGalleryLegacy() {
        try {
            String title = binding.detailTitle.getText().toString();
            if (title == null || title.isEmpty()) {
                title = "PhotoView_" + System.currentTimeMillis();
            }

            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!picturesDir.exists()) {
                picturesDir.mkdirs();
            }

            String fileName = title + "_" + System.currentTimeMillis() + ".jpg";
            File imageFile = new File(picturesDir, fileName);

            FileOutputStream fos = new FileOutputStream(imageFile);
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            // 갤러리에 스캔 요청
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
            values.put(MediaStore.Images.Media.TITLE, title);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Toast.makeText(this, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveImageToGalleryLegacy();
            } else {
                Toast.makeText(this, "저장 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

