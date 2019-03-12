import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class G20FTPClient {

    private final String STD_URL = "ftp.cs.brown.edu";
    private final String STD_USERNAME = "anonymous";
    private int dataPort;

    private Socket commandSocket;
    private Socket dataSocket;
    private BufferedWriter toServer;
    private BufferedInputStream bufferedInputStream;
    private FileOutputStream fileOutputStream;
    private BufferedReader fromServer;
    private Scanner input = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        G20FTPClient g20FTPClient = new G20FTPClient();
        g20FTPClient.clientFlow();
    }

    private void prepareCommandChannel() throws Exception {
        try {
            System.out.print("LOCAL:\tConnecting to " + STD_URL + " (command channel) ... ");
            commandSocket = new Socket(STD_URL, 21);
            System.out.print("SUCCESS\n");

            System.out.print("LOCAL:\tInitializing command socket streams ... ");
            toServer = new BufferedWriter(new OutputStreamWriter(commandSocket.getOutputStream()));
            fromServer = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
            System.out.print("SUCCESS\n");
            commandReply(5);

            System.out.print("LOCAL:\tLogging in ... ");
            commandServer("USER " + STD_USERNAME);
            System.out.print("SUCCESS\n");
            commandReply(1);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed command channel connection");
        }
    }

    private void prepareDataChannel() throws Exception {
        try {
            System.out.print("LOCAL:\tSetting server mode passive ... ");
            toServer.write("PASV\r\n");
            toServer.flush();
            System.out.print("SUCCESS\n");
            String passiveInfo = fromServer.readLine();
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
            e.printStackTrace();
            throw new Exception("!!! - Failed data channel connection");
        }
    }

    //Controls the flow of the program
    private void clientFlow() throws Exception {
        try {
            //Ensures both a socket for commanding and a socket for data receiving are opened and resources connected
            prepareCommandChannel();
            prepareDataChannel();

            //Main while loop showing and managing the user's options
            while (true) {
                System.out.println("\n" +
                        "--------------- CONNECTED TO FTP SERVER: " + STD_URL + " ---------------\n" +
                        "############################# G20 FTP Client #############################\n" +
                        "#                                                                        #\n" +
                        "#     WELCOME TO THE G20 FTP CLIENT                                      #\n" +
                        "#     ENTER A CHARACTER TO CONTINUE                                      #\n" +
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
            e.printStackTrace();
            throw new Exception("!!! - Main while loop failed");
        } finally {
            //Ensures all resources are closed if program crashes
            toServer.flush();
            fileOutputStream.flush();
            if (input != null) input.close();
            if (fileOutputStream != null) fileOutputStream.close();
            if (bufferedInputStream != null) bufferedInputStream.close();
            if (toServer != null) toServer.close();
            if (dataSocket != null) dataSocket.close();
            if (commandSocket != null) commandSocket.close();
        }
    }

    //Returns what the user writes to the scanner
    private String handleUserInput() throws Exception {
        try {
            System.out.print(">");
            return input.nextLine();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - User input failed");
        }
    }

    //Writes a command to local outputstream and flushes it to the server
    private void commandServer(String command) throws Exception {
        try {
            toServer.write(command + "\r\n");
            toServer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed writing to server");
        }
    }

    //Prints and returns server's reply from local inputstream. Should be called when commanding the server
    private String commandReply(int expectedReplies) throws Exception {
        String reply, temp;
        try {
            if (expectedReplies == 1) {
                temp = fromServer.readLine();
                System.out.println("SERVER:\t" + temp);
                reply = "SERVER:\t" + temp;
            } else {
                temp = fromServer.readLine();
                System.out.print("SERVER:\t" + temp);
                reply = "SERVER:\t" + temp;
                for (int i = 1; i < expectedReplies; i++) {
                    temp = fromServer.readLine();
                    System.out.print("\nSERVER:\t" + temp);
                    reply += "\nSERVER:\t" + temp;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed to read reply");
        }
        return reply;
    }

    //Downloads a file from the FTP server
    private void downloadFile() throws Exception {
        try {
            //Asks user to specify server filepath for file to download
            System.out.println("LOCAL:\tEnter the filepath for the file on the server (E.g.: /pub/README)");
            String filepath = handleUserInput();
            if (filepath.equals("")) {
                filepath = "/pub/README"; //Default
            }

            //Ask server for the size of the file to download, to ensure the file even exists
            commandServer("SIZE " + filepath);
            String[] reply = commandReply(1).split("\\s+");
            if (reply.length > 3) {
                throw new Exception("!!! - File not found on server");
            }

            //Asks the user where to download the file to
            System.out.println("LOCAL:\tEnter designated file name (E.g. MyFile.txt)");
            String fileName = handleUserInput();
            if (fileName.equals("")) {
                fileName = "DownloadedFile.txt"; //Default
            }
            fileOutputStream = new FileOutputStream(fileName);

            //Tells server to retrieve file, reads it from the data socket and saves it
            System.out.print("LOCAL:\tCommanding server to transfer file " + filepath + " ... ");
            commandServer("RETR " + filepath);
            System.out.print("SUCCESS\n");
            System.out.print("LOCAL:\tSaves the file as " + fileName + " ... ");
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.flush();
            System.out.print("SUCCESS\n");
            commandReply(4);

            //TODO: Mangler at udskrive 1KB af filen & kunne downloade to seperate filer

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed to download file");
        }
    }

    //Uploads a pre-made file from a local directory to the server
    private void uploadFile() throws Exception {
        try {
            //TODO: Mangler implementering
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed to upload file");
        }
    }

    //Ensures that all resources closes before shutting down the program
    private void quietExit() throws Exception {
        toServer.flush();
        fileOutputStream.flush();
        if (input != null) input.close();
        if (fileOutputStream != null) fileOutputStream.close();
        if (bufferedInputStream != null) bufferedInputStream.close();
        if (toServer != null) toServer.close();
        if (dataSocket != null) dataSocket.close();
        if (commandSocket != null) commandSocket.close();
        System.exit(0);
    }
}