package chinesewhispers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class ChineseWhispers {

    public static int inc = 100;

    public static void main(String[] args) throws Exception, IOException {
        ChineseWhispers.start();
    }

    public static void start() throws Exception, IOException {
        Scanner userInput = new Scanner(System.in);
        String file_name;
        //String range; 
        String range = "-100 0,0 100";
        //  String range = "1000 1100,1100 1200";
        //String range = "9800 9900,9900 10000";
        boolean exists = true;
        int count, start = 0, end;
        Directory();
        System.out.println("================ ChineseWhispers Algorithm ================");
        System.out.println("Enter the filename for data set: Ex. elec.txt");
        file_name = "elec.txt";
        //User Interface for Input
        do {
            System.out.println("Enter value of Time Interval separated by comma(,) ");
            System.out.println("Ex. 20 50,51 100");
            //range = userInput.nextLine();

            /*auto generate*/
            String[] range2 = range.split("\\s*(=>|,|\\s)\\s*");
            for (int i = 0; i < range2.length; i++) {
                int k = Integer.parseInt(range2[i]);
                k += inc;
                range2[i] = Integer.toString(k);
                System.out.println(range2[i]);
            }
            range = Arrays.toString(range2);
            range = range.substring(1, range.length() - 1).replaceAll(",", "");

            boolean flag = false;
            for (int i = 0; i < range.length(); i++) {
                char c = range.charAt(i);
                System.out.println(flag);
                if (c == ' ' && flag == true) {
                    range = replaceCharAt(range, i, ',');
                    System.out.println("In loop: " + i);
                    break;
                }
                if (c == ' ') {
                    flag = true;
                    continue;
                }
            }
            System.out.println("Range: " + range);

            /*end of generate*/
            count = range.split(",").length;
            while (count != 0) {
                String[] interval = range.split(",")[start].split("\\s+");
                CW cw = new CW();
                exists = true;
                try {
                    Utils.readDynamicData(file_name + " " + interval[0] + " " + interval[1], range.split(",")[start]);
                } catch (Exception e) {
                    System.out.println("Given data set file does not exist.Try again");
                    exists = false;
                    break;
                }
                cw.readData("data" + range.split(",")[start]);
                cw.Builder_Edge_File();
                cw.ChineseWhispers();
                cw.Display_Result();
                start++;
                count--;
            }

            String[] compare = range.split(",");
            Utils.compareCluster(compare[0], compare[1]);
            inc += 100;
            ChineseWhispers.start();

        } while (!exists);
    }

    public static void Directory() throws IOException {
        System.out.println("LIST OF FILES IN DIRECTORY");
        File f = new File("./src/chinesewhispers/dataset"); // current directory

        File[] files = f.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.print("directory:");
            } else {
                System.out.print("     file:");
            }
            System.out.println(file.getCanonicalPath());
        }

    }

    private static String replaceCharAt(String s, int i, char c) {
        StringBuffer buf = new StringBuffer(s);
        buf.setCharAt(i, c);
        return buf.toString();
    }
}
