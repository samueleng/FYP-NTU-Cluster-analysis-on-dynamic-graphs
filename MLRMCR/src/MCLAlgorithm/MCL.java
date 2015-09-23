package MCLAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class MCL {

    public static void main(String[] args) throws IOException {
        MCL mcl = new MCL();
        mcl.start();
    }

    public void start() throws IOException {
        Scanner userInput = new Scanner(System.in);
        String input, range;
        boolean exists = true;
        int count, start = 0;
        //User Interface for Input
        System.out.println("***** MCL ALGORITHM FULL *****");
        do {
            Directory();
            System.out.println("Enter the filename for data set:Ex. elec.txt");
            input = userInput.nextLine();
            System.out.println("Enter value of Time Interval separated by comma(,) ");
            System.out.println("Ex. 20 50,51 100");
            range = userInput.nextLine();

            count = range.split(",").length;

            while (count != 0) {
                String[] interval = range.split(",")[start].split("\\s+");

                MCLApp mcl = new MCLApp();
                try {
                    Utils.readDynamicData(input + " " + interval[0] + " " + interval[1], range.split(",")[start]);
                } catch (Exception e) {
                    System.out.println("Given data set file does not exist.Try again");
                    exists = false;
                    break;
                }
                exists=true;
                mcl.readData("data" + range.split(",")[start]);
//            mcl.readData(input);
                mcl.addSelfLoop();
                mcl.constructTransitionMatrix();
                mcl.mcl();
                start++;
                count--;
                
            }
        } while (!exists);
        String t1, t2;
        String choice;
        start = 0;
        do {
            do {
                System.out.println("Enter Time instance t1 and t2 to compare Cluster Results file");
                System.out.println("Enter t1: (Ex. t1:" + range.split(",")[start] + ")");
                t1 = userInput.nextLine();
                System.out.println("Enter t2: (Ex. t2:" + range.split(",")[start] + ")");
                t2 = userInput.nextLine();
                exists=true;
                if (!Utils.fileExists(t1) || !Utils.fileExists(t2)) {
                    System.out.println("Result files for given time instances does not exists, Please try again");
                    exists = false;
                }
            } while (!exists);
            Utils.compareCluster(t1, t2);
            System.out.println("Press 0 to Cluster Results of different time instance or any other key to exit");
            choice = userInput.nextLine();
        } while ("0".equals(choice));
    }
    public static void Directory() throws IOException{  
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

}
