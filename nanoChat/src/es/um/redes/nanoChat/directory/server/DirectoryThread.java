package es.um.redes.nanoChat.directory.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DirectoryThread extends Thread {

	// Tamaño máximo del paquete UDP
	private static final int PACKET_MAX_SIZE = 128;
	// Estructura para guardar las asociaciones ID_PROTOCOLO -> Dirección del
	// servidor
	protected Map<Integer, InetSocketAddress> servers;
	private static final byte OP_REGISTRATION_INFO = 1;
	private static final byte OP_REGISTRATION_OK = 2;
	private static final byte OP_QUERY_PROTOCOL = 3;
	private static final byte OP_SERVER_INFO = 4;
	private static final byte OP_NO_SERVER = 5;

	// Socket de comunicación UDP
	protected DatagramSocket socket = null;
	// Probabilidad de descarte del mensaje
	protected double messageDiscardProbability;

	public DirectoryThread(String name, int directoryPort, double corruptionProbability) throws SocketException {
		super(name);
		// TODO (hecho) Anotar la dirección en la que escucha el servidor de Directorio
		InetSocketAddress serverAddress = new InetSocketAddress(directoryPort);
		// TODO (hecho) Crear un socket de servidor
		socket = new DatagramSocket(serverAddress);
		messageDiscardProbability = corruptionProbability;
		// Inicialización del mapa
		servers = new HashMap<Integer, InetSocketAddress>();
	}

	@Override
	public void run() {
		byte[] buf = new byte[PACKET_MAX_SIZE];

		System.out.println("Directory starting...");
		boolean running = true;
		while (running) {

			// TODO 1) (hecho) Recibir la solicitud por el socket
			DatagramPacket dpRec = new DatagramPacket(buf, buf.length);
			try {
				socket.receive(dpRec);
			} catch (IOException e) {
				e.printStackTrace();
			}
			// TODO 2) (hecho) Extraer quién es el cliente (su dirección)
			InetSocketAddress clientAddress = (InetSocketAddress) dpRec.getSocketAddress();
			// 3) Vemos si el mensaje debe ser descartado por la probabilidad de descarte

			double rand = Math.random();
			if (rand < messageDiscardProbability) {
				System.err.println("Directory DISCARDED corrupt request from... ");
				continue;
			}

			// TODO 4) Analizar y procesar la solicitud (llamada a processRequestFromCLient)
			// TODO 5) Tratar las excepciones que puedan producirse
			try {
				processRequestFromClient(dpRec.getData(), clientAddress);
			} catch (IOException e) {
				e.printStackTrace();
			}
			buf = new byte[PACKET_MAX_SIZE];
		}
		socket.close();
	}

	// Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, InetSocketAddress clientAddr) throws IOException {
		// TODO 1) (hecho) Extraemos el tipo de mensaje recibido
		ByteBuffer ret = ByteBuffer.wrap(data);
		byte opCode = ret.get();
		// TODO 2) (hecho) Procesar el caso de que sea un registro y enviar mediante
		// sendOK
		switch (opCode) {
		case OP_REGISTRATION_INFO: {
			byte protocolId = ret.get();
			int port = ret.getInt();
			System.out.println("Incoming message, opCode = " + Byte.toString(opCode) + " (register chat server)"
					+ ", protocol = " + Byte.toString(protocolId) + ", port = " + Integer.toString(port));
			InetAddress chatserverAddress = clientAddr.getAddress();
			InetSocketAddress chatserverSocketAddress = new InetSocketAddress(chatserverAddress, port);
			servers.put((int) protocolId, chatserverSocketAddress);
			System.out.println("Value of servers (Map): ");
			for (Map.Entry<Integer, InetSocketAddress> entry : servers.entrySet()) {
				Integer key = entry.getKey();
				InetSocketAddress value = entry.getValue();
				String entry_address = value.getAddress().toString().substring(1);
				Integer entry_port = value.getPort();
				System.out.println(key.toString() + ": " + entry_address + " - " + entry_port.toString());
			}
			sendOK(clientAddr);
			break;
		}
		// TODO 3)(hecho) Procesar el caso de que sea una consulta
		case OP_QUERY_PROTOCOL: {
			byte protocolId = ret.get();
			System.out.println("Incoming message, opCode = " + Byte.toString(opCode) + ", protocol = "
					+ Byte.toString(protocolId));
			// TODO 3.1)(hecho) Devolver una dirección si existe un servidor
			// (sendServerInfo)
			if (servers.containsKey((int) protocolId)) {
				sendServerInfo(servers.get((int) protocolId), clientAddr);
				break;
			}
			// TODO 3.2)(hecho) Devolver una notificación si no existe un servidor
			// (sendEmpty)
			else {
				sendEmpty(clientAddr);
				break;
			}

		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + opCode);
		}

	}

	// Método para enviar una respuesta vacía (no hay servidor)
	private void sendEmpty(InetSocketAddress clientAddr) throws IOException {
		// TODO (hecho) Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(1);
		byte opCode = OP_NO_SERVER;
		bb.put(opCode);
		byte[] men = bb.array();
		// TODO (hecho) Enviar respuesta
		DatagramPacket dpSend = new DatagramPacket(men, men.length, clientAddr);
		socket.send(dpSend);
	}

	// Método para enviar la dirección del servidor al cliente
	private void sendServerInfo(InetSocketAddress serverAddress, InetSocketAddress clientAddr) throws IOException {
		// TODO (hecho) Obtener la representación binaria de la dirección
		byte[] ip = serverAddress.getAddress().getAddress();
		int puerto = serverAddress.getPort();
		// TODO (hecho) Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(9);
		byte opCode = OP_SERVER_INFO;
		bb.put(opCode);
		for (int i = 0; i < ip.length; i++) {
			bb.put(ip[i]);
		}
		bb.putInt(puerto);
		byte[] men = bb.array();
		// TODO (hecho) Enviar respuesta
		DatagramPacket dpSend = new DatagramPacket(men, men.length, clientAddr);
		socket.send(dpSend);

	}

	// Método para enviar la confirmación del registro
	private void sendOK(InetSocketAddress clientAddr) throws IOException {
		// TODO (hecho) Construir respuesta
		ByteBuffer bb = ByteBuffer.allocate(1);
		byte opCode = OP_REGISTRATION_OK;
		bb.put(opCode);
		byte[] men = bb.array();
		// TODO (hecho) Enviar respuesta
		DatagramPacket dpSend = new DatagramPacket(men, men.length, clientAddr);
		socket.send(dpSend);
	}
}
