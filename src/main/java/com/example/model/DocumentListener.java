package com.example.model;

public interface DocumentListener {
    public void onContentChanged(String newText);//if text content was modified
    public void onFilePathChanged(String newPath); //if file was renamed or saved
    public void onDirtyStateChanged(boolean isDirty); //file from unsaved to saved or vice versa
    public void onDocumentClose(); //if the document was closed 

} 
