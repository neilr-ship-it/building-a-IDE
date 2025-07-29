package com.example.model;
import javafx.scene.control.TextArea;

public class EditorSession {
    private final Document doc;
    private final TextArea editor;

    public EditorSession(Document doc, TextArea editor) {
        this.doc = doc;
        this.editor = editor;
    }

    public Document getDocument() {
        return doc;
    }

    public TextArea getEditor() {
        return editor;
    }

}
