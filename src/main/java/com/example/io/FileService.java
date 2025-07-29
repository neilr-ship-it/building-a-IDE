package com.example.io;

import com.example.model.Document;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileService {
    public Document open(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder contentBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            contentBuilder.append(line).append("\n");
        }
        reader.close();
        return new Document(contentBuilder.toString(), file.getAbsolutePath());
    }

    public void save(File file ,Document doc) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter( file));
        writer.write(doc.getContent());
        writer.close();
        doc.setFilePath(file.getAbsolutePath());
        doc.markClean();
    }

    //saving for the first time as 
    public void saveAs(File file ,Document doc) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter( file));
        writer.write(doc.getContent());
        writer.close();
        doc.setFilePath(file.getAbsolutePath());
        doc.markClean();
    }

    
}
