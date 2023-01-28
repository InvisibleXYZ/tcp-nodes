import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class DatabaseNode {
    private final Map<String, Status> connects;

    private int myPort;
    private String myAddress;

    private int key;
    private int value;

    private final Map<String, String> commandBuffer;

    public static void main(String[] args) {
        new DatabaseNode(args);
    }

    public DatabaseNode(String[]args){
        connects = new HashMap<>();

        myPort = 0;
        myAddress = "localhost";

        key = 0;
        value = 0;

        commandBuffer =
                new LinkedHashMap<String, String>() {
                    protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                        return size() > 5;
                    }
                };

        nodeStart(args);
    }

    private void nodeStart(String[] args){

        if (args.length < 4 || args.length % 2 == 1) {
            error("ERROR: wrong input. Make sure your input looks like:\n" +
                    "java DatabaseNode -tcpport <TCP port number> -record <key>:<value>[ -connect <address>:<port> ]", -6);
        }

        if (args[0].equals("-tcpport")) {
            try {
                myPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                error("ERROR: wrong port number", -6);
            }
        } else {
            error("ERROR: wrong input. Make sure your input looks like:\n" +
                    "java DatabaseNode -tcpport <TCP port number> -record <key>:<value>[ -connect <address>:<port> ]", -6);
        }

        if (args[2].equals("-record")) {
            if (!args[3].matches(".*:.*")) {
                error("ERROR: format of key and value. Make sure your input looks like\n:" +
                        "<key>:<value>", -6);
            }

            try {
                key = Integer.parseInt(args[3].replaceAll(":.*", ""));
                value = Integer.parseInt(args[3].replaceAll(".*:", ""));
            } catch (NumberFormatException e) {
                error("ERROR: wrong key or value number", -6);
            }
        } else {
            error("ERROR: wrong input. Make sure your input looks like:\n" +
                    "java DatabaseNode -tcpport <TCP port number> -record <key>:<value>[ -connect <address>:<port> ]", -6);
        }

        for (int i = 4; i < args.length; i += 2) {
            String address = null;
            int port = 0;

            if (args[i].equals("-connect")) {
                if (!args[i + 1].matches(".*:.*")) {
                    error("ERROR: format of address and port. Make sure your input looks like\n:" +
                            "<address>:<port>", -6);
                }

                try {
                    address = args[i + 1].replaceAll(":.*", "");
                    port = Integer.parseInt(args[i + 1].replaceAll(".*:", ""));
                } catch (NumberFormatException e) {
                    error("ERROR: wrong address or port number", -6);
                }

                if (address != null && port != 0) {
                    try (Socket socket = new Socket(address, port);
                         PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                        if(myAddress.equals("localhost")){
                            myAddress = socket.getLocalAddress().toString().substring(1);
                        }
                        writer.println("CONNECT " + myAddress + ":" + myPort);
                    } catch (UnknownHostException e) {
                        error("ERROR: unknown host", -5);
                    } catch (IOException e) {
                        error("ERROR: I/O exception", -5);
                    }

                    connects.put(address+":"+port, Status.CONNECTED);
                }
            } else {
                error("ERROR: wrong input. Make sure your input looks like:\n" +
                        "java DatabaseNode -tcpport <TCP port number> -record <key>:<value>[ -connect <address>:<port> ]", -6);
            }
        }

        try (ServerSocket mySocket = new ServerSocket(myPort)) {
            while (true) {
                Socket socket = mySocket.accept();
                new Thread(new NodeThread(socket)).start();
            }
        } catch (IOException e) {
            error("ERROR: I/O exception", -5);
        }
    }

    private class NodeThread implements Runnable {

        Socket socket;

        public NodeThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try(PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                if(myAddress.equals("localhost")){
                    myAddress = socket.getLocalAddress().toString().substring(1);
                }
                String line = reader.readLine();
                while (line != null){
                    if(line.matches("set-value .*:.*")){
                        String copyLine = line;
                        commandBuffer.entrySet().removeIf(e -> e.getKey().matches(copyLine.split(":")[0]+":.*") && !e.getValue().equals("ALDONE"));
                    }
                    else if(line.matches("get-value .*") || line.matches("find-key .*")){
                        String copyLine = line;
                        commandBuffer.entrySet().removeIf(e -> e.getKey().equals(copyLine) && !e.getValue().equals("ALDONE"));
                    }

                    if(commandBuffer.get(line) != null && commandBuffer.get(line).equals("ALDONE")){
                        writer.println(commandBuffer.get(line)); //ALREADY DONE
                        break;
                    }
                    else if (commandBuffer.get(line) != null && !commandBuffer.get(line).equals("ALDONE")){
                        writer.println(commandBuffer.get(line));
                        break;
                    }

                    System.out.println("RECEIVED: " + line);


                    if(line.matches("CONNECT .*:.*")){
                        String address = line.split(":")[0].substring(8);
                        int port = Integer.parseInt(line.split(":")[1].trim());

                        if(!connects.containsKey(address+":"+port))
                            connects.put(address+":"+port, Status.CONNECTED);
                    }
                    else if(line.matches("set-value .*:.*")){
                        int key;
                        int value;
                        try {
                            key = Integer.parseInt(line.split(":")[0].substring(10));
                            value = Integer.parseInt(line.split(":")[1].trim());
                        } catch (NumberFormatException e){
                            System.err.println("ERROR: wrong numbers");
                            break;
                        }


                        commandBuffer.put(line, "ALDONE");
                        String message = setValue(key, value);
                        commandBuffer.put(line, message);
                        writer.println(message);
                        break;
                    }
                    else if(line.matches("get-value .*")){

                        int key;
                        try {
                            key =Integer.parseInt(line.substring(10));
                        }catch (NumberFormatException e){
                            System.err.println("ERROR: wrong numbers");
                            break;
                        }


                        commandBuffer.put(line, "ALDONE");
                        String message = getValue(key);
                        commandBuffer.put(line, message);
                        writer.println(message);
                        break;
                    }
                    else if(line.matches("find-key .*")){
                        int key;
                        try {
                            key = Integer.parseInt(line.substring(9));
                        }catch (NumberFormatException e){
                            System.err.println("ERROR: wrong numbers");
                            break;
                        }

                        commandBuffer.put(line, "ALDONE");
                        String message = findKey(key);
                        commandBuffer.put(line, message);
                        writer.println(message);
                        break;
                    }
                    else if(line.matches("get-max")){

                        commandBuffer.put(line, "ALDONE");
                        String message = getMax();
                        commandBuffer.remove(line);
                        writer.println(message);
                        break;
                    }
                    else if(line.matches("get-min")){

                        commandBuffer.put(line, "ALDONE");
                        String message = getMin();
                        commandBuffer.remove(line);
                        writer.println(message);
                        break;
                    }
                    else if(line.matches("new-record .*:.*")){
                        int key;
                        int value;

                        try {
                            key = Integer.parseInt(line.split(":")[0].substring(11));
                            value = Integer.parseInt(line.split(":")[1].trim());
                        }catch (NumberFormatException e){
                            System.err.println("ERROR: wrong numbers");
                            break;
                        }

                        writer.println(newRecord(key, value));
                        break;
                    }
                    else if(line.matches("terminate")){
                        writer.println(terminate());
                        System.exit(0);
                    }
                    else if(line.matches("TERMINATED .*:.*")){
                        String address = line.split(":")[0].substring(11);
                        int port = Integer.parseInt(line.split(":")[1].trim());

                        if(connects.containsKey(address+":"+port)){
                            connects.put(address+":"+port, Status.TERMINATED);
                        }
                        else if(connects.containsKey("localhost:"+port)){
                            connects.put("localhost:"+port, Status.TERMINATED);
                        }
                    }
                    else break;
                    line = reader.readLine();
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void error(String text, int code) {
        System.err.println(text);
        System.exit(code);
        /*
         * -6: wrong input
         * -5: connection exception
         * -4: client serving error
         * */
    }

    private String setValue(int key, int value){
        if(this.key == key){
            this.value = value;
            System.out.println("CHANGED: " + this.key + ":" + this.value);
            return "OK";
        }

        for(String connection : connects.keySet()){
            if(connects.get(connection) == Status.TERMINATED) continue;

            try (Socket socket = new Socket(connection.split(":")[0], Integer.parseInt(connection.split(":")[1]));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                writer.println("set-value " + key + ":" + value);
                String line;
                while ((line = reader.readLine()) == null){
                    Thread.sleep(1000);
                }
                if(line.equals("ALDONE") || line.equals("ERROR")) continue;
                return line;
            } catch (IOException | InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }

        return "ERROR";
    }

    private String getValue(int key){
        if(this.key == key){
            System.out.println("FOUND: " + this.key + ":" + this.value);
            return String.valueOf(value);
        }

        for(String connection : connects.keySet()){
            if(connects.get(connection) == Status.TERMINATED) continue;

            try (Socket socket = new Socket(connection.split(":")[0], Integer.parseInt(connection.split(":")[1]));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                writer.println("get-value " + key);
                String line;
                while ((line = reader.readLine()) == null){
                    Thread.sleep(1000);
                }
                if(line.equals("ALDONE") || line.equals("ERROR")) continue;
                return line;
            } catch (IOException | InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }


        return "ERROR";
    }

    private String findKey(int key){
        if(this.key == key){
            System.out.println("FOUND: " + myAddress + ":" + myPort);
            return myAddress +":"+myPort;
        }

        for(String connection : connects.keySet()){
            if(connects.get(connection) == Status.TERMINATED) continue;

            try (Socket socket = new Socket(connection.split(":")[0], Integer.parseInt(connection.split(":")[1]));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                writer.println("find-key " + key);
                String line;
                while ((line = reader.readLine()) == null){
                    Thread.sleep(1000);
                }
                if(line.equals("ALDONE") || line.equals("ERROR")) continue;
                return line;
            } catch (IOException | InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }

        return "ERROR";
    }

    private String getMax(){
        int max = value;
        int keyToMax = key;

        for(String connection : connects.keySet()){
            if(connects.get(connection) == Status.TERMINATED) continue;

            try (Socket socket = new Socket(connection.split(":")[0], Integer.parseInt(connection.split(":")[1]));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                writer.println("get-max");
                String line;
                while ((line = reader.readLine()) == null){
                    Thread.sleep(1000);
                }
                if(line.equals("ALDONE")) continue;
                keyToMax = (max < Integer.parseInt(line.split(":")[1])) ? Integer.parseInt(line.split(":")[0]) : keyToMax;
                max =(Math.max(max, Integer.parseInt(line.split(":")[1])));
            } catch (IOException | InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }

        return (max < value) ? key+":"+value : keyToMax+":"+max;
    }

    private String getMin(){
        int min = value;
        int keyToMin = key;

        for(String connection : connects.keySet()){
            if(connects.get(connection) == Status.TERMINATED) continue;

            try (Socket socket = new Socket(connection.split(":")[0], Integer.parseInt(connection.split(":")[1]));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                writer.println("get-min");
                String line;
                while ((line = reader.readLine()) == null){
                    Thread.sleep(1000);
                }
                if(line.equals("ALDONE")) continue;
                keyToMin = (min > Integer.parseInt(line.split(":")[1])) ? Integer.parseInt(line.split(":")[0]) : keyToMin;
                min =(Math.min(min, Integer.parseInt(line.split(":")[1])));
            } catch (IOException | InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }

        return (min > value) ? key+":"+value : keyToMin+":"+min;
    }

    private String newRecord(int key, int value){
        this.key = key;
        this.value = value;
        System.out.println("NEW: " + this.key +":"+ this.value);
        return "OK";
    }

    private String terminate(){
        for(String connection : connects.keySet()){
            if(connects.get(connection) == Status.TERMINATED) continue;

            try (Socket socket = new Socket(connection.split(":")[0], Integer.parseInt(connection.split(":")[1]));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
                writer.println("TERMINATED " + myAddress + ":" + myPort);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

        System.out.println("TERMINATED");
        return "OK";
    }

    enum Status{
        CONNECTED, TERMINATED
    }

}
