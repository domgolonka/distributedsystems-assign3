
import java.io.*;
import java.util.*;

public class Transaction {
    String directory;
    static int nextTid;
    private static String logfile = "tid.log";
    private static Transaction ts = null;


    public static synchronized Transaction getInstance(String dir) {
        if (ts == null) {
            ts = new Transaction(dir);
            ts.Start();
        }
        return ts;
    }
    /* SERVER */
    public static boolean commit(String directory, String filename, ArrayList<ResMessage> writes) {

        try {

            //check if the file exists, if it doesn't create it
            open(directory, filename);

            //get the file location
            RandomAccessFile file = new RandomAccessFile((directory + filename),"rw");

            //go to the end of the file
            file.seek(file.length());

            //write the strings to the end of the file
            for (int i = 0; i < writes.size(); i++) {

                file.writeBytes(writes.get(i).getData());
            }

            file.getFD().sync();
            file.close();

            return true;
        }

        //failed to write
        catch (Exception e) {
            return false;
        }


    }
    /* SERVER */
    static void open(String directory, String filename) {


        //try to open the file
        try {
            File file = new File(directory + filename);

            //if the file does not exist, create it
            if(!file.exists()) {
                file.createNewFile();
            }
        }

        catch (Exception e) {
            System.out.println("Error at FileIO.open: " + e + " " +  e.getMessage());
            System.exit(-1);
        }

        return;

    }
    public Vector<Integer> HandleCommit(int tid, int num) throws IOException {
        String filename = new String();
        String txtfile = tid + ".txn.txt";
        File fileFromTxn = new File(directory, txtfile);
        FileInputStream inFromClient = new FileInputStream(fileFromTxn);
        int ch;
        System.out.println("NUM IS " + num);
        int cstate = inFromClient.read();
        num++;
        //System.out.println("TESTING HERE1: " + cstate);
        if (cstate == 0)
        {
            //System.out.println("TESTING HERE2: " + cstate);
            while ((ch = inFromClient.read()) != -1) {
                filename += (char) ch;
            }
            inFromClient.close();
            ArrayList<byte[]> content = new ArrayList<byte[]>();
            Vector<Integer> vi = new Vector<Integer>();
            for (int i = 0; i < num; i++)
            {
                String writefile = new String();
                writefile += tid + "_" + i + ".write.txt";
                File pFile = new File(directory, writefile);
                if (pFile.exists() == false) {
                    vi.add(i);
                    return vi;
                }
                FileInputStream pin = new FileInputStream(pFile);
                content.add(readContent(pin));
                pin.close();
            }
            File dest = new File(directory, filename);
            if (dest.exists() == false) {
                dest.createNewFile();
            }
            boolean append = true;
            FileOutputStream dout = new FileOutputStream(dest, append);
            for (int i = 0; i < content.size(); i++) {
                //System.out.println("Printing: " +content.get(i).toString());
                dout.write(content.get(i));
            }
            dout.flush();
            dout.close();
            FileOutputStream outToTxn = new FileOutputStream(fileFromTxn);
            outToTxn.write(1);
            outToTxn.flush();
            outToTxn.close();
            updateTID(logfile);

        } else if (cstate == 1)
        {
            return new Vector<Integer>();

        } else if (cstate == 2)
        {
            Vector<Integer> ve = new Vector<Integer>();
            ve.add(-1);
            return ve;
        } else {
            return new Vector<Integer>();
        }
        return new Vector<Integer>();
    }
    public int HandleWrite(String data, int tid, int seq) throws IOException {
        String txtfile = tid + ".txn.txt";
        File txnFile = new File(directory, txtfile);
        FileInputStream inFromClient = new FileInputStream(txnFile);

        int cstate = inFromClient.read();
        //System.out.println("TESTING HERE: " + cstate);
        inFromClient.close();
        if (cstate != 0) {
            return -1;
        }
        String writefile = tid + "_" + seq + ".write.txt";
        File f = new File(directory, writefile);
        f.createNewFile();
        FileOutputStream outToClient = new FileOutputStream(f);

        outToClient.write(data.getBytes("US-ASCII"));
        outToClient.flush();
        outToClient.close();
        return 0;
    }




    public void HandleAbort(int tid, int num) throws IOException {

        String txtfile = tid + ".txn.txt";
        File txnFile = new File(directory, txtfile);
        FileInputStream inFromClient = new FileInputStream(txnFile);

        int cstate = inFromClient.read();
        inFromClient.close();
        if (cstate == 2) {
            return;
        } else {
            for (int i = 1; i < num; i++)
            {
                String writefile = new String();
                writefile += tid + "_" + i + ".write.txt";
                File pFile = new File(directory, writefile);
                if (pFile.exists() == false) {
                    continue;
                } else {
                    pFile.delete();
                }
                FileOutputStream outToClient = new FileOutputStream(txnFile);
                cstate = 2;
                outToClient.write(2);
                outToClient.flush();
                outToClient.close();
            }
        }
    }

    private byte[] readContent(FileInputStream inFromClient) {
        int len;
        try {
            len = inFromClient.available();
            byte[] a = new byte[len];
            inFromClient.read(a);
            return a;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    private void Start() {

        File f = new File(directory, logfile);
        if (f.exists())
        {
            try {
                FileInputStream inFromFile = new FileInputStream(f);
                String startingtid = new String();
                int a;
                while ((a = inFromFile.read()) != -1) {
                    startingtid += (char) a;
                }
                nextTid = Integer.parseInt(startingtid);
                nextTid++;
                inFromFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            try {
                f.createNewFile();
                FileOutputStream outToFile = new FileOutputStream(f);
                nextTid = 0;
                outToFile.write('0');
                outToFile.flush();
                outToFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private Transaction(String directory) {
        this.directory = directory;
    }
    public String getDirectory() {
            return this.directory;
    }


    public void HandleNewTxn(String filename, int tid) throws IOException {
        String txnfile = tid + ".txn.txt";

        File f = new File(directory, txnfile);

        f.createNewFile();
        FileOutputStream outToClient = new FileOutputStream(f);
        outToClient.write(0);
        outToClient.write(filename.getBytes("US-ASCII"));

        outToClient.flush();
        outToClient.close();

    }



    public int getNextTid() {
        return nextTid++;
    }
    public boolean checkFile(String filepath) {
        File f = new File(directory, filepath);
        if(f.exists() && !f.isDirectory()) {
            return true;
        }
        return false;
    }
    public boolean checkTid(int tid) {
        String txtfile =  tid + ".txn.txt";
        File txnFile = new File(directory, txtfile);
        return txnFile.exists();

    }
    public void exit() {
        File f = new File(Server.getServerDirectory());
        if (!f.exists())
            return;

        deleteDirectory(f);
        //File[] fileList = f.listFiles();
        //String txn = "txn.txt";
        //String write = "write.txt";
        //String tidlog = "log";
        //String serverlog = "serverlog.txt";
        /*for (int i = 0; i < fileList.length; i++) {
            String filename = fileList[i].getName();
            String extension = filename.substring(filename.indexOf(".") +1, filename.length());
            System.out.println(extension);
            if (write.equalsIgnoreCase(extension) || txn.equalsIgnoreCase(extension) || tidlog.equalsIgnoreCase(extension)   || filename.equalsIgnoreCase(serverlog)) {
                //System.out.println("Deleting: " + fileList[i].toString());
                fileList[i].setWritable(true);
                fileList[i].delete();
            }
        }*/
    }
    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }
    public void updateTID(String sfile) {
        //System.out.println("Updating log file " + sfile + " TID: " +tid);
        try {
            File file = new File(directory, sfile);
            System.out.println("Deleting log file " + file.toString());
            file.setWritable(true);
            file.delete();
            File newFile = new File(directory,sfile);
            newFile.createNewFile();
            RandomAccessFile nfile = new RandomAccessFile(directory+File.separator + sfile, "rw");


            nfile.writeBytes(Integer.toString(nextTid));
            nfile.getFD().sync();
            nfile.close();

        }

        //failed to write
        catch (Exception e) {
            e.printStackTrace();
            //System.exit(-1);
        }


    }

}