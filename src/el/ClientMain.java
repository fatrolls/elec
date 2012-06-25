
package el;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;

import el.serv.ServerThread;

public class ClientMain {
	
	public static final String SHIP1_IMAGE = "/img/ship1.png";
	public static final String TILE1_IMAGE = "/img/tile1.png";

	private static final PrintStream out = System.out;
	private static final Map<String, Image> images = new HashMap<String, Image>();
	private static final Model model = new Model();
	
	private static JFrame frame;
	private static Timer timer;
	private static ServerThread server;

	public static void main(String[] args) {

		out.println("dir: " + System.getProperty("user.dir"));

		// final JCView v = new JCView(m);
		final CanvasView v = new CanvasView(model);

		final JPanel p = new JPanel(new BorderLayout());
		p.add(v, BorderLayout.CENTER);
		
		JFrame f = new JFrame();
		f.setTitle("Electron");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setJMenuBar(createMenuBar());
		f.setContentPane(p);
		f.pack();
		f.setVisible(true);
		frame = f;

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				// do initial paint and print some information
				v.paintImmediately(0, 0, v.getWidth(), v.getHeight());
				System.out.println("panel double buffered: " + p.isDoubleBuffered());
				System.out.println("panel opaque: " + p.isOpaque());
				System.out.println("panel optimized: " + p.isOptimizedDrawingEnabled());
				System.out.println("panel lightweight: " + p.isLightweight());
				System.out.println("view double buffered: " + v.isDoubleBuffered());
				System.out.println("view opaque: " + v.isOpaque());
				BufferStrategy bs = v.getBufferStrategy();
				System.out.println("canvas view buffer strategy: " + bs.getClass());
				BufferCapabilities c = bs.getCapabilities();
				System.out.println("canvas view buffer full screen required: " + c.isFullScreenRequired());
				System.out.println("canvas view buffer multi buffer: " + c.isMultiBufferAvailable());
				System.out.println("canvas view buffer page flipping: " + c.isPageFlipping());
				System.out.println("canvas view buffer back buffer accel: " + c.getBackBufferCapabilities().isAccelerated());
				System.out.println("canvas view buffer front buffer accel: " + c.getFrontBufferCapabilities().isAccelerated());
			}
		});

		timer = new Timer(25, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.update();
				// should skip this if behind
				v.paintImmediately(0, 0, v.getWidth(), v.getHeight());
			}
		});
		
		timer.start();
	}

	private static JMenuBar createMenuBar() {
		JMenuItem connectMenuItem = new JMenuItem("Connect (localhost/8111)");
		connectMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Socket socket = new Socket("localhost", 8111);
					ServerThread server = new ServerThread(socket, model);
					server.start();
					model.setServer(server);
					ClientMain.server = server;
					
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
		JMenuItem enterReqMenuItem = new JMenuItem("Request Enter");
		enterReqMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (server != null) {
					server.enterReq();
				} else {
					JOptionPane.showMessageDialog(frame, "Not connected");
				}
			}
		});
		
		JMenuItem specMenuItem = new JMenuItem("Spectate");
		specMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (server != null) {
					server.specReq();
					// wait for server?
					model.spec();
				} else {
					JOptionPane.showMessageDialog(frame, "Not connected");
				}
			}
		});
		
		JMenuItem enterMenuItem = new JMenuItem("Enter");
		enterMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				model.enter(-1);
			}
		});

		JMenu serverMenu = new JMenu("Server");
		serverMenu.add(connectMenuItem);
		serverMenu.add(enterReqMenuItem);
		serverMenu.add(specMenuItem);
		
		JMenu localMenu = new JMenu("Local");
		localMenu.add(enterMenuItem);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(serverMenu);
		menuBar.add(localMenu);
		return menuBar;
	}

	/**
	 * Increase frame rate
	 */
	static void faster() {
		timer.setDelay(timer.getDelay() - 1);
	}

	/**
	 * Reduce frame rate
	 */
	static void slower() {
		timer.setDelay(timer.getDelay() + 1);
	}

	/**
	 * Get delay between frames
	 */
	static int getDelay() {
		return timer.getDelay();
	}

	/**
	 * Get cached image
	 */
	public static Image getImage(String name) {
		Image image = images.get(name);
		if (image == null) {
			URL u = ClientMain.class.getResource(name);
			System.out.println("loading image " + u);
			try {
				BufferedImage i = ImageIO.read(u);
				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
				GraphicsDevice gd = ge.getDefaultScreenDevice();
				GraphicsConfiguration gc = gd.getDefaultConfiguration();
				image = gc.createCompatibleImage(i.getWidth(), i.getHeight(), Transparency.BITMASK);
				image.getGraphics().drawImage(i, 0, 0, null);
				images.put(name, image);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return image;
	}
}
