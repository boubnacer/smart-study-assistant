package com.example.smartstudyassistant.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.smartstudyassistant.R;
import com.example.smartstudyassistant.api.OpenAIApiService;
import com.example.smartstudyassistant.api.OpenAIRequest;
import com.example.smartstudyassistant.api.OpenAIResponse;
import com.example.smartstudyassistant.database.DatabaseHelper;
import com.example.smartstudyassistant.models.Document;

import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AskActivity extends AppCompatActivity {
    private static final String OPENAI_API_KEY = "sk-proj-..."; // Your API key

    // UI Elements
    private Spinner spinnerDocuments;
    private EditText etQuestion;
    private Button btnAsk;
    private TextView tvAnswer;
    private ProgressBar progressBar;
    private TextView tvSelectedDocument;
    private TextView tvProcessing;

    // API Service
    private OpenAIApiService apiService;

    // Data
    private List<Document> documents = new ArrayList<>();
    private ArrayAdapter<Document> documentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ask);

        // Initialize UI
        spinnerDocuments = findViewById(R.id.spinnerDocuments);
        etQuestion = findViewById(R.id.etQuestion);
        btnAsk = findViewById(R.id.btnAsk);
        tvAnswer = findViewById(R.id.tvAnswer);
        progressBar = findViewById(R.id.progressBar);
        tvSelectedDocument = findViewById(R.id.tvSelectedDocument);
        tvProcessing = findViewById(R.id.tvProcessing);
        LinearLayout spinnerContainer = findViewById(R.id.spinnerContainer);

        // Initialize API service
        initOpenAIService();

        // Load documents
        loadDocuments();

        // Set button listener
        btnAsk.setOnClickListener(v -> {
            String question = etQuestion.getText().toString().trim();
            if (!question.isEmpty()) {
                getAnswerFromOpenAI(question);
            } else {
                Toast.makeText(this, "Enter a question", Toast.LENGTH_SHORT).show();
            }
        });

        // Set spinner container click listener
        spinnerContainer.setOnClickListener(v -> spinnerDocuments.performClick());

        // Set spinner item selection listener
        spinnerDocuments.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Document selectedDoc = (Document) parent.getItemAtPosition(position);
                tvSelectedDocument.setText(selectedDoc.getName());
                tvSelectedDocument.setTextColor(ContextCompat.getColor(AskActivity.this, R.color.text_primary));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tvSelectedDocument.setText("Select document");
                tvSelectedDocument.setTextColor(ContextCompat.getColor(AskActivity.this, R.color.text_secondary));
            }
        });
    }

    private void initOpenAIService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + OPENAI_API_KEY)
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(OpenAIApiService.class);
    }

    private void loadDocuments() {
        DatabaseHelper db = new DatabaseHelper(this);
        documents = db.getAllDocuments();

        if (documents.isEmpty()) {
            Toast.makeText(this, "Upload documents first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create custom adapter for spinner
        documentAdapter = new ArrayAdapter<Document>(this,
                android.R.layout.simple_spinner_item, documents) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(ContextCompat.getColor(AskActivity.this, R.color.text_primary));
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(ContextCompat.getColor(AskActivity.this, R.color.text_primary));
                view.setBackgroundColor(ContextCompat.getColor(AskActivity.this, R.color.card_background));
                view.setPadding(32, 32, 32, 32);
                return view;
            }
        };

        documentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDocuments.setAdapter(documentAdapter);
    }

    private void getAnswerFromOpenAI(String question) {
        Document selectedDoc = (Document) spinnerDocuments.getSelectedItem();
        if (selectedDoc == null) {
            Toast.makeText(this, "Please select a document", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading UI
        progressBar.setVisibility(View.VISIBLE);
        tvProcessing.setVisibility(View.VISIBLE);
        btnAsk.setEnabled(false);
        tvAnswer.setText("");

        // Process in background thread
        new Thread(() -> {
            try {
                DatabaseHelper db = new DatabaseHelper(AskActivity.this);
                List<String> chunks = db.getChunksByDocId(selectedDoc.getId());

                if (chunks.isEmpty()) {
                    runOnUiThread(() -> {
                        tvAnswer.setText("No text extracted from this document");
                        progressBar.setVisibility(View.GONE);
                        tvProcessing.setVisibility(View.GONE);
                        btnAsk.setEnabled(true);
                    });
                    return;
                }

                StringBuilder contextBuilder = new StringBuilder();
                for (String chunk : chunks) {
                    if (contextBuilder.length() + chunk.length() > 12000) break;
                    contextBuilder.append(chunk).append("\n\n");
                }
                String context = contextBuilder.toString();

                String prompt = "Answer the question based ONLY on the following context:\n\n" +
                        context + "\n\nQuestion: " + question + "\nAnswer:";

                List<OpenAIRequest.Message> messages = new ArrayList<>();
                messages.add(new OpenAIRequest.Message("user", prompt));

                OpenAIRequest request = new OpenAIRequest(
                        "gpt-4-1106-preview",
                        messages,
                        0.7
                );

                Call<OpenAIResponse> call = apiService.getCompletion(
                        "Bearer " + OPENAI_API_KEY,
                        request
                );

                Response<OpenAIResponse> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    final String answer = response.body().getAnswer();

                    runOnUiThread(() -> {
                        tvAnswer.setText(answer);
                        db.addQA(question, answer);
                        progressBar.setVisibility(View.GONE);
                        tvProcessing.setVisibility(View.GONE);
                        btnAsk.setEnabled(true);
                    });
                } else {
                    String error = response.errorBody() != null ?
                            response.errorBody().string() : "API error: " + response.code();

                    runOnUiThread(() -> {
                        tvAnswer.setText("Error: " + error);
                        progressBar.setVisibility(View.GONE);
                        tvProcessing.setVisibility(View.GONE);
                        btnAsk.setEnabled(true);
                    });
                }

            } catch (Exception e) {
                Log.e("AskActivity", "Error getting answer", e);
                runOnUiThread(() -> {
                    tvAnswer.setText("Error: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    tvProcessing.setVisibility(View.GONE);
                    btnAsk.setEnabled(true);
                });
            }
        }).start();
    }
}