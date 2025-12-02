import java.util.ArrayList;
import java.util.List;

/*
 块移动服务：支持行块移动与列块（矩形选区）移动。
 行块：整体剪切并插入到目标行位置；列块：逐行提取列区并在目标位置插入。
 */

public class BlockMoveService {
    // 文本模型
    private TextModel textModel;

    public BlockMoveService(TextModel model) {
        this.textModel = model;
    }

    //行块移动：将[startLine, endLine]范围内的整行剪切并插入到targetLine位置
    public void moveLineBlock(int startLine, int endLine, int targetLine) {
        if (startLine < 0 || endLine >= textModel.getTotalLines() ||
                targetLine < 0 || targetLine > textModel.getTotalLines()) {
            throw new IllegalArgumentException("line index out of bounds");
        }

        List<String> block = new ArrayList<>();
        for (int i = startLine; i <= endLine; i++) {
            block.add(textModel.getLine(startLine));
            textModel.deleteLine(startLine);
        }

        // 若目标在原块之后，因先删除块会缩短列表，需要将目标位置回退块长度
        int actualTarget = targetLine > startLine ? targetLine - (endLine - startLine + 1) : targetLine;

        for (int i = 0; i < block.size(); i++) {
            textModel.insertLine(actualTarget + i, block.get(i));
        }
    }

    /*
    列块移动（矩形区域选择）：
    提取[startLine,endLine]每行[startCol,endCol)的文本为列块，先从原位置删除，再插入到目标行/列。
     */
    public void moveColumnBlock(int startLine, int endLine, int startCol, int endCol, int targetLine, int targetCol) {
        if (startLine < 0 || endLine < startLine || targetLine < 0) {
            throw new IllegalArgumentException("line index out of bounds");
        }
        if (startCol < 0 || endCol < startCol || targetCol < 0) {
            throw new IllegalArgumentException("column index out of bounds");
        }
        // 提取列块内容
        List<String> columnBlock = new ArrayList<>();
        for (int i = startLine; i <= endLine; i++) {
            String line = textModel.getLine(i);
            if (line == null) line = ""; // 超界容错
            if (startCol < line.length()) {
                int actualEndCol = Math.min(endCol, line.length());
                String blockContent = line.substring(startCol, actualEndCol);
                columnBlock.add(blockContent);
            } else {
                columnBlock.add("");
            }
        }

        // 从原位置删除：对每一行移除对应列区间
        for (int i = startLine; i <= endLine; i++) {
            String line = textModel.getLine(i);
            if (line == null) line = "";
            if (startCol < line.length()) {
                int actualEndCol = Math.min(endCol, line.length());
                String newLine = line.substring(0, startCol) + line.substring(actualEndCol);
                textModel.updateLine(i, newLine);
            }
        }

        // 插入到目标位置：必要时补齐空行；列不足时以空格填充到目标列
        for (int i = 0; i < columnBlock.size(); i++) {
            int lineIndex = targetLine + i;
            // 若目标行不足，则补齐空行
            while (textModel.getTotalLines() <= lineIndex) {
                textModel.insertLine(textModel.getTotalLines(), "");
            }

            String line = textModel.getLine(lineIndex);
            if (line == null) line = "";
            if (targetCol > line.length()) {
                // 填充空格直到目标列
                line = String.format("%-" + targetCol + "s", line);
            }

            //新行 = 目标列前的文本 + 列块内容 + 目标列后的文本(如果存在)
            String newLine = line.substring(0, Math.min(targetCol, line.length())) + columnBlock.get(i) +
                    (targetCol < line.length() ? line.substring(targetCol) : "");
            textModel.updateLine(lineIndex, newLine);
        }
    }
}
