import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class G20FTPClient {

    private final String STD_URL = "ftp.cs.brown.edu";
    private final String STD_USERNAME = "anonymous";
    private int dataPort;
    private boolean firstDataConnection = true;

    private Socket commandSocket;
    private Socket dataSocket;
    private BufferedWriter serverWriter;
    private BufferedReader serverReader;
    private BufferedInputStream bufferedInputStream;
    private FileOutputStream fileOutputStream;
    private Scanner inputScanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        G20FTPClient g20FTPClient = new G20FTPClient();
        g20FTPClient.clientFlow();
    }

    private void prepareControlChannel() throws Exception {
        try {
            System.out.print("LOCAL:\tOpening socket to " + STD_URL + " on port " + 21 + " (control channel) ... ");
            commandSocket = new Socket(STD_URL, 21);
            System.out.print("SUCCESS\n");

            System.out.print("LOCAL:\tInitializing control socket streams ... ");
            serverWriter = new BufferedWriter(new OutputStreamWriter(commandSocket.getOutputStream()));
            serverReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
            System.out.print("SUCCESS\n");
            serverReply(5);

            System.out.print("LOCAL:\tLogging in to FTP server ... ");
            serverCommand("USER " + STD_USERNAME);
            System.out.print("SUCCESS\n");
            serverReply(1);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed control channel connection");
        }
    }

    private void prepareDataChannel() throws Exception {
        try {
            System.out.print("LOCAL:\tRequesting server to set mode to passive ... ");
            serverWriter.write("PASV\r\n");
            serverWriter.flush();
            System.out.print("SUCCESS\n");
            String passiveInfo = serverReader.readLine();
            String[] infoSplit = passiveInfo.split("\\D+");
            dataPort = Integer.parseInt(infoSplit[5]) * 256 + Integer.parseInt(infoSplit[6]);
            System.out.println("SERVER:\t" + passiveInfo);
            System.out.println("LOCAL:\tData channel port set to: " + dataPort);

            System.out.print("LOCAL:\tRequesting server to set transfer type to binary ... ");
            serverCommand("TYPE I");
            System.out.print("SUCCESS\n");
            serverReply(1);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed setting up to use data channel");
        }
    }

    //Controls the flow of the program
    private void clientFlow() throws Exception {
        try {
            //Ensures both a socket for commanding and a socket for data receiving are opened and resources connected
            prepareControlChannel();
            prepareDataChannel();

            //Main while loop showing and managing the user's options
            while (true) {
                serverCommand("SYST");
                serverReply(1);
                System.out.println("" +
                        "---------------- CONNECTED TO FTP SERVER " + STD_URL + " ----------------\n" +
                        "############################# G20 FTP Client #############################\n" +
                        "#                                                                        #\n" +
                        "#     WELCOME TO THE G20 FTP CLIENT                                      #\n" +
                        "#     ENTER A CHARACTER TO CONTINUE                                      #\n" +
                        "#                                                                        #\n" +
                        "#     ACCEPTED COMMANDS:                                                 #\n" +
                        "#     > TC - Test the client with 2 downloads and 1 upload               #\n" +
                        "#     > DL - Download file from server                                   #\n" +
                        "#     > UP - Upload file to server                                       #\n" +
                        "#     > Q - Quit the program                                             #\n" +
                        "#                                                                        #\n" +
                        "##########################################################################");

                switch (handleUserInput().toLowerCase()) {
                    case "tc":
                        downloadFile(true);
                        downloadFile(true);
                        uploadFile(true);
                        break;
                    case "dl":
                        downloadFile(false);
                        break;
                    case "up":
                        uploadFile(false);
                        break;
                    case "q":
                        System.out.println("LOCAL:\tQutting program ...");
                        quietExit();
                        break;
                    default:
                        System.out.println("!!! - Wrong inputScanner");
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Main while loop failed");
        } finally {
            //Ensures used resources are closed if program crashes
            serverWriter.flush();
            fileOutputStream.flush();
            if (inputScanner != null) inputScanner.close();
            if (serverWriter != null) {
                serverWriter.flush();
                serverWriter.close();
            }
            if (commandSocket != null) commandSocket.close();
        }
    }

    //Returns what the user writes to the scanner
    private String handleUserInput() throws Exception {
        try {
            System.out.print(">");
            return inputScanner.nextLine();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - User inputScanner failed");
        }
    }

    //Writes a command to local outputstream and flushes it to the server
    private void serverCommand(String command) throws Exception {
        try {
            serverWriter.write(command + "\r\n");
            serverWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed writing to server");
        }
    }

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

    //Downloads a file from the FTP server
    private void downloadFile(boolean testmode) throws Exception {
        try {
            //Opening the dataSocket. Sender automatically closes after file transfer.
            System.out.print("LOCAL:\tOpening socket to " + STD_URL + " on port " + dataPort + " (data channel) ... ");
            dataSocket = new Socket(STD_URL, dataPort);
            System.out.print("SUCCESS\n");
            System.out.print("LOCAL:\tInitializing data socket streams ... ");
            bufferedInputStream = new BufferedInputStream(dataSocket.getInputStream());
            System.out.print("SUCCESS\n");

            //Asks user to specify server filepath for file to download if test is not being run
            System.out.println("LOCAL:\tEnter the filepath for the file on the server (E.g.: /pub/README)");
            String filepath;
            if (testmode) {
                if (firstDataConnection) {
                    filepath = "/pub/README";
                } else {
                    filepath = "/u/dg/Collision.java";
                }
            } else {
                filepath = handleUserInput();
            }
            if (filepath.equals("")) {
                filepath = "/pub/README"; //Default
                System.out.println("LOCAL:\tUsing default: " + filepath);
            }

            //Ask server for the size of the file to download, to ensure the file even exists
            serverCommand("SIZE " + filepath);
            String[] reply = serverReply(1).split("\\s+");
            if (reply.length > 3) {
                throw new Exception("!!! - File not found on server");
            }

            //Asks the user where to download the file to
            System.out.println("LOCAL:\tEnter designated file name (E.g. MyFile.txt)");
            String fileName;
            if (testmode) {
                if (firstDataConnection) {
                    fileName = "TestOver1KB";
                } else {
                    fileName = "TestUnder1KB";
                }
            } else {
                fileName = handleUserInput();
            }
            if (fileName.equals("")) {
                fileName = "DownloadedFile.txt"; //Default
                System.out.println("LOCAL:\tUsing default: " + fileName);
            }
            fileOutputStream = new FileOutputStream(fileName);

            //Tells server to retrieve file, reads it from the data socket and saves it
            System.out.print("LOCAL:\tCommanding server to transfer file " + filepath + " ... ");
            serverCommand("RETR " + filepath);
            System.out.print("SUCCESS\n");
            System.out.print("LOCAL:\tSaves the file as " + fileName + " ... ");
            byte[] buffer = new byte[Integer.parseInt(reply[2])];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
            fileOutputStream.flush();
            System.out.print("SUCCESS\n");
            if (firstDataConnection) {
                serverReply(4);
                firstDataConnection = false;
            } else {
                serverReply(3);
            }

            //Printing process of the first kilobyte
            System.out.print("\nLOCAL: FIRST TRANSFERRED KILOBYTE\n");
            int count = 1;
            for (byte b : buffer) {
                if (count == 1)
                    System.out.print("FILE: ");
                System.out.print((char)b);
                if (b == 10) {
                    System.out.print("FILE: ");
                }
                if (count++ > 1024)
                    break;
            }
            System.out.print("\nLOCAL: END OF KILOBYTE\n\n");

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed to download file");
        } finally {
            //Ensures used resources are closed if program crashes
            if (bufferedInputStream != null) bufferedInputStream.close();
            if (dataSocket != null) dataSocket.close();
            if (fileOutputStream != null) {
                fileOutputStream.flush();
                fileOutputStream.close();
            }
        }
    }

    //Uploads a pre-made file from a local directory to the server
    private void uploadFile(boolean testmode) throws Exception {
        try {
            //TODO: Mangler implementering
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("!!! - Failed to upload file");
        }
    }

    //Ensures that all resources closes before shutting down the program
    private void quietExit() throws Exception {
        if (inputScanner != null) inputScanner.close();
        if (serverWriter != null) {
            serverWriter.flush();
            serverWriter.close();
        }
        if (fileOutputStream != null) {
            fileOutputStream.flush();
            fileOutputStream.close();
        }
        if (bufferedInputStream != null) bufferedInputStream.close();
        if (dataSocket != null) dataSocket.close();
        if (commandSocket != null) commandSocket.close();
        System.exit(0);
    }
}