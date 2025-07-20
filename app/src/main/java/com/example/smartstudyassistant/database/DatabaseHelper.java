package com.example.smartstudyassistant.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.smartstudyassistant.models.Document;
import com.example.smartstudyassistant.models.QAItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "StudyAssistant.db";
    private static final int DATABASE_VERSION = 2;

    // Documents table
    private static final String TABLE_DOCUMENTS = "documents";
    private static final String COL_DOC_ID = "id";
    private static final String COL_DOC_NAME = "name";
    private static final String COL_DOC_PATH = "path";

    // Q&A table
    private static final String TABLE_QA = "qa_history";
    private static final String COL_QA_ID = "id";
    private static final String COL_QUESTION = "question";
    private static final String COL_ANSWER = "answer";

    // Chunks table
    private static final String TABLE_CHUNKS = "chunks";
    private static final String COL_CHUNK_ID = "id";
    private static final String COL_DOC_ID_FK = "doc_id";
    private static final String COL_CHUNK_TEXT = "chunk_text";

    // delete all documents
    private Context context;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create documents table
        String createDocTable = "CREATE TABLE " + TABLE_DOCUMENTS + "("
                + COL_DOC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_DOC_NAME + " TEXT,"
                + COL_DOC_PATH + " TEXT)";
        db.execSQL(createDocTable);

        // Create Q&A table
        String createQATable = "CREATE TABLE " + TABLE_QA + "("
                + COL_QA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_QUESTION + " TEXT,"
                + COL_ANSWER + " TEXT)";
        db.execSQL(createQATable);

        // Create chunks table
        String createChunksTable = "CREATE TABLE " + TABLE_CHUNKS + "("
                + COL_CHUNK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_DOC_ID_FK + " INTEGER,"
                + COL_CHUNK_TEXT + " TEXT)";
        db.execSQL(createChunksTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DOCUMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHUNKS);
        onCreate(db);
    }

    // Document CRUD
    public long addDocument(String name, String path) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DOC_NAME, name);
        values.put(COL_DOC_PATH, path);
        return db.insert(TABLE_DOCUMENTS, null, values);
    }


    // Q&A CRUD
    public long addQA(String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_QUESTION, question);
        values.put(COL_ANSWER, answer);
        return db.insert(TABLE_QA, null, values);
    }

    public List<QAItem> getAllQA() {
        List<QAItem> qaList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_QA;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                QAItem qa = new QAItem();
                qa.setId(cursor.getInt(0));
                qa.setQuestion(cursor.getString(1));
                qa.setAnswer(cursor.getString(2));
                qaList.add(qa);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return qaList;
    }

    // Chunks CRUD
    public long addChunk(int docId, String chunkText) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_DOC_ID_FK, docId);
        values.put(COL_CHUNK_TEXT, chunkText);
        return db.insert(TABLE_CHUNKS, null, values);
    }

    public List<String> getChunksByDocId(int docId) {
        List<String> chunks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CHUNKS,
                new String[]{COL_CHUNK_TEXT},
                COL_DOC_ID_FK + "=?",
                new String[]{String.valueOf(docId)},
                null, null, null);

        if (cursor.moveToFirst()) {
            do {
                chunks.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return chunks;
    }

    // Clean up orphaned documents, to make sure we don't have enteries in the database
    public void cleanOrphanedDocuments() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            List<Integer> idsToDelete = new ArrayList<>();
            Cursor cursor = db.query(TABLE_DOCUMENTS,
                    new String[]{COL_DOC_ID, COL_DOC_PATH},
                    null, null, null, null, null);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String path = cursor.getString(1);
                File file = new File(path);

                if (!file.exists()) {
                    idsToDelete.add(id);
                    Log.d("DatabaseHelper", "Found orphaned document: " + path);
                }
            }
            cursor.close();

            for (int id : idsToDelete) {
                deleteDocument(id);
            }

            if (!idsToDelete.isEmpty()) {
                Log.i("DatabaseHelper", "Cleaned " + idsToDelete.size() + " orphaned documents");
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error cleaning orphaned documents", e);
        }
    }

    public List<Document> getAllDocuments() {
        // Clean orphans before returning documents
        cleanOrphanedDocuments();

        List<Document> docList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase(); // Use readable database
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DOCUMENTS, null);

        if (cursor.moveToFirst()) {
            do {
                Document doc = new Document();
                doc.setId(cursor.getInt(0));
                doc.setName(cursor.getString(1));
                doc.setPath(cursor.getString(2));
                docList.add(doc);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return docList;
    }

    public void deleteDocument(int docId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Get file path
            Cursor cursor = db.query(TABLE_DOCUMENTS,
                    new String[]{COL_DOC_PATH},
                    COL_DOC_ID + "=?",
                    new String[]{String.valueOf(docId)},
                    null, null, null);

            String filePath = null;
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(0);
            }
            cursor.close();

            // Delete associated chunks
            db.delete(TABLE_CHUNKS, COL_DOC_ID_FK + " = ?",
                    new String[]{String.valueOf(docId)});

            // Delete the document
            db.delete(TABLE_DOCUMENTS, COL_DOC_ID + " = ?",
                    new String[]{String.valueOf(docId)});

            // Delete physical file
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists() && file.delete()) {
                    Log.d("DatabaseHelper", "File deleted: " + filePath);
                } else if (file.exists()) {
                    Log.w("DatabaseHelper", "Failed to delete file: " + filePath);
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting document", e);
        }
    }

    public void deleteAllDocuments() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Get all document paths
            List<String> filePaths = new ArrayList<>();
            Cursor cursor = db.query(TABLE_DOCUMENTS,
                    new String[]{COL_DOC_PATH},
                    null, null, null, null, null);

            while (cursor.moveToNext()) {
                filePaths.add(cursor.getString(0));
            }
            cursor.close();

            // Delete all database records
            db.delete(TABLE_CHUNKS, null, null);
            db.delete(TABLE_DOCUMENTS, null, null);

            // Delete all files
            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (file.exists() && file.delete()) {
                    Log.d("DatabaseHelper", "Deleted file: " + filePath);
                } else if (file.exists()) {
                    Log.w("DatabaseHelper", "Failed to delete file: " + filePath);
                }
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error deleting all documents", e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    // Delete single QA item ======
    public void deleteQA(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QA, COL_QA_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Delete all QA history ======
    public void deleteAllQA() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_QA, null, null);
    }
}