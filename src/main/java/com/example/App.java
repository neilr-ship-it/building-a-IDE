package com.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;


import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCode;



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
    private File selectedDir;
    private TextArea consoleArea;
    //fields needed for search and replace ui components
    private HBox searchBar;
    private TextField searchField; //user input box for search term
    private TextField replaceField;//user input box for replacement text
    private Button nextButton;
    private Button prevButton;
    private Button closeSearchButton;
    private Button replaceButton;

    @Override
    public void start(Stage stage) {
        tabDocumentMap = new HashMap<>();
        this.primaryStage = stage;
        fileService = new FileService();

        stage.setTitle("JCodePad");

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select project folder");
        this.selectedDir = directoryChooser.showDialog(stage);
        if(selectedDir == null) {
            System.out.println("no folder chose so exiting");
            Platform.exit();
            return;
        }
        stage.setScene(new Scene(buildMainUI(selectedDir), 800, 600));
        stage.show();
    }

    private Parent buildMainUI(File selectedStartDir) {
    menuBar = new MenuBar();

    Menu editMenu = new Menu("Edit");
    MenuItem undoItem = new MenuItem("Undo");
    MenuItem redoItem = new MenuItem("Redo");

    undoItem.setOnAction(e -> performUndo());
    redoItem.setOnAction(e -> performRedo());
    undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN)); // Ctrl/Cmd + Z
    redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN)); // Ctrl/Cmd + Y

    Menu fileMenu = new Menu("File");
    MenuItem newItem = new MenuItem("New");
    MenuItem openItem = new MenuItem("Open");
    MenuItem saveItem = new MenuItem("Save");
    MenuItem saveAsItem = new MenuItem("Save As");
    MenuItem runItem = new MenuItem("Run");

    newItem.setOnAction(e -> newDocument());
    openItem.setOnAction(e -> openDocument());
    saveItem.setOnAction(e -> saveDocument());
    saveAsItem.setOnAction(e -> saveDocumentAs());
    runItem.setOnAction(e -> runCurrentFile()); //implement run current file method

    newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN)); // Ctrl/Cmd + N
    openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN)); // Ctrl/Cmd + O
    saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)); // Ctrl/Cmd + S
    saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)); // Ctrl/Cmd + Shift + S

    fileMenu.getItems().addAll(newItem, openItem, saveItem, saveAsItem, runItem);
    editMenu.getItems().addAll(undoItem, redoItem);
    menuBar.getMenus().addAll(fileMenu, editMenu);

    tabPane = new TabPane();
    BorderPane root = new BorderPane();
    root.setTop(menuBar);
    root.setCenter(tabPane);
    //build the treview taking in the selectedStartDir parameter file 
    TreeView<File> treeView = new TreeView<>();
    TreeItem<File> rootItem = buildFileTree(selectedStartDir);
    treeView.setRoot(rootItem);
    treeView.setShowRoot(true);
    root.setLeft(treeView);
    treeView.setPrefWidth(250);
    //hooking up search bar ui 
    searchBar = createSearchBar();
    VBox topContainer = new VBox(menuBar, searchBar); 
    root.setTop(topContainer);

    //code to attatch key binding control f to open search bar and if its open and 
    //user clicks escape then we close it, done using lambdas 
    root.setOnKeyPressed(event -> {
        if(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN).match(event)) {
            searchBar.setVisible(true);
            searchField.requestFocus();
        } else if(event.getCode() == KeyCode.ESCAPE && searchBar.isVisible()) {
            searchBar.setVisible(false);
        }
    });

    //initializing the console area text area for where the compiled output will show up
    consoleArea = new TextArea();
    consoleArea.setEditable(false); // shouldnt be able to edit compiled output
    consoleArea.setPrefHeight(150); //adjust as needed 
    VBox bottomPanel = new VBox(consoleArea);
    root.setBottom(bottomPanel);

    treeView.setCellFactory(tv -> new TreeCell<>() {
        //using a boolean flag to represent whether the cell is empty 
        //means the file does not currently have a valid item 
        @Override
        protected void updateItem(File file, boolean empty) {
            //using super calls original update item method to keep internal behavior 
            //proper, always do this to avoid bugs with the cell state
            super.updateItem(file, empty);
            if(empty || file == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
            } else {
                setText(file.getName());

                //code to load icons
                ImageView iconView;
                if(file.isDirectory()) {
                    iconView = new ImageView(getClass().getResource("/icons/folder-Icon.png").toExternalForm());
                } else {
                    iconView = new ImageView(getClass().getResource("/icons/file-icon.png").toExternalForm());
                }
                iconView.setFitHeight(16);
                iconView.setFitWidth(16);
                setGraphic(iconView);

                MenuItem renameItem = new MenuItem("Rename");
                MenuItem deleteItem = new MenuItem("Delete");
                MenuItem newFileItem = new MenuItem("New File");
                MenuItem newFolderItem = new MenuItem("New Folder");

                renameItem.setOnAction(e -> {
                    renameFile(file, getTreeItem());
                });

                deleteItem.setOnAction(e -> {
                // Placeholder - implement helper later
                deleteFile(file, getTreeItem());
                });

                newFileItem.setOnAction(e -> {
                    //getting the folder that we want to make a nerw file in the same directory
                    File filee = getItem();
                    if(filee != null && filee.isDirectory()) {
                        createNewFile(filee, getTreeItem());
                    }
                });

                newFolderItem.setOnAction( e -> {
                    File folder = getItem();
                    if(folder != null && folder.isDirectory()) {
                        createNewFolder(file, getTreeItem());
                    }
                });

                ContextMenu menu = new ContextMenu(renameItem, deleteItem, newFileItem, newFolderItem);
                setContextMenu(menu);
            }
        }
    });

    treeView.setOnMouseClicked(event -> {
        //set the listener to see when a user clics on tree view file 
        TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
        if(selectedItem == null) {
            return;
        }
        //convert the tree item we got to a file so that we can open it in the editor
        File clickedFile = selectedItem.getValue();
        //ensure a check to make sure the file we got is not a folder as then we dont
        if(clickedFile.isFile()) {
            //call helper method open document from file(parameter: clicked file)
            openDocumentFromFile(clickedFile);
        }

    });

    //quick test to see if tree veiw is working
    //TreeItem<File> dummyRoot = new TreeItem<>(new File("Dummy Project"));
    //TreeItem<File> file1 = new TreeItem<>(new File("Main.java"));
    //TreeItem<File> file2 = new TreeItem<>(new File("utils/Helper.java"));
    //TreeItem<File> dir1 = new TreeItem<>(new File("utils"));

    //dir1.getChildren().add(file2); // file 2 should be a child of dir 1
    //dummyRoot.getChildren().addAll(file1, dir1);
    //treeView.setRoot(dummyRoot);

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
    tab.setContent(wrapWithLineNumbers(editorArea));
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
        fileService.saveAs(file, doc);
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

private ScrollPane wrapWithLineNumbers(TextArea editor) {
    VBox lineNumberBox = new VBox();
    //setting the style of how the line numbers should look
    lineNumberBox.setStyle("-fx-padding: 5; -fx-background-color: #f0f0f0;");
    //setting a fixed width for the line number column 
    lineNumberBox.setPrefWidth(40);
    //connect vertical scroll position of the text area to the line number box,
    //scroll top property gives the current vertical scrole value and we negatie it 
    //using -newVal.doubleValue() to move vbox up wheenver user scrolls down. visually
    //syncing line number to lines in editor
    //lambda expression is a short way to write a implementation of a functional 
    //interface, here it is used to listen for changes to the scrolltopproperty
    //the parameters are obs is the observable object which is scrolltopproperty and 
    //old val is the previous scroll position and new val is the new scroll position
    //lambda defines a changeListener<Double> that gets triggered on scroll changes 
    editor.scrollTopProperty().addListener((obs, oldVal, newVal) -> {
        lineNumberBox.setLayoutY(-newVal.doubleValue());
    });

    //use another lambda to listen for changes in the text area
    editor.textProperty().addListener((obs, oldText, newText) -> {
        //every time the contnet changes it triggers content in this block to update
        //the line numbers
        //line count: 
        int lineCount = editor.getText().split("\n", -1).length;
        lineNumberBox.getChildren().clear();
        //every time contnet changes we rebuild the line number list otherwise we 
        //would just add 1 again from where we are and keep going resulting in dups
        //loops from 1 to linecount to generate new line numbers 
        for(int i = 1; i <= lineCount; i++) {
            Label lineLabel = new Label(String.valueOf(i));
            lineLabel.setStyle("-fx-font-size: 12; -fx-text-fill: gray;");
            lineLabel.setMinHeight(18);
            //add the labels to the VBox so it shows up in uw 
            lineNumberBox.getChildren().add(lineLabel);
        }
    });
    //create a new Hbox that place the line number box on the left and editor on right
    HBox container = new HBox(lineNumberBox, editor);
    ScrollPane scrollPane = new ScrollPane(container);
    scrollPane.setFitToHeight(true);
    scrollPane.setFitToWidth(true);
    return scrollPane;
}

 //build file tree method recursively converts a folder.directory into 
//a tree structure that can be displayed in a treeview 
private TreeItem<File> buildFileTree(File dir) {
    TreeItem<File> rootItem = new TreeItem<>(dir);
    //get all the children of the root item
    File[] children = dir.listFiles();
    if(children != null) {
        for(File child : children) {
            if(child.isDirectory()) {
                TreeItem<File> currChildTree = buildFileTree(child);
                rootItem.getChildren().add(currChildTree);
            } else {
                rootItem.getChildren().add(new TreeItem<>(child));
            }
        }
    }
    return rootItem;
}

//helper method to open document given a file, helps to implement tree view ability
//to click on any file in the tree view and open in editor, the clicked file is the 
//file that is being passed in as a parameter here in this method 
//done via converting file to doc using file service method and then using createEditorTab
//method to return an object that is just a editor space as its 2 fields are a doc and tab
//we maintain the map we have of tab to corresponding editor sessions as, also
//checks if a the file we are trying to open is already open in a tab by using helper method
//implemented below  
private void openDocumentFromFile(File file) {
    if(file == null) {
        return;
    }
    //check if tab of file we are trying to open is already open
    Tab existingTab = correspondingTabToFileCheck(file);
    if(existingTab != null) {
        tabPane.getSelectionModel().select(existingTab);
        return; // return out of the method if the existing tab is there for the file
        //dont implement below logic 
    }

    try{
        Document doc = fileService.open(file);
        TabWithEditor newOne = createEditorTab(doc, file.getName());
        tabDocumentMap.put(newOne.tab, new EditorSession(doc, newOne.editor));
    } catch(IOException e) {
        e.printStackTrace();
    }
}

//before opening a tab by click from tree view we want to add duplicate tab check 
//functionality, if a file is already open in a tab, dont open it again just stay
//focused on the existing tab, this is a helper method to see if a file is already open
//if a tab is open that means the file is open, for the file we click we want to,
//see if its tab is open, method is to check if the parameter file path and doc open in
//tab file path is the same 
private Tab correspondingTabToFileCheck(File file) {
    for(Tab currTab : tabDocumentMap.keySet()) {
        Document currDoc = tabDocumentMap.get(currTab).getDocument();
        if(file.getAbsolutePath().equals(currDoc.getFilePath())) {
            return currTab;
        }
    }
    return null;
}

//add a right click menu to the sidebar that shows options depends on whether user 
//click is on a file or a folder, with a IDE style file explorer using Treeview<File>
//each tree cell represents one FILe (either a file or folder) showin in the tree view
//each tree cell <File> is the visual part a row in the treeView UI that displays 
//the file/folder form its corresponding tree item
//method to prompt user for a new file name then rename the file
//file is the real world file, and tree Item is the visual file view node in treeview 
private void renameFile(File file, TreeItem<File> treeItem) {
    TextInputDialog dialog = new TextInputDialog(file.getName());
    dialog.setTitle("Rename File");
    dialog.setHeaderText("Rename " + file.getName());
    dialog.setContentText("New name: ");
    dialog.showAndWait().ifPresent(newName -> {
        //creating a new file object in same folder as old file and with the new name 
        File newFile = new File(file.getParentFile(), newName);
        //tries to rename the original file to the new file and returns true if success
        boolean success = file.renameTo(newFile);
        if(success) {
            treeItem.setValue(newFile);
        } else {
            showError("Rename Failed", "Couldnt Rename " + file.getName());
        }
    });
}

//delete file method first uses an alert to confirm the file we actually want to delete it
private void deleteFile(File file, TreeItem<File> treeItem) {
    Alert confirmation = new Alert(AlertType.CONFIRMATION);
    confirmation.setTitle("Delete File");
    confirmation.setHeaderText(" do you want to delete " + file.getName());
    confirmation.showAndWait().ifPresent(result -> {
        if(result == ButtonType.OK) {
            boolean success = file.delete();
            if (success) {
                treeItem.getParent().getChildren().remove(treeItem);
            } else {
                showError("Delete Failed", "could not delete " + file.getName());
            }
        }
    });
}
//helper method to display error for delete file rename file and other cell menu operations
private void showError(String title, String message) {
    Alert error = new Alert(Alert.AlertType.ERROR);
    error.setTitle(title);
    error.setHeaderText(message);
    error.showAndWait();
}

//method to create a new file, we want the parent File object (folder) and parent tree item as paraemters
//because new file should go in that directory 
private void createNewFile(File parentDir, TreeItem<File> parentItem) {
    TextInputDialog dialog = new TextInputDialog("NewFile.txt");
    dialog.setTitle("Create new File");
    dialog.setContentText("File Name: ");
    dialog.setHeaderText("Create a new file in " + parentDir.getName());

    dialog.showAndWait().ifPresent(fileName -> {
        //creates a file object in memory not on disk yet .createNewFile
        //creates the file on disk if it doesnt exist already there yet
        File newFile = new File(parentDir, fileName);
        try{
            boolean success = newFile.createNewFile();
            if(success) {
                parentItem.getChildren().add(new TreeItem<>(newFile));
            } else {
                showError("Creation failed", "Could not create File " + fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error", "An error occured " + e.getMessage());
        }
    });
}
//same to create new file we want the parent file object and parent tree item
//.mkdir method attempts to create a single directory(folder) on the disk,
//it fails if the parent directory does not exist 
//java f
private void createNewFolder(File parentDir, TreeItem<File> parentItem) {
    TextInputDialog dialog = new TextInputDialog("NewFolder");
    dialog.setTitle("Create a new folder");
    dialog.setContentText("Folder name: ");
    dialog.setHeaderText("Create a new fodler in " + parentDir.getName());

    dialog.showAndWait().ifPresent(folderName -> {
        File newFolder = new File(parentDir, folderName);
        if(newFolder.mkdir()) {
            parentItem.getChildren().add(buildFileTree(newFolder));
        } else {
            showError("Creation failed", "Could not create folder " + folderName);
        }
    });
}

//starter implementation of run current file, run it on a background thread and stream
//output to console
//compilation and run in one thread, first create compileandRUntaskclass that implements
//runnable to implement the logic we want thread to execute in the run method 
//implement the run current file method 
    private void runCurrentFile() {
        //first make sure that the tab and editor session for file i am trying to
        //run actually exist
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if(currentTab == null) {
            return;
        }

        EditorSession session = tabDocumentMap.get(currentTab);
        if(session == null) {
            return;
        }

        //make sure that the className we are passing is not null
        Document currDoc = session.getDocument();
        String content = currDoc.getContent();
        String className = extractClassName(content);
        if(className == null) {
            consoleArea.appendText("Error: could not find public class name");
            return;
        }
        //tempdir logic is for if file is unsaved, then ide gets treated like a scratchpad
        File fileToRun;
        //check if the file is already saved, then there is no need to create a temp dir- this was
        //my original idea 
        if(currDoc.getFilePath() != null) {
            fileToRun = new File(currDoc.getFilePath());
        } else {
            //this handles if file is unsaved then we will save it to temp, we want to 
            //create a file object pointing to a temproary directory for my application
            // to store temporary files like .java or .class sysiogetProperty(java.io.tmpdir
            // is a built in java command that gives u the default temporary directory for your operating
            //system, java will use this path when we need place to store temporary files,
            //that are not user visible and are disposable, the second argument "jcodepad" is 
            //creating a subfolder inside the temp directory just for my app so instad of writing 
            //into /tmp/ we are writing into /tmp/jcodepad/, helps with cleanup, we can delete all of 
            //temp/jcodepad/ if needed, tempdir.mkdirs, actually creats /tmp/jcodepad if its not existing
            //sysprop line doesnt actually create the directory on file system just creates a file object
            //reference to file or dir that may exist may not, mkdirs line actually creates folder
            //on disk if it doesnt already exist, first line is like saying point to where /tmp/jcodepad
            //would be if it existed and the next line is like saying go create that folder o nthe disk 
            //if it doesnt already exist  
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "jcodepad");
            tempDir.mkdirs();
            fileToRun = new File(tempDir, className + ".java");
            //write code into temporary file from unsaved one 
            try (FileWriter writer = new FileWriter(fileToRun)) {
                writer.write(content);
            } catch (IOException e) {
                consoleArea.appendText("Failed to create temp file: " + e.getMessage() + "\n");
                return;
            }
        }
        //if temp file was created then create background thread and compile and run in bg 
        CompileAndRunTask currTask = new CompileAndRunTask(className, fileToRun);
        Thread compileAndRunThread = new Thread(currTask);
        compileAndRunThread.setDaemon(true);
        compileAndRunThread.start();

    }

    private String extractClassName(String content) {
        //split the file into lines and look for a lien starting with public class
        String[] lines = content.split("\\R"); // this splits on line breaks 
        for(String line : lines) {
            line = line.trim();
            if(line.startsWith("public class ")) {
                //split this line into its words so we can take word other then public class
                String[] classSplitter = line.split("\\s+");
                if(classSplitter.length >=3) {
                    return classSplitter[2].split("\\{")[0]; //got the dash syntax from gpt 
                }
            }
        }
        return null;
    }

private class CompileAndRunTask implements Runnable {
    //fields needed are name of file to run and directory from which to run the 
    //process
    private final String className;
    private final File javaFile;
    public CompileAndRunTask(String className, File javaFile) {
        this.javaFile = javaFile;
        this.className = className;
    }

    //override and implement the run method 
    @Override
    public void run() {
        try{
            //remember 2 steps to this process first compile and then rum
        //we are running the javac command first 
        //get the file we ant to run the command on
        ProcessBuilder compileBuilder = new ProcessBuilder("javac", javaFile.getAbsolutePath());
        //initialize the process
        Process compileProcess = compileBuilder.start();
        int exitCode = compileProcess.waitFor();
        if(exitCode != 0) {
            String error = new String(compileProcess.getErrorStream().readAllBytes());
            //we are right now in a background thread but since a compile error has 
            //occured and store in a var called error since consoleArea is a javafx 
            //ui component and only javaFX application thread can modify ui components
            //not the background threads we cant directly update from this bg thread.
            //we instead wrap the ui update in a lambda and pass it to platform.runlater
            //java fx will run this lambda on the ui thread, lambda works to append the
            //error message to the ui then returns from the method early
            Platform.runLater(() -> consoleArea.appendText("Compile error:\n " + error));
            return;
        }
        Platform.runLater(() -> consoleArea.appendText("Compilation succesful"));
        //we know have compiled code actually run the code, now that resulting .class file
        //is stored in the temp dir directory, cp is classpath tells java where to look for .class files
        File parentDir = javaFile.getParentFile();  
        ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", parentDir.getAbsolutePath(), className);
        Process runProcess = runBuilder.start();

        //captiure the output
        //buffered reader is used to read text input efficiently from a character stream,
        //like lines of input from a running process, process.getInput stream gives a byte stream
        //and inputStream reader converts it to character stream and buffered readers adds buffering 
        //to allow line by line reading 

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
        String line;
        while((line = outputReader.readLine()) != null) {
            String output = line;
            Platform.runLater(() -> consoleArea.appendText(output + "\n"));
        }

        while ((line = errorReader.readLine()) != null) {
            String error = line;
            Platform.runLater(() -> consoleArea.appendText("ERR: " + error + "\n"));
        }
        runProcess.waitFor();
        } catch(Exception e) {
            Platform.runLater(() -> consoleArea.appendText("Exception: " + e.getMessage()));
        }
    }
}

private HBox createSearchBar() {
    searchField = new TextField();
    searchField.setText("Find");
    replaceField = new TextField();
    replaceField.setText("Replace with");

    nextButton = new Button("Next");
    prevButton = new Button("Prev");
    replaceButton = new Button("Replace");
    closeSearchButton = new Button("X");

    nextButton.setOnAction(e -> findNext());
    prevButton.setOnAction(e -> findPrevious());
    replaceButton.setOnAction(e -> replaceCurrent());
    closeSearchButton.setOnAction(e -> searchBar.setVisible(false));

    HBox bar = new HBox(5, searchField, replaceField, nextButton, prevButton, replaceButton, closeSearchButton);
    bar.setStyle("-fx-padding: 5; -fx-background-color: #ddd;");
    bar.setVisible(false);
    return bar;

}

//helper methods for the search bar
//first need a method just to know what editor we are in
//because we have to use its text area 
private int lastSearchIndex = -1;

private TextArea getCurrEditor() {
    Tab currTab = tabPane.getSelectionModel().getSelectedItem();
    if(currTab == null) {
        return null;
    }
    EditorSession currEditorSession = tabDocumentMap.get(currTab);
    if (currEditorSession != null) {
        return currEditorSession.getEditor();
    } else {
        return null;
    }
}

private void findNext() {
    TextArea editor = getCurrEditor();
    if(editor == null) {
        return;
    }
    String query = searchField.getText();
    String text = editor.getText();
    if(query.isEmpty()) {
        return;
    }
    //if the index -1 meants it wasnt found from current start onwards so we then wrap
    //around to see if it exists from the start and if it does also highlight that 

    int startPos = editor.getCaretPosition();
    int index = text.indexOf(query, startPos);
    if(index == -1 && startPos > 0) {
        index = text.indexOf(query);
    }
    if(index >= 0) {
        editor.selectRange(index, index + query.length());
        lastSearchIndex = index;
    }
}
//start = query length - 1 to skip the current match and look earlier in the text
//if going back by 1 query takes us out of bounds of the current text then we want to
//wrap around and start at the end, last index of gives last occurnece before or at 
//that position if found index will be start position of that match if not found then -1

private void findPrevious() {
    TextArea editor = getCurrEditor();
    if(editor == null) {
        return;
    }
    String text = editor.getText();
    String query = searchField.getText();
    if(query.isEmpty()) {
        return;
    }
    int startPos = editor.getCaretPosition() - query.length() - 1;

    if(startPos < 0) {
        startPos = text.length() - 1;
    }
    int index = text.lastIndexOf(query, startPos);
    if(index >= 0) {
        editor.selectRange(index, index + query.length());
        lastSearchIndex = index;
    }
}

private void replaceCurrent() {
    TextArea editor = getCurrEditor();
    if(editor == null) {
        return;
    }
    if(editor.getSelectedText().equals(searchField.getText())) {
        editor.replaceSelection(replaceField.getText());
    }
    findNext();
}

    public static void main(String[] args) {
        launch();
    }

}