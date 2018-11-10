package server.net;

import common.Request;
import common.Response;
import server.model.Game;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Handles all communication with one specific game client, makes sure to serve the client with what it requests.
 */
public class ClientHandler implements Runnable {

    private OutputStream toTheClient;
    private InputStream fromTheClient;
    private Game game = new Game();
    private boolean isConnected;
    private Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        isConnected = true;
        this.clientSocket = clientSocket;

    }
    @Override
    public void run(){
        Response response;
        try {
            fromTheClient = clientSocket.getInputStream();
            toTheClient = clientSocket.getOutputStream();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        while(isConnected){
            ByteBuffer byteBuffer;
            byte[] temp = new byte[4];
            try {
                for(int i = 0;i<4;i++){
                    temp[i] = (byte)fromTheClient.read();
                }
                byteBuffer = ByteBuffer.wrap(temp);
                int size = byteBuffer.getInt(0);
                byte[] object = new byte[size];
                for(int i = 0; i<size;i++) {
                    object[i] = (byte) fromTheClient.read();
                }
                Request request = byteArrayToResponseObject(object);
                switch (request.getRequestType()){
                    case QUIT:
                        clientDisconnect();
                        break;
                    case NEW_GAME:
                        response = game.newGame();
                        sendToClient(response);
                        break;
                    case GUESSLETTER:
                        response = game.guessWithLetter(request.getLetterToGuess());
                        sendToClient(response);
                        break;
                    case GUESSWORD:
                        response = game.guessWithWord(request.getWordToGuess());
                        sendToClient(response);
                        break;
                    default :
                        System.out.println("no ");
                }
            }
            catch (IOException ex) {
                clientDisconnect();
            }
        }
    }

    /**
     * Responsible to send data to the client through the socket.
     */
    public void sendToClient(Response response) {
        try {
            byte[] temp = calculateAndPrependSizeOfObjectToBeSent(response);
            for (int i = 0;i<temp.length;i++)
                toTheClient.write(temp[i]);
            toTheClient.flush();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void clientDisconnect(){
        try {
            clientSocket.close();
            isConnected = false;
        }
        catch(IOException ex){
            System.err.println("Failed to disconnect client");
        }
    }

    private byte[] calculateAndPrependSizeOfObjectToBeSent(Response response){
        byte[] objectArray;
        byte[] copy = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            objectOutputStream.writeObject(response);
            objectOutputStream.flush();
            objectOutputStream.close();

            objectArray = byteArrayOutputStream.toByteArray();

            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.putInt(objectArray.length);

            byte[] byteBufferArray = byteBuffer.array();
            copy = new byte[byteBufferArray.length+objectArray.length];

            System.arraycopy(byteBufferArray, 0, copy, 0, byteBufferArray.length);
            System.arraycopy(objectArray,0,copy,byteBufferArray.length,objectArray.length);

        }catch (IOException e){
            e.printStackTrace();
        }

        return copy;
    }

    private Request byteArrayToResponseObject(byte[] objectByteArray){
        Request request = null;
        try {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(objectByteArray);
            ObjectInputStream in = new ObjectInputStream(byteArrayInputStream);
            request = (Request) in.readObject();
        } catch (IOException e){
            e.printStackTrace();
        }catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        return request;
    }
}
