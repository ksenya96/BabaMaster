import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.Locale;

public class Other {
    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.ENGLISH);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("D:\\share\\test_reg.txt")));
        PrintWriter writer1 = new PrintWriter(new FileOutputStream("D:\\share\\test_reg.ll"));

        String str;
        reader.readLine();
        while ((str = reader.readLine()) != null) {
            String[] words = str.split(",");
            String newStr = words[words.length - 1];
            for (int i = 0; i < words.length - 1; i++) {
                newStr += " " + (i + 1) + ":" + words[i];
            }
            writer1.println(newStr);
        }
        reader.close();
        writer1.close();
        /*PrintWriter writer1 = new PrintWriter(new FileOutputStream("D:\\share\\train_reg.txt"));
        PrintWriter writer2 = new PrintWriter(new FileOutputStream("D:\\share\\test_reg.txt"));
        float[] w = new float[24];
        for (int i = 0; i < w.length; i++) {
            w[i] = (float) Math.random();
        }
        writer1.println("1000000 23");
        for (int i = 0; i < 1000000; i++) {
            float label = w[23];
            float[] x = new float[24];
            for (int j = 0; j < w.length - 1; j++) {
                x[j] = (float) Math.random();
                writer1.format("%.2f,", x[j]);
            }
            for (int j = 0; j < w.length - 1; j++) {
                label += x[j] * w[j];
            }
            writer1.format("%.2f", label);
            writer1.println();
        }
        writer1.close();
        writer2.println("1000000 23");
        for (int i = 0; i < 1000000; i++) {
            float label = w[23];
            float[] x = new float[24];
            for (int j = 0; j < w.length - 1; j++) {
                x[j] = (float) Math.random();
                writer2.format("%.2f,", x[j]);
            }
            for (int j = 0; j < w.length - 1; j++) {
                label += x[j] * w[j];
            }
            writer2.format("%.2f", label);
            writer2.println();
        }
        writer2.close();*/

    }
}
