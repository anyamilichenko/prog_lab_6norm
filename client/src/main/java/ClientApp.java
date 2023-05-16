import dto.CommandFromClientDto;
import dto.CommandResultDto;
import exceptions.DataCantBeSentException;
import exceptions.NoAnswerException;
import utilities.Pair;

import java.io.*;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.UnresolvedAddressException;

@SuppressWarnings("FieldCanBeLocal")
public class ClientApp {
    private final int clientPort;
    private final int serverPort;
    private final String clientIp;
    private final String serverIp;
    private final int waitingTime = 500;
    private final int countOfBytesForSize = 4;
    private final int timeoutToSend = 10;


    public ClientApp(int clientPort, int serverPort, String clientIp, String serverIp) {
        this.clientPort = clientPort;
        this.serverPort = serverPort;
        this.clientIp = clientIp;
        this.serverIp = serverIp;
    }

    //отправляет команду от клиента на сервер и получает результат выполнения команды от сервера
//создает неблокирующий канал DatagramChannel, отправляет сериализованный объект команды на сервер с помощью метода send,
// а затем получает результат выполнения команды от сервера с помощью метода receiveCommandResult
    public CommandResultDto sendCommand(CommandFromClientDto commandFromClientDto) throws DataCantBeSentException {
        try (DatagramChannel datagramChannel = DatagramChannel.open()) {
            datagramChannel.configureBlocking(false);
            send(datagramChannel, commandFromClientDto);
            return receiveCommandResult(datagramChannel);
        } catch (BindException e) {
            e.printStackTrace();
            return new CommandResultDto("Failed to send data to the address, binding exception. Please restart the client with other arguments");
        } catch (IOException e) {
            return new CommandResultDto("Something went wrong when executing the command, message: " + e.getMessage());
        }
    }


    //отправляет запрос серверу через созданный канал датаграм. Сначала он привязывает созданный канал датаграм к IP-адресу и порту клиента.
// Затем он создает объект типа InetSocketAddress с IP-адресом и портом сервера и использует его для отправки данных на сервер
    private void send(DatagramChannel datagramChannel, CommandFromClientDto commandFromClientDto) throws IOException, DataCantBeSentException {

        //datagramChannel.bind(new InetSocketAddress(clientIp, clientPort));

        SocketAddress serverSocketAddress = new InetSocketAddress(serverIp, serverPort);

        Pair<byte[], byte[]> pair = serialize(commandFromClientDto);

        byte[] sendDataBytes = pair.getFirst();
        byte[] sendDataAmountBytes = pair.getSecond();

        try {
            ByteBuffer sendDataAmountWrapper = ByteBuffer.wrap(sendDataAmountBytes);
            int limit = timeoutToSend;
            while (datagramChannel.send(sendDataAmountWrapper, serverSocketAddress) < sendDataAmountBytes.length) {
                limit -= 1;
                System.out.println("Failed to send, please try again");
                if (limit == 0) {
                    throw new DataCantBeSentException();
                }
            }
            ByteBuffer sendBuffer = ByteBuffer.wrap(sendDataBytes);
            while (datagramChannel.send(sendBuffer, serverSocketAddress) < sendDataBytes.length) {
                limit -= 1;
                System.out.println("Failed to send, please try again");
                if (limit == 0) {
                    throw new DataCantBeSentException();
                }
            }
        } catch (IOException e) {
            System.out.println("The address you specified could not be resolved. Please check this and maybe restart the client.");
            e.printStackTrace();
        } catch (UnresolvedAddressException e) {
            System.out.println("The data could not be sent because it was too large");
        }
    }

    //ожидает ответ от сервера, принимая на вход DatagramChannel и используя его для чтения данных из сети,
// и возвращает объект типа CommandResultDto. Если ответ не был получен в течение заданного времени ожидания,
// или если полученные данные не могут быть десериализованы в CommandResultDto, метод возвращает объект CommandResultDto с сообщением об ошибке.
    private CommandResultDto receiveCommandResult(DatagramChannel datagramChannel) throws IOException {
        byte[] amountOfBytesHeader = new byte[countOfBytesForSize];
        ByteBuffer amountOfBytesHeaderWrapper = ByteBuffer.wrap(amountOfBytesHeader);
        try {
            receiveToBuffer(datagramChannel, amountOfBytesHeaderWrapper, waitingTime);
            byte[] dataBytes = new byte[bytesToInt(amountOfBytesHeader)];


            ByteBuffer dataBytesWrapper = ByteBuffer.wrap(dataBytes);

            receiveToBuffer(datagramChannel, dataBytesWrapper, 1);

            return (CommandResultDto) deserialize(dataBytes);

        } catch (NoAnswerException e) {
            return new CommandResultDto("Could not get any otvet from the server");
        } catch (ClassNotFoundException e) {
            return new CommandResultDto("An incorrect otvet was received from the server");
        }
    }

    //пытается получить данные из datagramChannel и записать их в receiverBuffer.
// Если datagramChannel не возвращает никаких данных в течение заданного таймаута, метод выбрасывает исключение NoAnswerException.
// Если данные успешно получены, метод завершается без возвращаемого значения
    private void receiveToBuffer(DatagramChannel datagramChannel, ByteBuffer receiverBuffer, int timeoutMills) throws NoAnswerException, IOException {
        int timeout = timeoutMills;
        SocketAddress checkingAddress = null;

        while (checkingAddress == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace(); // never thrown
            }
            checkingAddress = datagramChannel.receive(receiverBuffer);
            if (timeout == 0) {
                throw new NoAnswerException("Превышен тайм-аут. Не удалось получить никаких данных");
            }
            timeout--;
        }
    }


    //принимает объект и возвращает пару байтовых массивов. Он используется для сериализации объекта в массив байтов,
    // который затем отправляется на удаленный сервер
    /**
     * @return первое - сами данные, второе - количество байт в данных
     */
    private Pair<byte[], byte[]> serialize(Object obj) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

        objectOutputStream.writeObject(obj);
        byte[] sizeBytes = ByteBuffer.allocate(countOfBytesForSize).putInt(byteArrayOutputStream.size()).array();

        return new Pair<>(byteArrayOutputStream.toByteArray(), sizeBytes);
    }

    public static int bytesToInt(byte[] bytes) {
        final int vosem = 8;
        final int ff = 0xFF;

        int value = 0;
        for (byte b : bytes) {
            value = (value << vosem) + (b & ff);
        }
        return value;
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }
}
