import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Locale;

/*
 简易文本编辑器主窗口：负责菜单、文本区、状态栏与服务层的集成。
 提供查找替换、插入、块移动、删除、存盘与取盘等功能，并实时显示状态栏信息。
 架构要点：
 - 视图（JTextArea）与模型（TextModel）双向同步
 - 服务类（Find/Replace/Insert/Delete/BlockMove）在模型上执行具体编辑算法
 - FileService 负责将模型与磁盘文件读写
 */
public class TextEditor extends JFrame implements ActionListener {
    private JTextArea textArea;      // 主文本区域
    private JMenuBar menuBar;        // 菜单栏
    private JLabel statusLabel;      // 状态栏：显示行数、光标位置、选区长度等
    private TextModel textModel;     // 文本数据模型
    private FileService fileService; // 文件读写服务
    private JToolBar toolBar;        // 顶部工具栏
    private JTextArea lineNumberArea; // 行号视图

    // 算法服务：在模型上执行具体操作
    private FindReplaceService findReplaceService;
    private InsertService insertService;
    private BlockMoveService blockMoveService;
    private DeleteService deleteService;
    private boolean isModified = false;        // 是否有未保存修改
    private boolean isProgrammaticChange = false; // 程序性修改标记，用于避免监听递归触发
    private String currentFileName = "new.txt"; // 当前文件名

    /*
     构造函数：初始化模型、服务与界面组件，并建立文档监听
     */
    public TextEditor() {
        //设置 Java Swing 中的 Nimbus主题，提升外观与交互细节
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignore) {}
        // 统一按钮文案为英文
        Locale.setDefault(Locale.ENGLISH);
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("FileChooser.openButtonText", "Open");
        UIManager.put("FileChooser.saveButtonText", "Save");
        UIManager.put("FileChooser.cancelButtonText", "Cancel");
        // 初始化数据模型和服务
        textModel = new TextModel();
        fileService = new FileService(textModel);
        // 新增服务实例化
        findReplaceService = new FindReplaceService(textModel);
        insertService = new InsertService(textModel);
        blockMoveService = new BlockMoveService(textModel);
        deleteService = new DeleteService(textModel);

        createMenuBar();
        initUI();

        // 初始化行数显示
        updateStatusBar();

        // 文档监听，同步模型与行数
        setupDocumentSync();
        updateTitle();
    }

    /**
     * 创建菜单栏与菜单项，设置助记键与快捷键，并注册事件监听
     */
    private void createMenuBar() {
        menuBar = new JMenuBar();

        // File菜单
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit菜单
        JMenu editMenu = new JMenu("Edit");
        JMenuItem findItem = new JMenuItem("Find");
        JMenuItem replaceItem = new JMenuItem("Replace");
        JMenuItem insertItem = new JMenuItem("Insert");
        JMenuItem blockMoveItem = new JMenuItem("Block Move");
        JMenuItem deleteItem = new JMenuItem("Delete");

        editMenu.add(findItem);
        editMenu.add(replaceItem);
        editMenu.addSeparator();
        editMenu.add(insertItem);
        editMenu.add(blockMoveItem);
        editMenu.addSeparator();
        editMenu.add(deleteItem);

        // Help菜单
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);

        // 菜单助记键（Alt+字母）
        fileMenu.setMnemonic('F');
        editMenu.setMnemonic('E');
        helpMenu.setMnemonic('H');

        // 快捷键
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        replaceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
        insertItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK));
        blockMoveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        aboutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));

        // 添加事件监听
        newItem.addActionListener(this);
        openItem.addActionListener(this);
        saveItem.addActionListener(this);
        findItem.addActionListener(this);
        replaceItem.addActionListener(this);
        insertItem.addActionListener(this);
        blockMoveItem.addActionListener(this);
        deleteItem.addActionListener(this);
        aboutItem.addActionListener(this);
        exitItem.addActionListener(this);
    }

    /*
     初始化界面：放置文本区与状态栏，设置窗口属性
     */
    private void initUI() {
        setJMenuBar(menuBar);

        textArea = new JTextArea();
        // 文本区美化：等宽字体、自动换行、内边距、光标与选区颜色
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(8, 12, 8, 12));
        textArea.setCaretColor(new Color(30, 144, 255));
        textArea.setSelectionColor(new Color(204, 232, 255));
        textArea.setSelectedTextColor(Color.BLACK);
        textArea.addCaretListener(e -> updateStatusBar());

        JScrollPane scrollPane = new JScrollPane(textArea);
        // 行号视图：置于滚动窗格行头，随行数变化更新
        lineNumberArea = new JTextArea("1");
        lineNumberArea.setEditable(false);
        lineNumberArea.setBackground(new Color(245, 245, 245));
        lineNumberArea.setForeground(new Color(120, 120, 120));
        lineNumberArea.setFont(textArea.getFont());
        lineNumberArea.setMargin(new Insets(8, 8, 8, 8));
        scrollPane.setRowHeaderView(lineNumberArea);// 行号视图
        add(scrollPane, BorderLayout.CENTER);// 文本区视图
        // total lines: 0 setting
        statusLabel = new JLabel("total lines: 0");
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(250, 250, 250));
        add(statusLabel, BorderLayout.SOUTH);

        // 顶部工具栏：常用操作快速入口
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(createToolButton("New"));
        toolBar.add(createToolButton("Open"));
        toolBar.add(createToolButton("Save"));
        toolBar.addSeparator();
        toolBar.add(createToolButton("Find"));
        toolBar.add(createToolButton("Replace"));
        toolBar.addSeparator();
        toolBar.add(createToolButton("Insert"));
        toolBar.add(createToolButton("Block Move"));
        toolBar.addSeparator();
        toolBar.add(createToolButton("Delete"));
        add(toolBar, BorderLayout.NORTH);

        setTitle("Simple Text Editor");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    /*
     建立文档监听：将用户编辑变化同步到模型，并更新修改标记与状态栏
     */
    private void setupDocumentSync() {
        // 为文本区域的文档添加文档监听器
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isProgrammaticChange) { isModified = true; updateTitle(); }
                syncModelAndStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isProgrammaticChange) { isModified = true; updateTitle(); }
                syncModelAndStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!isProgrammaticChange) { isModified = true; updateTitle(); }
                syncModelAndStatus();
            }
        });
    }

    /*
     视图→模型同步：从文本区读取全文到模型，并刷新状态栏
     */
    private void syncModelAndStatus() {
        textModel.setContentFromString(textArea.getText());// 视图到模型同步
        updateStatusBar();
    }

    /*
      模型→视图刷新：将模型内容写回文本区（服务操作后调用），避免监听循环
     */
    private void refreshViewFromModel() {
        isProgrammaticChange = true;
        textArea.setText(textModel.getContentAsString());
        isProgrammaticChange = false;
        updateStatusBar();
    }

    /*
     更新状态栏：显示总行数、当前行列、选区长度、字符数与修改标记
     */
    private void updateStatusBar() {
        int total = textModel.getTotalLines();
        int caret = textArea.getCaretPosition();
        int[] lc = offsetToLineCol(caret);
        int sel = Math.max(0, textArea.getSelectionEnd() - textArea.getSelectionStart());
        int chars = textArea.getText().length();
        String modifiedStr = isModified ? " | modified" : "";
        statusLabel.setText("total lines: " + total + " | line: " + (lc[0] + 1) + ", col: " + lc[1] +
                " | selected: " + sel + " | chars: " + chars + modifiedStr);
        updateLineNumbers();
    }

    /*
     更新窗口标题（附加未保存修改标记*）
     */
    private void updateTitle() {
        setTitle("Simple Text Editor - " + currentFileName + (isModified ? "*" : ""));
    }

    /*
     将文本偏移量转换为行列位置
     @param offset 文本偏移量（JTextArea基于全文的字符索引）
     @return [行, 列]（0-based）
     */
    private int[] offsetToLineCol(int offset) {
        try {
            int line = textArea.getLineOfOffset(offset);
            int col = offset - textArea.getLineStartOffset(line);
            return new int[]{line, col};
        } catch (Exception ex) {
            return new int[]{0, 0};
        }
    }

    /*
     菜单事件分发：根据命令名称调用对应功能
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // 处理菜单项点击事件
        String command = e.getActionCommand();
        switch (command) {
            case "New":
                newFile();
                break;
            case "Open":
                openFile();
                break;
            case "Save":
                saveFile();
                break;
            case "Find":
                findText();
                break;
            case "Replace":
                replaceText();
                break;
            case "Insert":
                insertText();
                break;
            case "Block Move":
                blockMove();
                break;
            case "Delete":
                deleteText();
                break;
            case "About":
                showAbout();
                break;
            case "Exit":
                exitApplication();
                break;
        }
    }

    /*
     新建文件：清空视图与模型，重置状态
     */
    public void newFile() {
        isProgrammaticChange = true;
        textArea.setText("");
        textModel.setContentFromString("");
        isProgrammaticChange = false;
        isModified = false;
        currentFileName = "new file";
        updateTitle();
        updateStatusBar();
    }

    /*
     打开文件：选择路径后用文件服务读取到模型并刷新视图
     */
    public void openFile() {
        JFileChooser fileChooser = new JFileChooser();//创建文件选择器
        int result = fileChooser.showOpenDialog(this);//显示打开文件对话框，并获取用户操作结果
        if (result == JFileChooser.APPROVE_OPTION) {//如果用户选择了文件
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();//获取用户选择的文件路径
            if (fileService.openFile(filePath)) {
                isProgrammaticChange = true;
                refreshViewFromModel();//刷新视图，显示打开的文件内容
                isProgrammaticChange = false;
                currentFileName = fileChooser.getSelectedFile().getName();
                isModified = false;
                updateTitle();
                JOptionPane.showMessageDialog(this, "File opened successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "File open failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /*
     保存文件：同步模型并写出到用户选择的路径
     */
    public void saveFile() {
        // 先同步模型与行数（保证保存的是当前文本区域内容）
        syncModelAndStatus();

        JFileChooser fileChooser = new JFileChooser();//创建文件选择器
        int result = fileChooser.showSaveDialog(this);//显示保存文件对话框，并获取用户操作结果
        if (result == JFileChooser.APPROVE_OPTION) {//如果用户选择了文件
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();//获取用户选择的文件路径
            if (fileService.saveFile(filePath)) {
                currentFileName = fileChooser.getSelectedFile().getName();
                isModified = false;
                updateTitle();
                updateStatusBar();
                JOptionPane.showMessageDialog(this, "File saved successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "File save failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 工具栏按钮构造：复用菜单命令与事件处理
    private JButton createToolButton(String command) {
        JButton btn = new JButton(command);
        btn.setFocusable(false);
        btn.setActionCommand(command);
        btn.addActionListener(this);
        return btn;
    }

    // 行号更新：根据模型总行数生成行号文本
    private void updateLineNumbers() {
        if (lineNumberArea == null) return;
        int total = Math.max(1, textModel.getTotalLines());
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= total; i++) {
            sb.append(i).append('\n');
        }
        if (!sb.isEmpty()) sb.setLength(sb.length() - 1); // 去除末尾换行
        lineNumberArea.setText(sb.toString());
    }

    /*
     查找文本：弹窗输入，查找后选中首个匹配并提示总数
     */
    public void findText() {
        String searchText = JOptionPane.showInputDialog(this, "Enter text to find:");
        if (searchText != null && !searchText.trim().isEmpty()) {
            // 先同步一次，确保模型与文本框一致
            syncModelAndStatus();
            List<TextPosition> positions = findReplaceService.findText(searchText);//查找文本
            if (positions != null && !positions.isEmpty()) {
                TextPosition p = positions.getFirst();
                try {
                    int startOffset = textArea.getLineStartOffset(p.getLine()) + p.getColumn();//起始偏移量：行起始偏移量+列位置引索
                    textArea.setCaretPosition(startOffset);//设置光标位置
                    textArea.select(startOffset, startOffset + p.getLength());//选择匹配文本
                    textArea.grabFocus();//获取焦点
                } catch (Exception ignore) {}
                JOptionPane.showMessageDialog(this, "Found " + positions.size() + " occurrences.");
            } else {
                JOptionPane.showMessageDialog(this, "Text not found: " + searchText);
            }
        }
    }

    /*
     替换文本：支持等长/不等长两种策略，操作后刷新视图并更新状态
     */
    public void replaceText() {
        String target = JOptionPane.showInputDialog(this, "Enter text to find:");
        if (target == null || target.isEmpty()) return;
        String replacement = JOptionPane.showInputDialog(this, "Enter replacement text:");
        if (replacement == null) return;
        // 同步模型，确保替换基于最新文本
        syncModelAndStatus();
        try {
            int count = findReplaceService.replaceService(target, replacement);
            refreshViewFromModel();
            isModified = true;
            updateTitle();
            updateStatusBar();
            JOptionPane.showMessageDialog(this, "Replacements made: " + count);//提示替换次数
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    /*
     插入文本：支持插入字符串或多行文本块，以当前光标位置为插入点
     */
    public void insertText() {
        JRadioButton insertStringBtn = new JRadioButton("Insert String", true);
        JRadioButton insertBlockBtn = new JRadioButton("Insert Text Block");
        ButtonGroup group = new ButtonGroup(); //创建按钮组，确保只能选择一个单选按钮
        group.add(insertStringBtn); group.add(insertBlockBtn);//添加单选按钮到按钮组

        JPanel panel = new JPanel(new BorderLayout());//创建面板，用于显示对话框内容
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(insertStringBtn); top.add(insertBlockBtn);//添加单选按钮
        panel.add(top, BorderLayout.NORTH);//添加单选按钮面板到北区域

        JTextField stringField = new JTextField();
        JTextArea blockArea = new JTextArea(8, 30);
        JPanel center = new JPanel(new CardLayout());
        JPanel stringCard = new JPanel(new BorderLayout());//创建面板，用于显示字符串输入框
        stringCard.add(new JLabel("Enter string:"), BorderLayout.NORTH);
        stringCard.add(stringField, BorderLayout.CENTER);
        JPanel blockCard = new JPanel(new BorderLayout());//创建面板，用于显示文本块输入框
        blockCard.add(new JLabel("Enter text block (supports multi-line):"), BorderLayout.NORTH);
        blockCard.add(new JScrollPane(blockArea), BorderLayout.CENTER);
        center.add(stringCard, "string");
        center.add(blockCard, "block");
        panel.add(center, BorderLayout.CENTER);
        CardLayout cl = (CardLayout) center.getLayout();
        insertStringBtn.addActionListener(evt -> cl.show(center, "string"));
        insertBlockBtn.addActionListener(evt -> cl.show(center, "block"));

        int result = JOptionPane.showConfirmDialog(this, panel, "Insert Text", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            // 同步模型，以当前光标位置为插入点
            syncModelAndStatus();
            int caret = textArea.getCaretPosition();//获取光标位置
            int[] lc = offsetToLineCol(caret);//将光标位置转换为行列
            if (insertStringBtn.isSelected()) {
                String textToInsert = stringField.getText();//获取要插入的字符串
                insertService.insertString(lc[0], lc[1], textToInsert);//插入字符串
            } else {
                String blockText = blockArea.getText();//获取文本块内容
                String[] lines = blockText.split("\n", -1);//将文本块内容按行分割
                insertService.insertTextBlock(lc[0], lc[1], lines);//插入文本块
            }
            refreshViewFromModel();
        }
    }

    /*
     块移动：支持行块与列块两种方式，输入参数后执行并刷新视图
     */
    public void blockMove() {
        JRadioButton lineBlockBtn = new JRadioButton("Line Block Move", true);
        JRadioButton colBlockBtn = new JRadioButton("Column Block Move");
        ButtonGroup group = new ButtonGroup();//创建按钮组，确保只能选择一个单选按钮
        group.add(lineBlockBtn); group.add(colBlockBtn);//添加单选按钮到按钮组

        JPanel panel = new JPanel(new BorderLayout());//创建面板，用于显示对话框内容
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(lineBlockBtn); top.add(colBlockBtn);
        panel.add(top, BorderLayout.NORTH);

        // 行块参数
        JPanel linePanel = new JPanel(new GridLayout(3, 2));//创建面板，用于显示行块参数输入框
        JTextField startLineField = new JTextField();
        JTextField endLineField = new JTextField();
        JTextField targetLineField = new JTextField();
        linePanel.add(new JLabel("Start line (1-based):")); linePanel.add(startLineField);
        linePanel.add(new JLabel("End line (1-based):")); linePanel.add(endLineField);
        linePanel.add(new JLabel("Target line (1-based):")); linePanel.add(targetLineField);

        // 列块参数
        JPanel colPanel = new JPanel(new GridLayout(6, 2));//创建面板，用于显示列块参数输入框
        JTextField cStartLineField = new JTextField();
        JTextField cEndLineField = new JTextField();
        JTextField cStartColField = new JTextField();
        JTextField cEndColField = new JTextField();
        JTextField cTargetLineField = new JTextField();
        JTextField cTargetColField = new JTextField();
        colPanel.add(new JLabel("Start line (1-based):")); colPanel.add(cStartLineField);
        colPanel.add(new JLabel("End line (1-based):")); colPanel.add(cEndLineField);
        colPanel.add(new JLabel("Start column (0-based):")); colPanel.add(cStartColField);
        colPanel.add(new JLabel("End column (0-based):")); colPanel.add(cEndColField);
        colPanel.add(new JLabel("Target line (1-based):")); colPanel.add(cTargetLineField);
        colPanel.add(new JLabel("Target column (0-based):")); colPanel.add(cTargetColField);

        JPanel center = new JPanel(new CardLayout());//创建面板，用于显示行块参数输入框与列块参数输入框
        center.add(linePanel, "line");//将行块参数输入框添加到面板中
        center.add(colPanel, "col");//将列块参数输入框添加到面板中
        panel.add(center, BorderLayout.CENTER);//将面板添加到对话框中
        CardLayout cl = (CardLayout) center.getLayout();//获取卡片布局管理器
        lineBlockBtn.addActionListener(evt -> cl.show(center, "line"));//添加行块参数输入框显示监听器
        colBlockBtn.addActionListener(evt -> cl.show(center, "col"));//添加列块参数输入框显示监听器

        int result = JOptionPane.showConfirmDialog(this, panel, "Block Move", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            syncModelAndStatus();
            try {
                if (lineBlockBtn.isSelected()) {
                    int s = Integer.parseInt(startLineField.getText()) - 1;//获取起始行
                    int e = Integer.parseInt(endLineField.getText()) - 1;//获取结束行
                    int t = Integer.parseInt(targetLineField.getText()) - 1;//获取目标行
                    blockMoveService.moveLineBlock(s, e, t);//移动行块
                } else {
                    int sL = Integer.parseInt(cStartLineField.getText()) - 1;//获取起始行
                    int eL = Integer.parseInt(cEndLineField.getText()) - 1;//获取结束行
                    int sC = Integer.parseInt(cStartColField.getText());//获取起始列
                    int eC = Integer.parseInt(cEndColField.getText());//获取结束列
                    int tL = Integer.parseInt(cTargetLineField.getText()) - 1;//获取目标行
                    int tC = Integer.parseInt(cTargetColField.getText());//获取目标列
                    blockMoveService.moveColumnBlock(sL, eL, sC, eC, tL, tC);//移动列块
                }
                refreshViewFromModel();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Parameter error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /*
     删除文本：优先根据选区删除；无选区时弹窗输入删除范围
     */
    public void deleteText() {
        if (textArea.getSelectedText() != null) {
            // 将选择区域转换为起止行列并使用删除服务
            int start = textArea.getSelectionStart();
            int end = textArea.getSelectionEnd();
            int[] sLC = offsetToLineCol(start);//获取起始行列
            int[] eLC = offsetToLineCol(end);//获取结束行列
            syncModelAndStatus();
            deleteService.deleteTextBlock(sLC[0], sLC[1], eLC[0], eLC[1]);
            refreshViewFromModel();
        } else {
            // 无选择时，弹框输入要删除的范围
            JPanel panel = new JPanel(new GridLayout(4, 2));
            JTextField sLine = new JTextField();
            JTextField sCol = new JTextField();
            JTextField eLine = new JTextField();
            JTextField eCol = new JTextField();
            panel.add(new JLabel("Start line (1-based):")); panel.add(sLine);
            panel.add(new JLabel("Start column (0-based):")); panel.add(sCol);
            panel.add(new JLabel("End line (1-based):")); panel.add(eLine);
            panel.add(new JLabel("End column (0-based):")); panel.add(eCol);
            int result = JOptionPane.showConfirmDialog(this, panel, "Delete Text Block", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    int sl = Integer.parseInt(sLine.getText()) - 1;//获取起始行
                    int sc = Integer.parseInt(sCol.getText());//获取起始列
                    int el = Integer.parseInt(eLine.getText()) - 1;//获取结束行
                    int ec = Integer.parseInt(eCol.getText());//获取结束列
                    syncModelAndStatus();
                    deleteService.deleteTextBlock(sl, sc, el, ec);
                    refreshViewFromModel();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Parameter error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /*
     关于窗口：显示项目信息
     */
    public void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Simple Text Editor\nVersion 1.0\nCourse Design Project",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /*
     退出应用：确认后关闭程序
     */
    public void exitApplication() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?",
                "Exit",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    /*
     程序入口：在 EDT中创建并显示主窗口
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TextEditor().setVisible(true);
        });
    }
}
