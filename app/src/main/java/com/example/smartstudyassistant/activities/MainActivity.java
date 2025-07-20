// MainActivity.java
package com.example.smartstudyassistant.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smartstudyassistant.R;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Test the database
//        testDB();


        Button btnUpload = findViewById(R.id.btnUpload);
        Button btnAsk = findViewById(R.id.btnAsk);
        Button btnHistory = findViewById(R.id.btnHistory);

        btnUpload.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, UploadActivity.class);
            startActivity(intent);
        });

        btnAsk.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AskActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Add to MainActivity's onCreate()
        Button btnDocList = findViewById(R.id.btnDocList);
        btnDocList.setOnClickListener(v -> {
            startActivity(new Intent(this, DocumentDetailActivity.class));
        });


    }

    // Database testing
//    private void testDB() {
//        DatabaseHelper db = new DatabaseHelper(this);
//
//
//        db.addDocument("Math Textbook", "/storage/emulated/0/documents/math.pdf");
//        db.addDocument("History Notes", "/storage/emulated/0/documents/history.docx");
//
//        db.addQA("What is photosynthesis?", "Process plants use to make food");
//        db.addQA("Capital of France?", "Paris");
//
//        Log.d("DB_TEST", "Documents: " + db.getAllDocuments().size());
//        Log.d("DB_TEST", "QA Items: " + db.getAllQA().size());
//    }
}