package com.mycompany.app.simulated_device;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class App {
	private static String connString = "HostName=myIoTHubKe.azure-devices.net;DeviceId=myFirstJavaDevice;SharedAccessKey=QubkaeVkEGh7rlef8YhjeQ==";
	private static IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
	private static String deviceId = "myFirstJavaDevice";
	private static DeviceClient client;

	public static void main(String[] args) throws IOException, URISyntaxException {
		try {
			client = new DeviceClient(connString, protocol);
			client.open();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		MessageSender sender = new MessageSender();

		ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(sender);

		System.out.println("Press ENTER to exit.");
		System.in.read();
		executor.shutdownNow();
		client.close();
	}

	private static class TelemetryDataPoint {
		public int gasLeak;

		public String serialize() {
			Gson gson = new Gson();
			return gson.toJson(this);
		}
	}

	private static class EventCallback implements IotHubEventCallback {
		public void execute(IotHubStatusCode status, Object context) {
			System.out.println("IoT Hub responded to message with status: " + status.name());

			if (context != null) {
				synchronized (context) {
					context.notify();
				}
			}
		}
	}

	private static class MessageSender implements Runnable {

		public void run() {
			try {
				Random rand = new Random();

				for(int i = 1; i < 1001; i++ ) {
					int currentGasLeak = rand.nextInt(1000-1) + i;
					TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
					telemetryDataPoint.gasLeak = currentGasLeak;

					String msgStr = telemetryDataPoint.serialize();
					Message msg = new Message(msgStr);
					System.out.println("Sending: " + msgStr);

					Object lockobj = new Object();
					EventCallback callback = new EventCallback();
					client.sendEventAsync(msg, callback, lockobj);

					synchronized (lockobj) {
						lockobj.wait();
					}
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				System.out.println("Finished.");
			}
		}
	}

}
