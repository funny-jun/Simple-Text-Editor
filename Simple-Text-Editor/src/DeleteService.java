/*
 删除服务：支持删除指定行内的字符或跨多行的文本块。
 */
public class DeleteService {

    private TextModel textModel;

    public DeleteService(TextModel model) {
        this.textModel = model;
    }

    //删除指定行内的一段字符
    public void deleteChars(int lineIndex, int startCol, int length) {
        String line = textModel.getLine(lineIndex);
        if (startCol >= line.length()) return;

        int endCol = Math.min(startCol + length, line.length());
        String newLine = line.substring(0, startCol) + line.substring(endCol);
        textModel.updateLine(lineIndex, newLine);
    }

    /*
    删除文本块（可能跨行）
    单行：直接删除区间；多行：首行保留左段与末行右段并拼接，删除中间行。
     */
    public void deleteTextBlock(int startLine, int startCol, int endLine, int endCol) {
        if (startLine == endLine) {
            // 单行删除
            deleteChars(startLine, startCol, endCol - startCol);
        } else {
            // 多行删除
            String firstLine = textModel.getLine(startLine);
            String lastLine = textModel.getLine(endLine);

            // 更新首行
            String newFirstLine = firstLine.substring(0, startCol) + lastLine.substring(endCol);
            textModel.updateLine(startLine, newFirstLine);

            // 删除中间行：从首行之后开始逐一删除
            for (int i = startLine + 1; i <= endLine; i++) {
                textModel.deleteLine(startLine + 1);
            }
        }
    }
}
