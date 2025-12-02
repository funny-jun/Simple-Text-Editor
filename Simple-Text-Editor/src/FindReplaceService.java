import java.util.ArrayList;
import java.util.List;

/*
查找/替换服务：在文本模型上执行字符串查找与替换。
查找逐行扫描；替换以整行为单位调用String.replace。
*/

public class FindReplaceService {

    private TextModel textModel;

    public FindReplaceService(TextModel model) {
        this.textModel = model;
    }

    //查找文本：返回所有匹配位置
    public List<TextPosition> findText(String searchText) {
        List<TextPosition> positions = new ArrayList<>();

        for (int i = 0; i < textModel.getTotalLines(); i++) {
            String line = textModel.getLine(i);
            int index = 0;
            while ((index = line.indexOf(searchText, index)) != -1) {//在line字符串中从index位置开始查找searchText
                positions.add(new TextPosition(i, index, searchText.length()));//i：当前行号; index：匹配的起始位置; searchText.length()：匹配文本的长度
                index += searchText.length();
            }
        }

        return positions;
    }

    //替换
    public int replaceService(String findText, String replaceText) {
        int replaceCount = 0;

        for (int i = 0; i < textModel.getTotalLines(); i++) {
            String line = textModel.getLine(i);
            if (line.contains(findText)) {
                String newLine = line.replace(findText, replaceText);//替换findText为replaceText
                textModel.updateLine(i, newLine);//更新文本行
                replaceCount++;
            }
        }
        return replaceCount;
    }
}

//位置描述：表示一次匹配结果的行、列与长度。
class TextPosition {
    private int line;
    private int column;
    private int length;

    public TextPosition(int line, int column, int length) {
        this.line = line;
        this.column = column;
        this.length = length;
    }

    //行索引
    public int getLine() {
        return line;
    }

    //列索引
    public int getColumn() {return column;}

    //匹配长度
    public int getLength() {
        return length;
    }
}
