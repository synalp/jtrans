package fr.xtof54.jtrserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.FileOutputStream;

public class JTrServer {
	public static void main(String args[]) {
		Thread listenth = new Thread(new Runnable() {
			public void run() {
				try {
					final int port = 4539;
					ServerSocket serverSocket = new ServerSocket(port);
					System.out.println("detjtrapp waiting for server app ");
					Socket clientSocket = serverSocket.accept();
					System.out.println("detjtrapp got server app ");
					DataInputStream f = new DataInputStream(clientSocket.getInputStream());
					int nfichs = f.readInt();
					System.out.println("detjtrapp found "+nfichs+" files");
					byte[] buf = new byte[1024];
					for (int i=0;i<nfichs;i++) {
						String gnom = f.readUTF();
						System.out.println("detjtrapp loading "+gnom);
						FileOutputStream g = new FileOutputStream(gnom);
						for (;;) {
							int nb = f.readInt();
							if (nb<=0) break;
							for (;;) {
								int m=f.read(buf,0,nb);
								if (m>0) {
									g.write(buf,0,m);
									nb-=m;
									if (nb<=0) break;
								}
							}
						}
						g.close();
					}
					f.close();
					System.out.println("all is loaded");
				} catch (Exception e) {
					System.out.print("detjtrapp listenPort exception");
					e.printStackTrace();
				}
			}});
		listenth.start();
	}
}
