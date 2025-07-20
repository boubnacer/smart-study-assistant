package com.example.smartstudyassistant.utils;

import android.content.Context;
import android.util.Log;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class DocumentTextExtractor {

    public static List<String> extractText(Context context, String filePath) {
        List<String> chunks = new ArrayList<>();
        try {
            Log.d("TextExtractor", "Starting extraction for: " + filePath);
            String extension = FilenameUtils.getExtension(filePath).toLowerCase();
            String text = "";

            switch (extension) {
                case "pdf":
                    text = extractPdfText(context, filePath);
                    break;
                case "txt":
                    text = extractTextFile(filePath);
                    break;
                case "docx":
                    text = extractDocxText(filePath);
                    break;
                default:
                    Log.e("TextExtractor", "Unsupported file type: " + filePath);
                    return chunks;
            }

            Log.d("TextExtractor", "Extracted text length: " + text.length());
            chunks = splitTextIntoChunks(text, 2000);
            Log.d("TextExtractor", "Created " + chunks.size() + " chunks");

        } catch (Exception e) {
            Log.e("TextExtractor", "Error extracting text from " + filePath, e);
        }
        return chunks;
    }

    private static String extractPdfText(Context context, String filePath) {
        try {
            PDFBoxResourceLoader.init(context);
            File file = new File(filePath);
            try (PDDocument document = PDDocument.load(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (Exception e) {
            Log.e("TextExtractor", "PDF extraction failed", e);
            return "";
        }
    }

    private static String extractTextFile(String filePath) {
        try (Scanner scanner = new Scanner(new File(filePath), StandardCharsets.UTF_8.name())) {
            return scanner.useDelimiter("\\A").next();
        } catch (Exception e) {
            Log.e("TextExtractor", "Text file extraction failed", e);
            return "";
        }
    }

    private static String extractDocxText(String filePath) {
        try (ZipFile zipFile = new ZipFile(filePath)) {
            Log.d("TextExtractor", "Processing DOCX file: " + filePath);
            ZipEntry entry = zipFile.getEntry("word/document.xml");
            if (entry != null) {
                Log.d("TextExtractor", "Found document.xml in DOCX");
                try (InputStream is = zipFile.getInputStream(entry)) {
                    // ... [existing extraction code] ...
                }
            } else {
                Log.e("TextExtractor", "document.xml not found in DOCX archive");
            }
        } catch (Exception e) {
            Log.e("TextExtractor", "DOCX extraction failed", e);
        }
        return "";
    }

    private static List<String> splitTextIntoChunks(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        for (int start = 0; start < text.length(); start += maxChunkSize) {
            int end = Math.min(text.length(), start + maxChunkSize);
            chunks.add(text.substring(start, end));
        }
        return chunks;
    }
}