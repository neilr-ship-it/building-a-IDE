package com.example;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.example.io.FileService;
import com.example.model.Document;
import com.example.model.DocumentListener;
import com.example.model.EditorSession;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private TabPane tabPane;
    private MenuBar menuBar;
    private Stage primaryStage;
    private FileService fileService;
    private HashMap<Tab, EditorSession> tabDocumentMap;

    @Override
    public void start(Stage stage) {
        tabDocumentMap = new HashMap<>();
        this.primaryStage = stage;
        fileService = new FileService();
        stage.setTitle("JCodePad");
        stage.setScene(new Scene(buildMainUI(), 800, 600));
        stage.show();
    }

    private Parent buildMainUI() {
       menuBar = new MenuBar();

       Menu editMenu = new Menu("Edit");
       MenuItem undoItem = new MenuItem("Undo");
       MenuItem redoItem = new MenuItem("Redo");

       undoItem.setOnAction(e -> performUndo());
       redoItem.setOnAction(e -> performRedo());

       Menu fileMenu = new Menu("File");
       MenuItem newItem = new MenuItem("New");
       MenuItem openItem = new MenuItem("Open");
       MenuItem saveItem = new MenuItem("save");
       MenuItem saveAsItem = new MenuItem("save as");

       saveItem.setOnAction(e -> saveDocument());
       saveAsItem.setOnAction(e -> saveDocumentAs());  
       openItem.setOnAction(e -> openDocument());
       newItem.setOnAction(e -> newDocument());// when user clicks new menu item run the 
       //new document method 
       fileMenu.getItems().addAll(newItem, openItem, saveItem, saveAsItem);
       editMenu.getItems().addAll(undoItem, redoItem);
       menuBar.getMenus().addAll(fileMenu, editMenu);
       

       tabPane = new TabPane();
       BorderPane root = new BorderPane();
       root.setTop(menuBar);;
       root.setCenter(tabPane);

       return root;
    }

    //need to return both tab and text area in creatEditorTab, can do it using a
    //static class which takesboth thsoe fields and consrtucts a tabWithEditor class
    //with the 2 fields we want then we make creatEditorTab return a TabWithEditor
    //object

    private static class TabWithEditor{
        Tab tab;
        TextArea editor;

        TabWithEditor(Tab tab, TextArea editor) {
            this.tab = tab;
            this.editor = editor;
        }
    }

    private void newDocument() {
    Document doc = new Document();
    TabWithEditor res = createEditorTab(doc, "untitled");
    tabDocumentMap.put(res.tab, new EditorSession(doc, res.editor));
    
}

private void openDocument() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("choose your file");
    File file = fileChooser.showOpenDialog(primaryStage);
    if (file == null) return;

    try {
        Document doc = fileService.open(file);
        TabWithEditor res = createEditorTab(doc, file.getName());
        tabDocumentMap.put(res.tab, new EditorSession(doc, res.editor));
    } catch (IOException e) {
        e.printStackTrace();
    }  
}

private TabWithEditor createEditorTab(Document doc, String tabTitle) {
    TextArea editorArea = new TextArea(doc.getContent());

    editorArea.textProperty().addListener((obs, oldText, newText) -> {
        doc.setContent(newText);
    });

    Tab tab = new Tab(tabTitle);
    tab.setContent(editorArea);
    tab.setClosable(true);

    doc.addListener(new DocumentListener() {
        @Override
        public void onContentChanged(String newText) {
            // optional future live update feature
        }

        @Override
        public void onFilePathChanged(String newPath) {
            tab.setText(new File(newPath).getName());
        }

        @Override
        public void onDirtyStateChanged(boolean isDirty) {
            String title = tab.getText();
            if (isDirty && !title.endsWith("*")) {
                tab.setText(title + "*");
            } else if (!isDirty && title.endsWith("*")) {
                tab.setText(title.substring(0, title.length() - 1));
            }
        }

        @Override
        public void onDocumentClose() {
            // future feature
        }
    });

    tabPane.getTabs().add(tab);
    tabPane.getSelectionModel().select(tab);

    return new TabWithEditor(tab, editorArea);
}

public void saveDocument() {
    //get the currently selected tab
    Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
    if(selectedTab == null) {
        return;
    }
    EditorSession session = tabDocumentMap.get(selectedTab);
    if(session == null) {
        return;
    }
    Document doc = session.getDocument();
    String filePath = doc.getFilePath();
    if(filePath != null) {
        //we need to first create a file 
        File file = new File(filePath);
        try{
            fileService.save(file, doc);
        }catch (IOException e) {
            e.printStackTrace();
        } 
    } else {
        saveDocumentAs();
    }
}

public void saveDocumentAs() {
    Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
    if(selectedTab == null) {
        return;
    }
    EditorSession session = tabDocumentMap.get(selectedTab);
    if(session == null) {
        return;
    }
    Document doc = session.getDocument();
    //use a fileChooser to let a user pick a save location
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("save file as");
    File file = fileChooser.showSaveDialog(primaryStage);
    if(file == null) {
         return;
    }
    //save the created file
    try{
        fileService.save(file, doc);
        selectedTab.setText(file.getName());
    }catch (IOException e) {
        e.printStackTrace();
    }
}

public void performUndo() {
    Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
    if(currentTab == null) {
        return;
    }
    EditorSession currSession = tabDocumentMap.get(currentTab);
    if(currSession != null) {
        currSession.getEditor().undo();
    }
}

private void performRedo() {
    Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
    if (selectedTab == null) return;

    EditorSession session = tabDocumentMap.get(selectedTab);
    if (session != null) {
        session.getEditor().redo();
    }
}

    public static void main(String[] args) {
        launch();
    }

}