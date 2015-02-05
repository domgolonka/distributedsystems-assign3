
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class ResMessage extends Message {
    Transaction ts;
    Communication cm;
    private String method;
    private int tid;
    private int seqnumber;
    private int conlen;
    private String data;

    public ResMessage(Communication cm, String method, int tid, int seqnumber, int conlen, String data)  {
       this.method=method;
       this.tid=tid;
       this.seqnumber=seqnumber;
       this.conlen=conlen;
       this.data=data;
       ts=Transaction.getInstance(null);
       this.cm = cm;
    }
    public void Read(ResMessage message ) throws IOException{
        String filename=message.getData();
        String directory = ts.getDirectory();
        String fullfilename = directory + "/" + filename;
        if(ts.checkFile(filename)==false)
        {
            String errcode="205";
            ReqMessage errmsg = new ReqMessage("ERROR",tid,seqnumber,errcode, data);
            cm.sendMsg(errmsg);
            return;
        }
        BufferedReader br = new BufferedReader(new FileReader(fullfilename));
        try {

            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            String getdata = sb.toString();
            ReqMessage ack=new ReqMessage("ACK", message.getTid(), message.getSeqNumber(), "0", getdata);
            cm.sendMsg(ack);
        }
        finally {
            br.close();
        }


    }
    public void NewTxn(ResMessage message) throws IOException{
        int newtid=ts.getNextTid();
        String filename=message.getData();

        ts.HandleNewTxn(filename, newtid);
        System.out.println("New_TXN " + newtid + " " + message.getSeqNumber() + " " + filename);
        ReqMessage ack=new ReqMessage("ACK", newtid, message.getSeqNumber(), "0", filename);
        cm.sendMsg(ack);
    }
    public void Write(ResMessage message) throws IOException{
        int tid=message.getTid();
        String data=message.getData();
        int seq=message.getSeqNumber();

        System.out.println("WRITING " + data + " " + tid + " " + seq);
        if(ts.checkTid(tid)==false)
        {
            //System.out.println("201 ERROR");
            ReqMessage errmsg=new ReqMessage("ERROR", tid, seq,"201", data);
            cm.sendMsg(errmsg);
            return;
        }

        if(ts.HandleWrite(data, tid, seq)==-1)
        {
            System.out.println("204 ERROR");
            ReqMessage errmsg=new ReqMessage("ERROR", tid, seq,"204", data);
            cm.sendMsg(errmsg);
        } else {
            ReqMessage ackmsg=new ReqMessage("ACK", tid, seq,"0", data);
            ackmsg.print();
            cm.sendMsg(ackmsg);
        }

    }
    public void Commit(ResMessage message) {
        int tid=message.getTid();
        String data = message.getData();
        int seq=message.getSeqNumber();
        if(ts.checkTid(tid)==false)
        {
            //System.out.println("201 ERROR");
            ReqMessage errmsg=new ReqMessage("ERROR", tid, seq,"201", data);
            cm.sendMsg(errmsg);
            return;
        }

        Vector<Integer> commitmiss;
        try {
            commitmiss=ts.HandleCommit(tid, seq);
        } catch (IOException e) {

            e.printStackTrace();
            ReqMessage errmsg=new ReqMessage("ERROR",tid,seq,"202", data);
            cm.sendMsg(errmsg);
            return;
        }
        //System.out.println("Commiting " + data + " " + tid + " " + seq);
        if(commitmiss.size()==0)
        {
            ReqMessage ack=new ReqMessage("ACK",tid,seq, "0", data);
            cm.sendMsg(ack);
        }
        else if (commitmiss.get(0)==-1)
        {
            //
        }
        else
        {
            for(int i=0;i<commitmiss.size();i++){
                ReqMessage askresend=new ReqMessage("ASK_RESEND",tid,commitmiss.get(i),"0", data);
                cm.sendMsg(askresend);
            }
        }
    }
    public void Abort(ResMessage message){
        int tid=message.getTid();
        int seq=message.getSeqNumber();
        if(ts.checkTid(tid)==false)
        {
            ReqMessage errmsg=new ReqMessage("ERROR", tid, seq,"201", data);
            cm.sendMsg(errmsg);
            return;
        }

        try {
            ReqMessage ack=new ReqMessage("ACK",tid,seq, "0", data);
            cm.sendMsg(ack);
            ts.HandleAbort(tid, seq);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public int getTid() {
        return tid;
    }
    public void setTid(int tid) {
        this.tid = tid;
    }
    public int getSeqNumber() {
        return seqnumber;
    }
    public void setSeqNumber(int seqnumber) {
        this.seqnumber = seqnumber;
    }
    public int getContentLength() {
        return conlen;
    }
    public void setContentLength(int conlen) {
        this.conlen = conlen;
    }
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }
    @Override
    public String print() {

        return null;
    }


}
