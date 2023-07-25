package util;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class OutputHandler {
    private static final OutputHandler outputHandler = new OutputHandler();
    private static ArrayList<String> riscvStringList = new ArrayList<>();
    private static ArrayList<String> llvmStringList = new ArrayList<>();

    private OutputHandler() {
    }

    public static OutputHandler getInstance() {
        return outputHandler;
    }

    public static FileOutputStream getOutputFile(String fileName) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileOutputStream;
    }

    public static void closeOutputFile(FileOutputStream fileOutputStream) {
        try {
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void output2Stream(StringBuilder str, OutputStream outputStream) {
        try {
            outputStream.write(str.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void output2Stream(String str, OutputStream outputStream) {
        try {
            outputStream.write(str.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addRiscvString(String str) {
        riscvStringList.add(str);
        riscvStringList.add("\n");
    }

    public static void addLlvmString(String str) {
        llvmStringList.add(str);
        llvmStringList.add("\n");
    }

    public static void outputRiscvString(OutputStream outputStream) {
        for (String str : riscvStringList) {
            output2Stream(str, outputStream);
        }
    }

    public static void outputLlvmString(OutputStream outputStream) {
        for (String str : llvmStringList) {
            output2Stream(str, outputStream);
        }
    }

    public static void clearRiscvString() {
        riscvStringList.clear();
    }

    public static void clearLlvmString() {
        llvmStringList.clear();
    }

    public static void main(String[] args) {
        FileOutputStream fileOutputStream = OutputHandler.getOutputFile("test2.txt");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hello world");
        String str = "hello world";
        OutputHandler.output2Stream(str, fileOutputStream);

        OutputHandler.addLlvmString(str);
        OutputHandler.addLlvmString(str);
        OutputHandler.addLlvmString(str);

        OutputHandler.outputLlvmString(fileOutputStream);
        OutputHandler.clearLlvmString();
        OutputHandler.outputLlvmString(fileOutputStream);

        OutputHandler.addRiscvString(str);
        OutputHandler.addRiscvString(str);
        OutputHandler.addRiscvString(str);

        OutputHandler.outputRiscvString(fileOutputStream);
        OutputHandler.clearRiscvString();
        OutputHandler.outputRiscvString(fileOutputStream);

        OutputHandler.closeOutputFile(fileOutputStream);
    }
}
