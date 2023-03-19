package es.um.redes.nanoChat.directory.connector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	// Tamaño máximo del paquete UDP (los mensajes intercambiados son muy cortos)
	private static final int PACKET_MAX_SIZE = 128;
	// Puerto en el que atienden los servidores de directorio
	private static final int DEFAULT_PORT = 6868;
	// Valor del TIMEOUT
	private static final int TIMEOUT = 1000;
	// Constante que indica el número máximo de reintentos
	private static final int MAX_REINTENTOS = 5;
	private static final byte OP_REGISTRATION_INFO = 1;
	private static final byte OP_REGISTRATION_OK = 2;
	private static final byte OP_QUERY_PROTOCOL = 3;
	private static final byte OP_SERVER_INFO = 4;
	// private static final byte OP_NO_SERVER = 5;

	private DatagramSocket socket; // socket UDP
	private InetSocketAddress directoryAddress; // dirección del servidor de directorio

	public DirectoryConnector(String agentAddress) throws IOException {
		// TODO (hecho) A partir de la dirección y del puerto generar la dirección de
		// conexión para el Socket
		directoryAddress = new InetSocketAddress(InetAddress.getByName(agentAddress), DEFAULT_PORT);
		// TODO (hecho) Crear el socket UDP
		socket = new DatagramSocket();
	}

	/*
	 * Esnvía una solicitud para obtener el servidor de chat asociado a un
	 * determinado protocolo 
	 */

	// Método público para enviar una cadena al servidor de directorio y quedar a la
	// espera de la recepción de un
	// datagrama (que contendrá esa misma cadena, pero en mayúscula, porque es así
	// como hemos programado el servidor
	// de directorio).

	public String convertToUpper(String strToConvert) throws IOException {
		byte[] bufSend = strToConvert.getBytes();
		DatagramPacket dpSend = new DatagramPacket(bufSend, bufSend.length, directoryAddress);
		byte[] bufRec = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(bufRec, bufRec.length);
		//socket.send(dpSend);
		// cuando se vence el TIMEOUT salta la excepción
		// utilizar variable para que haya un maximo de reintentos
		int cont = 0;
		while (cont < MAX_REINTENTOS) {
			socket.send(dpSend);
			socket.setSoTimeout(TIMEOUT);
			try {
				socket.receive(dpRec);
			} catch (SocketTimeoutException e) {
				cont++;
				System.out.println(
						"There was an error trying to conect, " + (MAX_REINTENTOS - cont) + " oportunities left");
				continue;
			}
			break;
		}
		String strConverted = new String(dpRec.getData());
		return strConverted;
	}

	public InetSocketAddress getServerForProtocol(int protocol) throws IOException {

		// TODO Generar el mensaje de consulta llamando a buildQuery()
		byte[] men = buildQuery(protocol);
		// TODO Construir el datagrama con la consulta
		DatagramPacket dpSend = new DatagramPacket(men, men.length, directoryAddress);
		// TODO Enviar datagrama por el socket
		// TODO preparar el buffer para la respuesta
		byte[] res = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(res, res.length);
		// TODO Establecer el temporizador para el caso en que no haya respuesta
		int cont = 0;
		while (cont < MAX_REINTENTOS) {
			// TODO Recibir la respuesta
			socket.send(dpSend);
			socket.setSoTimeout(TIMEOUT);
			try {
				socket.receive(dpRec);
			} catch (SocketTimeoutException e) {
				cont++;
				System.out.println(
						"There was an error trying to conect, " + (MAX_REINTENTOS - cont) + " oportunities left");
				continue;
			}
			break;
		}
		
		// TODO Procesamos la respuesta para devolver la dirección que hay en ella
		InetSocketAddress resp = getAddressFromResponse(dpRec);
		return resp;
	}

	// Método para generar el mensaje de consulta (para obtener el servidor asociado
	// a un protocolo)
	private byte[] buildQuery(int protocol) {
		// TODO Devolvemos el mensaje codificado en binario según el formato acordado
		ByteBuffer bb = ByteBuffer.allocate(2);
		byte opCode = OP_QUERY_PROTOCOL;
		byte protocolId = ((Integer) protocol).byteValue();
		bb.put(opCode);
		bb.put(protocolId);
		byte[] men = bb.array();
		return men;
	}

	// Método para obtener la dirección de internet a partir del mensaje UDP de
	// respuesta
	private InetSocketAddress getAddressFromResponse(DatagramPacket packet) throws UnknownHostException {
		// TODO Analizar si la respuesta no contiene dirección (devolver null)
		ByteBuffer ret = ByteBuffer.wrap(packet.getData());
		byte opCode = ret.get();
		// TODO Si la respuesta no está vacía, devolver la dirección (extraerla del mensaje)
		if (opCode == OP_SERVER_INFO) {
			byte[] serverIP = new byte[4];
			ret.get(serverIP);
			InetAddress addr = InetAddress.getByAddress(serverIP);
			InetSocketAddress resp = new InetSocketAddress(addr, ret.getInt());
			return resp;
		}
		
		return null;
	}

	/**
	 * Envía una solicitud para registrar el servidor de chat asociado a un
	 * determinado protocolo
	 *
	 */

	// Igual pero con los reenvíos
	public boolean registerServerForProtocol(int protocol, int port) throws IOException {
		boolean resp = false;
		// TODO (hecho) Construir solicitud de registro (buildRegistration)
		byte[] men = buildRegistration(protocol, port);
		// TODO Enviar solicitud
		DatagramPacket dpSend = new DatagramPacket(men, men.length, directoryAddress);
		// TODO Recibe respuesta
		byte[] res = new byte[PACKET_MAX_SIZE];
		DatagramPacket dpRec = new DatagramPacket(res, res.length);
		//Configuramos un temporizador y un número máximo de reintentos
		int cont = 0;
		while (cont < MAX_REINTENTOS) {
			socket.send(dpSend);
			socket.setSoTimeout(TIMEOUT);
			try {
				socket.receive(dpRec);
			} catch (SocketTimeoutException e) {
				cont++;
				System.out.println(
						"There was an error trying to conect, " + (MAX_REINTENTOS - cont) + " oportunities left");
				continue;
			}
			break;
		}
		ByteBuffer ret = ByteBuffer.wrap(dpRec.getData());
		byte opCode = ret.get();
		if (opCode == OP_REGISTRATION_OK) {
			resp = true;
		}
		return resp;
		
		
	}

	// Método para construir una solicitud de registro de servidor
	// OJO: No hace falta proporcionar la dirección porque se toma la misma desde la
	// que se envió el mensaje
	private byte[] buildRegistration(int protocol, int port) {
		ByteBuffer bb = ByteBuffer.allocate(6);
		byte opCode = OP_REGISTRATION_INFO;
		byte protocolId = ((Integer) protocol).byteValue();
		bb.put(opCode);
		bb.put(protocolId);
		bb.putInt(port);
		byte[] men = bb.array();
		return men;
		// TODO Devolvemos el mensaje codificado en binario según el formato acordado
	}

	public void close() {
		socket.close();
	}
}
