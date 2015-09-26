package MCLAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

public class MCL {

    public static int inc;

    public static String startEnd = "";
    public static String[] startEnd2;
    public static String start;
    public static String end;
    public static int parseStart;
    public static int parseEnd;
    public static String range;
    public static String finalRange;
    public static String file_name;
    public static boolean exists = true;
    public static boolean check = false;
    public static boolean check2 = false;
    public static boolean checkCompare = false;
    public static int count;
    public static String[] track = new String[2];

    public static void main(String[] args) throws IOException {

        UIStart();
        MCL.start();

    }

    public static void start() throws IOException {
        Scanner userInput = new Scanner(System.in);

        do {
            /*auto generate*/
            count = range.split(",").length;
            String[] compare1 = range.split(",");

            while (count != 0) {
                /* DO FIRST INTERVAL*/
                String[] interval = range.split(",")[0].split("\\s+"); //0 100  
                String[] interval2 = range.split(",")[1].split("\\s+"); //100 200
                MCLApp mcl = new MCLApp();
                try {
                    Utils.readDynamicData(file_name + " " + interval[0] + " " + interval[1], range.split(",")[0]);
                    System.out.println(interval[0] + " and " + interval[1]);

                } catch (Exception e) {
                    System.out.println("Given data set file does not exist.Try again");
                    exists = false;
                    break;
                }
                exists = true;

                if (check == false || !(track[0].equals(interval[0]) && track[1].equals(interval[1]))) {
                    mcl.readData("data" + range.split(",")[0]);
                    mcl.addSelfLoop();
                    mcl.constructTransitionMatrix();
                    mcl.mcl();
                    count--;
                    System.out.println("Done with  : " + interval[0] + " " + "and " + interval[1]);
                    System.out.println("***************************************************************************");
                } else {
                    checkCompare = true;
                }

                /* DO SECOND INTERVAL*/
                MCLApp mcl2 = new MCLApp();
                System.out.println("SECOND HALF: " + range.split(",")[1]); //100 200  
                if (Integer.parseInt(interval2[0]) == parseEnd) {
                    break;
                }
                if (Integer.parseInt(interval2[1]) > parseEnd) {
                    break;
                }
                Utils.readDynamicData(file_name + " " + interval2[0] + " " + interval2[1], range.split(",")[1]);
                mcl2.readData("data" + range.split(",")[1]);
                mcl2.addSelfLoop();
                mcl2.constructTransitionMatrix();
                mcl2.mcl();
                System.out.println("Done with  : " + interval2[0] + " " + "and " + interval2[1]);
                System.out.println("***************************************************************************");

                //now track interval2[0] and interval2[1]  
                String value = String.valueOf(interval2[0]);
                track[0] = value;
                String value2 = String.valueOf(interval2[1]);
                track[1] = value2;

                if (checkCompare == true) {
                    System.out.println("No more generated files to compare");
                }

                //COMPARE CLUSTERS
                Utils.compareCluster(compare1[0], compare1[1]);
                System.out.println("***************JUST FINISH COMPARE*******************");

                //increment inc should be 100 200,200 300 <== increment 
                String[] range2 = range.split("\\s*(=>|,|\\s)\\s*");
                for (int i = 0; i < range2.length; i++) {
                    int k = Integer.parseInt(range2[i]);
                    k += inc;
                    range2[i] = Integer.toString(k);
                }
                range = Arrays.toString(range2);
                range = range.substring(1, range.length() - 1).replaceAll(",", "");

                boolean flag = false;
                for (int i = 0; i < range.length(); i++) {
                    char c = range.charAt(i);
                    if (c == ' ' && flag == true) {
                        range = replaceCharAt(range, i, ',');
                        break;
                    }
                    if (c == ' ') {
                        flag = true;
                    }
                }
                System.out.println("*******************************FINISH MODIFYING RANGE****************************");

                System.out.println("RANGE NOW: " + range);

                //compare time steps
                parseStart = parseStart + inc;
                check = true;
                if (parseStart > parseEnd) {
                    System.out.println("parseStart: " + parseStart + " parseEnd: " + parseEnd);
                    System.exit(1);
                }
                System.out.println("*******************************Going to call start()****************************");
                MCL.start();
            }
        } while (!exists);
    }

    public static void Directory() throws IOException {
        System.out.println("LIST OF FILES IN DIRECTORY");
        File f = new File("./src/MCLAlgorithm/dataset"); // current directory

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

    public static void UIStart() {
        Scanner userInput = new Scanner(System.in);
        //User Interface for Input
        System.out.println("***** MCL ALGORITHM FULL *****");
        System.out.println("Enter the filename for data set: Ex : elec.txt, munmun.txt, slashdot.txt");
        file_name = userInput.nextLine();

        System.out.println("Enter a time step (etc: 100) : ");
        inc = userInput.nextInt();
        userInput.nextLine();

        System.out.println("Enter a range {start, end} (0,200): "); //(0,200)
        startEnd = userInput.nextLine();     //startEnd = 0,200

        startEnd2 = startEnd.split("\\s*(=>|,|\\s)\\s*"); //startEnd2 = [0] [200]
        start = startEnd2[0]; //start = 0 
        end = startEnd2[1];  //end = 200
        parseStart = Integer.parseInt(start); //parseStart = 0
        parseEnd = Integer.parseInt(end); //parseEnd = 200
        int newVal = Integer.parseInt(start) + inc;
        range = start + " " + String.valueOf(newVal) + "," + String.valueOf(newVal) + " " + String.valueOf(newVal + inc);  //range = 0 100,100 200
        System.out.println(range);
    }

}
