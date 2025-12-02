import java.io.*;

/*
 文件服务：负责将模型内容与磁盘文件进行读写同步。
 读取时按行拼接为单一字符串；写入时直接输出模型的整段文本。
 */

public class FileService {
    // 依赖的文本模型：I/O完成后与该模型同步
    private TextModel textModel;

    public FileService(TextModel model) {
        this.textModel = model;
    }

    //保存文件：将模型中的全文字符串写入到指定路径
    public boolean saveFile(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {//创建文件写入器
            writer.write(textModel.getContentAsString());//将文本模型的内容写入文件
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //打开文件：按行读取并以\n连接为全文，同步到模型
    public boolean openFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder sb = new StringBuilder();//用于拼接全文的字符串
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {//按行读取
                if (!first) sb.append("\n");//非首行前添加换行符
                sb.append(line);//按行拼接
                first = false;
            }

            // 同步到模型（空文件会在模型中保持一个空行）
            textModel.setContentFromString(sb.toString());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
