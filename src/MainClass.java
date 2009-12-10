import javax.swing.JFrame;


public class MainClass {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
    	MainFrame mainWindow = new MainFrame();
    	mainWindow.setLocation(100, 100);
    	mainWindow.setSize(1000, 600);
    	mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	mainWindow.setVisible(true);


//        HashSet<Integer> set1 = new HashSet<Integer>();
//        set1.add(1);
//        set1.add(2);
//        set1.add(3);
//
//        HashSet<Integer> set2 = (HashSet<Integer>)set1.clone();
//        set2.add(4);
//
//        set1.remove(2);
//
//        System.out.println("set1="+set1);
//        System.out.println("set2="+set2);
	}

}
