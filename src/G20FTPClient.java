import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class G20FTPClient {

    private final String STD_URL = "ftp.cs.brown.edu";
    private final String STD_USERNAME = "anonymous";
    private Scanner input = new Scanner(System.in);

    private int dataPort;
    private byte[] buffer;

    private Socket commandSocket;
    private Socket dataSocket;
    private PrintWriter toServer;
    private BufferedInputStream bufferedInputStream;
    private FileOutputStream fileOutputStream;
    private Scanner fromServer;

    public static void main(String[] args) throws Exception {
        G20FTPClient g20FTPClient = new G20FTPClient();
        g20FTPClient.showMenu();
    }

    private void prepareCommandChannel() throws Exception{
        try {
            System.out.print("LOCAL:\tConnecting to " + STD_URL + " (command channel) ... ");
            commandSocket = new Socket(STD_URL,21);
            System.out.print("SUCCESS\n");

            System.out.print("LOCAL:\tInitializing command socket streams ... ");
            toServer = new PrintWriter(commandSocket.getOutputStream());
            fromServer = new Scanner(commandSocket.getInputStream());
            System.out.print("SUCCESS\n");
            commandReply(5);

            System.out.print("LOCAL:\tLogging in ... ");
            commandServer("USER " + STD_USERNAME);
            System.out.print("SUCCESS\n");
            commandReply(1);
        } catch (Exception e) {
            System.out.println("!!! - Failed command channel connection");
            e.printStackTrace();
        }
    }

    private void prepareDataChannel() throws Exception{
        try {
            System.out.print("LOCAL:\tSetting server mode passive ... ");
            toServer.write("PASV\r\n");
            toServer.flush();
            System.out.print("SUCCESS\n");
            String passiveInfo = fromServer.nextLine();
            String[] infoSplit = passiveInfo.split("\\D+");
            dataPort = Integer.parseInt(infoSplit[5]) * 256 + Integer.parseInt(infoSplit[6]);
            System.out.println("SERVER:\t" + passiveInfo);
            System.out.println("LOCAL:\tData channel port set to: " + dataPort);

            System.out.print("LOCAL:\tConnecting to " + STD_URL + " (data channel) ... ");
            dataSocket = new Socket(STD_URL, dataPort);
            System.out.print("SUCCESS\n");

            System.out.print("LOCAL:\tInitializing data socket streams ... ");
            bufferedInputStream = new BufferedInputStream(dataSocket.getInputStream());
            System.out.print("SUCCESS\n");

            System.out.print("LOCAL:\tSetting server transfer type to binary ... ");
            commandServer("TYPE I");
            System.out.print("SUCCESS\n");
            commandReply(1);

        } catch (Exception e) {
            System.out.println("!!! - Failed data channel connection");
            e.printStackTrace();
        }
    }

    private void showMenu() throws Exception {
        try {
            prepareCommandChannel();
            prepareDataChannel();
             while (true) {
                 System.out.println("\nPRESS ENTER TO CONTINUE");
                 input.nextLine();
                 System.out.println(
                         "--------------- CONNECTED TO FTP SERVER: " + STD_URL + " ---------------\n" +
                         "############################# G20 FTP Client #############################\n" +
                         "#                                                                        #\n" +
                         "#     WELCOME TO THE G20 FTP CLIENT                                      #\n" +
                         "#     WRITE A CHARACTER TO CONTINUE                                      #\n" +
                         "#                                                                        #\n" +
                         "#     COMMANDS:                                                          #\n" +
                         "#     > DL - Download file from server                                   #\n" +
                         "#     > UP - Upload file to server                                       #\n" +
                         "#     > Q - Quit the program                                             #\n" +
                         "#                                                                        #\n" +
                         "##########################################################################");

                 switch (handleUserInput().toLowerCase()) {
                     case "dl":
                         downloadFile();
                         break;
                     case "up":
                         uploadFile();
                         break;
                     case "q":
                         System.out.println("LOCAL:\tQutting program ...");
                         quietExit();
                         break;
                     default:
                         System.out.println("!!! - Wrong input");
                         break;
                 }
             }
        } catch (Exception e) {
            System.out.println("!!! - Main while loop failed");
        } finally {
            if (fileOutputStream != null) fileOutputStream.close();
            if (bufferedInputStream != null) bufferedInputStream.close();
            if (toServer != null) toServer.close();
            if (dataSocket != null) dataSocket.close();
            if (commandSocket != null) commandSocket.close();
        }
    }

    private String handleUserInput() {
        try {
            System.out.print(">");
            return input.nextLine();
        } catch (Exception e) {
            System.out.println("!!! - User input failed");
            e.printStackTrace();
        }
        return null;
    }

    private void commandServer(String command) {
        toServer.write(command + "\r\n");
        toServer.flush();
    }

    private String commandReply(int expectedReplies) {
        String reply = "";
        if (expectedReplies == 1) {
            reply = "SERVER:\t" + fromServer.nextLine();
        }  else {
            reply = "SERVER:\t" + fromServer.nextLine();
            for (int i = 1; i < expectedReplies; i++) {
                reply += "\nSERVER:\t" + fromServer.nextLine();
            }
        }
        System.out.println(reply);
        return reply;
    }

    private void downloadFile() throws Exception {
        try {
            int replyCode = 0;
            System.out.println("LOCAL:\tEnter the filepath for the file on the server (E.g.: /pub/README)");
            String filepath = handleUserInput();
            if (filepath.equals("")) {
                filepath = "/pub/README";
            }
            commandServer("SIZE " + filepath);
            String[] reply = commandReply(1).split("\\s+");
            if (reply.length > 3) {
                System.out.println("!!! - File not found on server");
                throw new Exception();
            }

            System.out.println("LOCAL:\tEnter designated file name (E.g. MyFile.txt)");
            String fileName = handleUserInput();
            if (fileName.equals("")) {
                fileName = "DownloadedFile.txt";
            }
            fileOutputStream = new FileOutputStream(fileName);

            commandServer("RETR " + filepath);
            buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer,0,bytesRead);
            }
            commandReply(2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadFile() {

    }

    private void quietExit() throws Exception{
        if (fileOutputStream != null) fileOutputStream.close();
        if (bufferedInputStream != null) bufferedInputStream.close();
        if (toServer != null) toServer.close();
        if (dataSocket != null) dataSocket.close();
        if (commandSocket != null) commandSocket.close();
        System.exit(0);
    }
}