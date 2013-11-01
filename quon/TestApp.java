package quon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * JavaQuON
 * 
 * A small test application, where N JavaQuON nodes are started with entities moving randomly
 * inside a rectangular region (bouncing of the borders). The real positions of all nodes is
 * denoted by gray circles, the position of entity 0 is denoted by a blue circle (with a blue
 * box illustrating its area of interest) and the (assumed) positions of all entities that
 * entity 0 retrieves via JavaQuON are denoted by red circles. 
 */
public class TestApp {
	public static void main(String[] args) {
		InetAddress localhost = null;
		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		}
		
		int N = 30;
		int width  = 800;
		int height = 600;
		int aoiRadius = 100;
		
		Random random = new Random();
		
		Entity entity[] = new Entity[N];
		Node node[] = new Node[N];
		for (int i = 0; i < N; i++) {
			entity[i] = new Entity(new Identifier(localhost, 10000 + i));
			entity[i].position.x = random.nextInt(width - 20) + 10;
			entity[i].position.y = random.nextInt(height - 20) + 10;
			entity[i].aoiRadius = aoiRadius;
			node[i] = new Node(entity[i]);
			if (i == 0)
				node[i].join(null);
			else
				node[i].join(entity[i-1].identifier);
		}
		
		JFrame frame = new JFrame("Singularity Test");
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(width,height));
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		int dx[] = new int[N];
		int dy[] = new int[N];
		
		for (int i = 0; i < N; i++) {
			dx[i] = (random.nextInt(2) == 0) ? - 1 : 1;
			dy[i] = (random.nextInt(2) == 0) ? - 1 : 1;
		}
		
		while(true) {
			Graphics g = panel.getGraphics();
			g.setColor(Color.black);
			g.fillRect(0, 0, width, height);
			
			g.setColor(Color.blue);
			int aoi = (int)entity[0].aoiRadius;
			int x = (int) entity[0].position.x;
			int y = (int) entity[0].position.y;
			g.fillOval(x - 8, y - 8, 16, 16);
			g.drawRect(x - aoi, y - aoi, 2 * aoi, 2 * aoi);
			
			g.setColor(Color.gray);
			for (int i = 1; i < N; i++) {
				int xe = (int) entity[i].position.x;
				int ye = (int) entity[i].position.y;
				g.fillOval(xe - 8, ye - 8, 16, 16);
			}
			
			g.setColor(Color.red);
			for (Entity visibleEntity : node[0].entitiesInAoi()) {
				g.fillOval((int)visibleEntity.position.x - 8, (int)visibleEntity.position.y - 8, 16, 16);
			}
			
			for (int i = 0; i < N; i++) {
				if ((entity[i].position.x == 1) || ((entity[i].position.x == width-1)))
					dx[i] = -dx[i];
				if ((entity[i].position.y == 1) || ((entity[i].position.y == height-1)))
					dy[i] = -dy[i];
				
				entity[i].position.x += dx[i];
				entity[i].position.y += dy[i];
			}
			
			try {
				Thread.sleep(30);
			} catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
}
