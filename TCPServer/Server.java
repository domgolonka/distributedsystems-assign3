


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;

public class Server {

    final static String serverCode = "BackupServer";
    static String serverDirectory ="";
    static String otherServerDirectory = "";
    String otherdirectory = null, selfdirectory = null;
    final static String servDirExtension ="backup";

    static Semaphore transfer = new Semaphore(1, true);

    final int backlog = 50;
    public Server(InetAddress host, int clientPort, int serverPort, String directory, String primaryLocation) {
        Thread thread;
        String connectionInfo = "";
        String primaryHost = "";
        checkDirectoryExists(directory);
        int retry = 0;
        UserCommands input = new UserCommands(directory);
        thread = new Thread(input);
        thread.start();


        // make server directory
        serverDirectory = directory + getUniqueID() + "." +servDirExtension;
        File dir = new File(serverDirectory);
        dir.mkdir();

        while (retry < 4) {
            try {
                connectionInfo = TCPServer.readPrimaryFile(primaryLocation);
            }
            catch (FileNotFoundException e) {

                System.out.println("Error: Primary.txt not found. Error " + e.getMessage());
                System.exit(-1);
            }

            StringTokenizer token = new StringTokenizer(connectionInfo);
            primaryHost = token.nextToken(" ").trim();
            token.nextToken(" ").trim(); // port
            if (token.hasMoreTokens()) {
                 otherdirectory = token.nextToken(" ").trim();
            }
            if (token.hasMoreTokens()) {
                selfdirectory = token.nextToken(" ").trim();
            }
            try {
                // try to connect to backup
                while (true) {
                    //System.out.println("Trying to connect to primary: " +primaryHost + " Port: " +serverPort);

                    Socket connectionSocket = new Socket(primaryHost, serverPort);
                    connectionSocket.setKeepAlive(true);
                    //System.out.println("We have successfully connected to the primary at " +primaryHost + " Port: " +serverPort);
                    retry = 0;
                    //System.out.println("Other directory " + otherdirectory + " Self directory: " + serverDirectory);
;
                    if (selfdirectory != null && !selfdirectory.isEmpty()) {
                        File f = new File(selfdirectory);
                        File olddirectory = new File(serverDirectory);
                        //System.out.println("directory exists test " + f.exists() + " " + f.toString());
                        //System.out.println("Other directory " + otherdirectory + " Self directory: " + selfdirectory + " ServerDirectory  " + serverDirectory + " Other Directory " + otherServerDirectory);
                        if ((otherdirectory != null && !otherdirectory.isEmpty()) && (selfdirectory != null && !selfdirectory.isEmpty()) && !(selfdirectory.equals(serverDirectory)) && f.exists()) {
                            //System.out.println("Other directory " + otherdirectory + " Self directory: " + serverDirectory);
                            Transaction.deleteDirectory(olddirectory);
                            serverDirectory = selfdirectory;
                            otherServerDirectory = otherdirectory;

                        } else if ((otherdirectory != null && !otherdirectory.isEmpty()) && !(otherdirectory.equals(serverDirectory)) && f.exists()) {

                            otherServerDirectory = otherdirectory;

                        }
                    }

                    ObjectOutputStream oos = new ObjectOutputStream(connectionSocket.getOutputStream());
                    ObjectInputStream ois = new ObjectInputStream(connectionSocket.getInputStream());
                    oos.writeObject(Server.serverCode);
                    oos.writeObject(Server.serverDirectory);



                    //gets the other server's directory
                    otherServerDirectory = (String) ois.readObject();
                    backup(otherServerDirectory, serverDirectory);

                    oos.close();
                    ois.close();
                    connectionSocket.close();

                }
            }

            catch (Exception e) {

                if (retry > 2) {
                    System.out.println("I cannot connect to the primary server. I will become the primary. The primary.txt file has been updated to reflect these changes.");
                    retry++;
                    /*if (otherdirectory != null && !otherdirectory.isEmpty()) {
                        File f = new File(otherdirectory);
                        System.out.println("Other directory " + otherdirectory + " Self directory: " + selfdirectory);
                        if ((otherdirectory != null && !otherdirectory.isEmpty()) && (selfdirectory != null && !selfdirectory.isEmpty()) && !(otherdirectory.equals(serverDirectory)) && f.exists()) {
                            System.out.println("Other directory " + otherdirectory + " Self directory: " + serverDirectory);
                            Transaction.deleteDirectory(f);
                            serverDirectory = selfdirectory;
                            otherServerDirectory = otherdirectory;

                        } else if ((otherdirectory != null && !otherdirectory.isEmpty()) && !(otherdirectory.equals(serverDirectory)) && f.exists()) {

                            otherServerDirectory = otherdirectory;

                        }
                    }*/

                    TCPServer.upPrimaryFile(primaryLocation, host, serverPort, serverDirectory, otherServerDirectory);

                    //set up thread to talk to secondary
                    ServerCommunication serverCommunication = new ServerCommunication(host, serverPort);
                    thread = new Thread(serverCommunication);
                    thread.start();
                }
                else {
                    retry++;
                }

            }
        }

        try {

            ServerSocket serverSocket = new ServerSocket(clientPort, backlog, host);
            Message.Start();
            System.out.println("The server directory is: "+ serverDirectory);
            Transaction.getInstance(serverDirectory);

            while(true) {
                Socket transactionSocket = serverSocket.accept();
                Communication cm = new Communication(transactionSocket);
                thread = new Thread(cm);
                thread.start();
            }
        }
        catch (Exception e) {
            System.out.println("Error at Server.Server: " + e + ". " + e.getMessage());
            System.exit(-1);
        }
    }

    public static String getServerDirectory() {
        return serverDirectory;
    }
    public static int getUniqueID() {;

        int returnValue = 0;
        Random rand = new Random();
        returnValue = rand.nextInt(1000);

        return returnValue;
    }


    public static void checkDirectoryExists(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            if(!dir.mkdirs()) {
                System.out.println("Error creating directories.");
                System.exit(-1);
            }
        }

    }

    public void backup (String source, String dest) {
        File fsource = new File(source);
        File fdest = new File(dest);
        try {
            copyDirectory(fsource, fdest);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void copyDirectory(File sourceLocation , File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }


final class UserCommands implements Runnable {

    String directory;

    public UserCommands(String directory) {
        this.directory = directory;
    }
    public void run() {

        while (true) {
            Scanner scan = new Scanner(System.in);
            String input = scan.nextLine();
            if (input.compareTo("quit") == 0) {
                TCPServer.exit();

            }
            else {
                System.out.println("The command was not recognized. There is only one command to quit: quit");
            }
        }

    }
}



}
