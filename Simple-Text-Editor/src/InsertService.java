//插入服务：在指定行/列位置插入字符串或多行文本块。
public class InsertService {
    // 文本模型：插入操作直接修改其行内容与结构
    private TextModel textModel;

    public InsertService(TextModel model) {
        this.textModel = model;
    }

    //插入字符串到指定位置
    public void insertString(int lineIndex, int column, String text) {
        String line = textModel.getLine(lineIndex);
        StringBuilder newLine = new StringBuilder(line);
        newLine.insert(column, text);//插入字符串到指定位置
        textModel.updateLine(lineIndex, newLine.toString());
    }

    /*
     插入文本块（支持多行）
     首行插入到列位置前；末行与原首行列右侧拼接；中间行直接插入。
     */
    public void insertTextBlock(int lineIndex, int column, String[] textBlock) {
        if (textBlock == null || textBlock.length == 0) return;

        if (textBlock.length == 1) {
            insertString(lineIndex, column, textBlock[0]);
            return;
        }

        // 处理第一行：列左侧 + 第一块
        String firstLine = textModel.getLine(lineIndex);
        String newFirstLine = firstLine.substring(0, column) + textBlock[0];//插入第一块
        textModel.updateLine(lineIndex, newFirstLine);

        // 插入中间行：逐行插入到当前行之后
        for (int i = 1; i < textBlock.length - 1; i++) {
            textModel.insertLine(lineIndex + i, textBlock[i]);
        }

        // 处理最后一行：最后一块 + 原首行列右侧
        String lastLineContent = textBlock[textBlock.length - 1] +
                firstLine.substring(column);
        textModel.insertLine(lineIndex + textBlock.length - 1, lastLineContent);
    }
}
