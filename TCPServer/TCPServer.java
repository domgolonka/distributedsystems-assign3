

import java.io.*;
import java.net.*;


public class TCPServer  {
    static final int TIMEOUT = 5000;
    static int COMMAND_LINE = 5;
    final static String primaryFile = "primary.txt";
    //static String config;
    static InetAddress IP;
    static String directory, primarydirectory;
    static int cport, sport;

    public static void main(String args[]) {
        TCPServer tcp = new TCPServer();
        try {
            tcp.ConfigServer(args);
            //tcp.StartServer();
        }
        catch (IOException e) {
            System.out.println("There was an error with the configuration " + e.getMessage());
        }
    }
   /* public void StartServer() throws IOException{
        ServerSocket welcomeSocket = new ServerSocket(port, 0, IP);
        System.out.println("The server is running on: IP " + IP + " PORT: " + port);
        while (true) {
            Socket connectionSocket;
            try {
                connectionSocket = welcomeSocket.accept();
                Communication req = new Communication(connectionSocket);
            } catch (IOException e) {
                System.out.println("There was an error with the configuration " + e.getMessage());
            }
        }
    }
*/
    private static void ConfigServer(String args[]) throws IOException {
        // IP CLIENT_PORT SERVER_PORT DIRECTORY primary.txt
        if (args.length == 0) {
            System.out.println("You must enter the format [IP] [Client_port] [Server_port] [File Directory] [/dir/dir/primary.txt]");
            System.exit(1);
        }
        if (args.length != COMMAND_LINE) {
            String parts[] = new String[6];
            parts[0] = "127.0.0.1";
            parts[1] = "8081"; // server default port
            parts[2] = "8080"; // client default port
            if ((args[0].charAt(0) != '/')) {
                System.out.println("Invalid file directory. Your directory must start with a /");
                System.exit(-1);
            }
            if ((args[1].charAt(0) != '/')) {
                System.out.println("Invalid primary.txt directory. Your directory must start with a /");
                System.exit(-1);
            }
            parts[3] = args[0]; // file directory
            parts[4] = args[1];  // primary.txt directory
            ReadConfig(parts);

        } else if (args.length == COMMAND_LINE) {
            ReadConfig(args);
        } else {
            System.out.println("The command is invalid.");
            System.exit(-1);
        }

    }

    private static void ReadConfig(String[] args)  throws IOException {
        // IP CLIENT_PORT SERVER_PORT DIRECTORY primary.txt
        IP = GetIPAddress(args[0]);
        cport = Integer.parseInt(args[1]);
        sport = Integer.parseInt(args[2]);
        if ((cport < 1024 || cport > 65535)) {
            System.out.println("Invalid Client port " + cport);
            System.exit(-1);
        }
        if ( (sport < 1024 || sport > 65535)){
            System.out.println("Invalid Server port " + sport);
            System.exit(-1);
        }
        if ((args[3].charAt(0) != '/')) {
            System.out.println("Invalid file directory. Your directory must start with a /");
            System.exit(-1);
        }
        if ((args[4].charAt(0) != '/')) {
            System.out.println("Invalid primary.txt. Your directory must start with a /");
            System.exit(-1);
        }
        directory = args[3];
        primarydirectory = args[4];

        //Transaction fm = Transaction.getInstance(directory);
        //fm.exit();
        new Server(IP, cport, sport, directory, primarydirectory);
    }

        private static InetAddress GetIPAddress (String IP) throws IOException {
        InetAddress inetAddress = null;
        String[] StrArr = IP.split("\\.");
        if (StrArr.length != 4) {
            System.out.println("Unknown IP address: " + IP);
            return null;
        }
        else {
            byte[] ByteAddr = {(byte)Integer.parseInt(StrArr[0]), (byte)Integer.parseInt(StrArr[1]),
                    (byte)Integer.parseInt(StrArr[2]), (byte)Integer.parseInt(StrArr[3])};
            try {
                inetAddress = InetAddress.getByAddress(ByteAddr);
                if (!inetAddress.isReachable(TIMEOUT)) {
                    System.out.println("Unknown IP address: " + IP);
                    return null;
                }
            } catch (UnknownHostException uhe) {
                System.out.println("Unknown IP address: " + IP);
                return null;
            } catch (IOException ioe) {
                System.out.println("I/O Error: " + ioe.getMessage());
                return null;
            }
        }

        return inetAddress;
    }
    public static void exit()
    {
        System.out.println("You have shutdown the server.");
        Transaction ts =Transaction.getInstance(null);
        ts.exit();
        System.exit(0);
    }
    static String readPrimaryFile(String location) throws FileNotFoundException {

        BufferedReader br = null;
        String line = "";
        String fString = "";

        try {
            File file = new File(location);

            if (file.getName().compareTo(primaryFile) != 0) {

                System.out.println("Error:  File indicated is not the primary.txt file. Your file is: "+ file.getName().toString());
                System.exit(-1);
            }

            else if (!file.exists()) {

                System.out.println("Error:  primary.txt does not exist");
                System.exit(-1);
            }

            br = new BufferedReader(new FileReader(file));

            while ((line = br.readLine()) != null) {
                fString = fString + line + "\r\n";

            }
        }
        catch (FileNotFoundException e) {
            throw e;
        }
        catch (Exception e) {
            System.out.println("There was an error at readPrimary : " + e + " " + e.getMessage());
            System.exit(-1);
        }

        finally {
            try {
                if (br != null)
                    br.close();
            }
            catch (Exception e) {
                System.out.println("There was an error at closing readPrimary  " + e + " " + e.getMessage());
                System.exit(-1);
            }
        }
        fString = fString.replaceFirst("^/", "");

        return fString;
    }
    public static boolean upPrimaryFile(String primaryLocation, InetAddress host, int port, String primaryd, String otherd) {
        try {
            File file = new File(primaryLocation);

            if (file.getName().compareTo(primaryFile) != 0) {

                System.out.println("Error:  File indicated is not the primary.txt file.");
                System.exit(-1);
            }

            else if (!file.exists()) {
                System.out.println("Error:  primary.txt does not exist");
                System.exit(-1);
            }

            file.delete();
            File newFile = new File(primaryLocation);
            newFile.createNewFile();
            RandomAccessFile primary = new RandomAccessFile(primaryLocation, "rw");
            if (otherd != null && !otherd.isEmpty())  {
                primary.writeBytes(host + " " + port + " " + primaryd + " " + otherd);
            }
            else if (primaryd != null && !primaryd.isEmpty()) {
                primary.writeBytes(host + " " + port + " " + primaryd );

            }
            else {
                primary.writeBytes(host + " " + port);
            }

            primary.getFD().sync();
            primary.close();

            return true;
        }

        catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return false;
        }


    }

}