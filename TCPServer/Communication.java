
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Communication implements Runnable {
    OutputStream outToClient;
    InputStream inFromClient;
    PrintWriter out;
    //BufferedReader br;
    public Communication(Socket socket) {
                 Socket connectionSocket = socket;
            try {
                    inFromClient = connectionSocket.getInputStream();

                    outToClient = connectionSocket.getOutputStream();
                    //br = new BufferedReader(new InputStreamReader(inFromClient));
                    out = new PrintWriter(connectionSocket.getOutputStream(), true);
                    //this.start();
            }
            catch (IOException e) {
                System.out.println("There is an issue with the server: " + e.getMessage());
            }
        }

    public void sendMsg(ReqMessage msg){
             System.out.println(msg.print());
        try {

            out.write(msg.print());
            out.close();
        } catch (Exception e) {
            System.out.println("There was an issue with send Message" + e.getMessage());
        }
    }
    private void ProcessRequest(InputStream input)throws IOException{
        try{
            String method = new String();
            String startTid = new String();
            String sequence = new String();
            String conlen = new String();
            String data = new String();
            int tid, seq, len;
            byte bytez;
            //System.out.println("Method");
            while(!isSpaceSeq(bytez=(byte)input.read()))
            {
                method+=(char)bytez; // uppercase or lower case
            }
            //System.out.println("startid");
            while(!isSpaceSeq(bytez=(byte)input.read()))
            {
                startTid+=(char)bytez;
            }
            //System.out.println("sequence");
            tid=Integer.parseInt(startTid);
            while(!isSpaceSeq(bytez=(byte)input.read()))
            {
                sequence+=(char)bytez;
            }
            //System.out.println("conlen");
            seq=Integer.parseInt(sequence);
            while(!isSpaceSeq(bytez=(byte)input.read()))
            {
                conlen+=(char)bytez;
            }
            //System.out.println("len checker");
            len=Integer.parseInt(conlen);
            if (len > 0) {
                try {
                    input.skip(3);
                    for (int i = 0; i < len; i++) {

                        //append the next character to the data string
                        data = data + (char)input.read();

                    }
                    //input.skip(4);
                }

                //catch the exception
                catch (Exception e) {
                    System.out.println("There was an error parsing the TCPCLient input: " + e + " " + e.getMessage());
                }
            }
            System.out.println("Method " + method + " Data" + data + "tid" + tid +" seq" + seq);
            Message.ResponseMessage(this, method, tid, seq, len, data);
            input.close();
        }
        catch(NumberFormatException ne)
        {
            ReqMessage emsg=new ReqMessage("ERROR", -1,-1,"203", "Wrong message Format");
            this.sendMsg(emsg);

        }


    }
    private boolean isSpaceSeq(byte bytez)
    {
        if(bytez==32||bytez==10||bytez==13)
            return true;
        else
            return false;
    }
    public void run() {
        while(true) {
            try {
                ProcessRequest(inFromClient);
            }catch(IOException e)
            {
                //e.printStackTrace();
                //System.out.println("There was an error with processing the request at Communication.Run " + e.getMessage());
                //return;
            }
            try {
                //close everything
                inFromClient.close();
                outToClient.close();
                out.close();

            }

            catch (SocketException e) {
                System.out.println("Client has closed socket, error message not sent.");
            }

            catch (Exception e) {
                System.out.println("Error at Transaction.run(2): " + e + " " + e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }

        }
    }

}
final class ServerCommunication implements Runnable {

    InetAddress host;
    int serverPort;
    int backlog = 100;

    public ServerCommunication(InetAddress host, int serverPort) {

        this.host = host;
        this.serverPort = serverPort;
    }

    public void run() {
        try {

            //creates a new port to communicate with backup
            ServerSocket serverSocket = new ServerSocket(serverPort, backlog, host);
            System.out.println("Starting to listen for backup server communication on " + host + " PORT: " +serverPort);
            while (true) {
                Socket backupsocket = serverSocket.accept();
                BackupCommunication secondaryTalk = new BackupCommunication(backupsocket);
                Thread talk = new Thread(secondaryTalk);
                talk.start();
            }
        }

        catch (Exception e) {
            System.out.println("There was an error communicating with the Backup Server. Error:" + e + " ErrorMessage:" + e.getMessage());
            System.exit(-1);
        }
    }
}
final class BackupCommunication implements Runnable {

    Socket backupsocket;
    public BackupCommunication(Socket socket) {

        this.backupsocket = socket;
    }

    public void run() {

        ObjectOutputStream oos;
        ObjectInputStream ois;
        String backupdirectory;

        try {
            backupsocket.setKeepAlive(true);

            ois = new ObjectInputStream(backupsocket.getInputStream());
            oos = new ObjectOutputStream(backupsocket.getOutputStream());

            String secondarySignal = (String)ois.readObject();

            if(secondarySignal.compareTo(Server.serverCode) == 0) {


                try {

                    backupdirectory = (String) ois.readObject();
                    oos.writeObject(Server.getServerDirectory());
                    //System.out.println("Sending our directory to Backup Server. Receiving backup directory. It is " + backupdirectory);
                }
                catch (Exception e) {
                    System.out.println("Error at communication outgoing with backup server: " + e + " " + e.getMessage());
                }
                oos.flush();

                //System.out.println("The backup server has been given previous commits transactions and the next commit Transaction ID is: " + Transaction.nextTid);

            }
            oos.close();
            backupsocket.close();

        }
        catch (Exception e) {
            System.out.println("Error at communicating with backup server: " + e + " " + e.getMessage());

            Server.transfer.release();
        }

        finally {

            backupsocket = null;

        }
    }


}
