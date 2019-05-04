import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ZYBOFTP {
    BufferedWriter serverWriter;
    BufferedReader serverReader;

    public static void main(String[] args){
        ZYBOFTP z = new ZYBOFTP();
        z.go();
    }

    private void go(){
        try {
            Socket socket = new Socket("192.168.69.3", 21);
            serverWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.print("Connection established\n");
        } catch (Exception e){
            e.printStackTrace();
        }
    }
/*
    //Prints and returns server's reply from local inputstream. Should be called when commanding the server
    private String serverReply(int expectedReplies) throws Exception {
        String reply, temp;
        try {
            if (expectedReplies == 1) {
                temp = serverReader.readLine();
                System.out.println("SERVER:\t" + temp);
                reply = "SERVER:\t" + temp;
            } else {
                temp = serverReader.readLine();
                System.out.println("SERVER:\t" + temp);
                reply = "SERVER:\t" + temp;
                for (int i = 1; i < expectedReplies; i++) {
                    temp = serverReader.readLine();
                    System.out.println("SERVER:\t" + temp);
                    reply += "\nSERVER:\t" + temp;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed to read reply");
        }

        return reply;
    }
    */
}
