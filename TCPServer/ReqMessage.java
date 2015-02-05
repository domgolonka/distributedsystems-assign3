
import java.io.IOException;

public class ReqMessage extends Message {
    private String method;
    private int tid;
    private int seqnumber;
    private int conlen;
    private String errcode;
    private String data;

    private String sequence;
    public ReqMessage(String method, int tid, int seqnumber, String errcode, String data)  {
        this.method=method;
        this.tid=tid;
        this.seqnumber=seqnumber;
        this.errcode = errcode;
        this.data = data;
    }
    public void ACK(ReqMessage msg) throws IOException{}
    public void AskResend(ReqMessage msg) throws IOException{}
    public void Error(ReqMessage msg) throws IOException{}
    public String getErrorName(String errcode) {
        if (errcode == "201") {
            return "Invalid transaction ID. Sent by the server if the client had sent a message that included an invalid transaction ID, i.e., a transaction ID that the server does not remember";
        }
        else if (errcode == "202") {
            return "Invalid operation. Sent by the server if the client attemtps to execute an invalid operation - i.e., HandleWrite as part of a transaction that had been committed";
        }
        else if (errcode == "204") {
            return "Wrong message format. Sent by the server if the message sent by the client does not follow the specified message format";
        }
        else if (errcode == "205") {
            return "File I/O error";
        }
        else if (errcode == "206") {
            return "File not found";
        }
        return null;
    }
    public String getSequence() {
        return sequence;
    }
    public void setSequence(String sequence) {
        this.sequence = sequence;
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
    public void setErrcode(String errcode) {
        this.errcode = errcode;
    }
    public String getErrcode() {
        return errcode;
    }

    public int getTid() {
        return tid;
    }
    public void setTid(int transId) {
        this.tid = transId;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    @Override
    public String print() {
        String message = new String();
        message +=method;
        message += "\t";
        message +=tid;
        message += "\t";
        message +=seqnumber;
        message += "\t";
        if (errcode != "0") {
            message  += errcode + "\t";
        } else {
            message  += "0\t";
        }
        if(data != null && !data.isEmpty()) {
            this.conlen = data.length();
            message +=conlen;
            message += "\t";
            message += data;
            message +="\r\n\r\n\r\n";
        } else if (errcode != "0") {
            this.conlen = errcode.length();
            message +=conlen + "\t";
            message += this.getErrorName(errcode);
        } else {
            message += "\r\n\r\n\r\n";
        }
        try {

                return(message  + '\0');

            //return message;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

}
