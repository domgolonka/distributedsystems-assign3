
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

public abstract class Message implements Serializable {
    static ArrayList<String> messages = new ArrayList<String>();
    public Message() {

        Start();
    }
    static void Start() {
        messages.add("READ"); //0
        messages.add("NEW_TXN");//1
        messages.add("WRITE");//2
        messages.add("COMMIT");//3
        messages.add("ABORT");//4
        messages.add("ACK");//5
        messages.add("ASK_RESEND");//6
        messages.add("ERROR");//7
    }

    public static Message ResponseMessage(Communication cm, String method, int tid, int sequence, int conlen, String data) throws IOException {


        ResMessage resm = new ResMessage(cm, method, tid, sequence, conlen, data);
        switch(messages.indexOf(method)) {
            case 0:
                resm.Read(resm);
                //System.out.println("Method " + method + " Data" + data);
                break;
            case 1:
                resm.NewTxn(resm);
                //System.out.println("Method " + method + " Data" + data);
                break;
            case 2:
                resm.Write(resm);
                //System.out.println("Method " + method + " Data" + data);
                break;
            case 3:
                resm.Commit(resm);
                //System.out.println("Method " + method + " Data" + data);
                break;
            case 4:
                resm.Abort(resm);
                //System.out.println("Method " + method + " Data" + data);
                break;
            default:
                break;
        }
        return null;
    }
    public static Message RequestMessage(String method, int tid, int sequence, String  errcode, String data) throws IOException {
        ReqMessage reqm = new ReqMessage(method, tid, sequence, errcode, data);
        switch(messages.indexOf(method)) {
            case 0:
                reqm.ACK(reqm);
                break;
            case 1:
                reqm.AskResend(reqm);
                break;
            case 2:
                reqm.Error(reqm);
                break;
            default:
                break;
        }
        return null;
    }
    public abstract String print();


}
