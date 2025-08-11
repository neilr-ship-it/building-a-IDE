package com.example.model;

import java.util.ArrayList;

public class Document {
    private String content;
    private String filePath;
    private boolean isDirty;
    private ArrayList<DocumentListener> listeners;

    public Document(String content, String filePath) {
        this.content = content;
        this.filePath = filePath;
        this.isDirty = false;
        this.listeners = new ArrayList<>();
    }

    public Document() {
        this.content = "";
        this.filePath = null;
        this.isDirty = true;
        this.listeners = new ArrayList<>();
    }

    public void setContent(String newContent) {
        content = newContent;
        if(!this.isDirty) {
            isDirty = true;
            for(DocumentListener listener : listeners) {
                listener.onDirtyStateChanged(true);
            }
        }

        for(DocumentListener listener : listeners) {
            listener.onContentChanged(newContent);
        }
    }

    public void markClean() {
        isDirty = false;
    }

    public void setFilePath(String newPath) {
        if(newPath != null && !newPath.equals(filePath)) {
            filePath = newPath;
            for(DocumentListener listener : listeners) {
            listener.onFilePathChanged(newPath);
            }
        }    
    }

    public void addListener(DocumentListener listenerOne) {
        listeners.add(listenerOne);
    }

    public void removeListener(DocumentListener listenerTwo) {
        listeners.remove(listenerTwo);
    }

    //notification to all users that the document is closing 
    public void close() {
        for(DocumentListener listener : listeners) {
            listener.onDocumentClose();
        }
    }

    public String getContent() { return content; }

    public String getFilePath() { return filePath; }
    
    public boolean isDirty() { return isDirty; }
}
