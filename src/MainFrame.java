import info.clearthought.layout.TableLayout;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;  


public class MainFrame extends JFrame {
	private static final long serialVersionUID = 2269971701250845501L;
	
	private JMenuBar menuBar;
	private JMenu fileMenu;
	private JMenuItem openFileMenuItem;
	private JMenuItem exitMenuItem;

	private JTabbedPane tabs;
	
	private ASASample sample;
	private JTable sampleTable;
	private AbstractTableModel sampleTableModel;
    
	private XYSeriesCollection sampleGraphDataset;
	
	private JTable sampleParamsTable;
	private AbstractTableModel sampleParamsTableModel;

    private JTable pairedCorelationTable;
    private AbstractTableModel pairedCorelationTableModel;

    private JTable partialCorelationsTable;
    private AbstractTableModel partialCorelationsTableModel;
    private JTable significantPartialCorelationsTable;
    private AbstractTableModel significantPartialCorelationsTableModel;

    private JTable multipleCorelationsTable;
    private AbstractTableModel multipleCorelationsTableModel;

    private JTable regressionCoeficientsTable;
    private AbstractTableModel regressionCoeficientsTableModel;
    private Label ftestLabel = new Label("F Test");

    private JTable regressionLineTable;
    private AbstractTableModel regressionLineTableModel;

    private XYSeriesCollection diagnosticDiagramGraphDataset = new XYSeriesCollection();


    public MainFrame(){
		setTitle("ASA - Lab 2 - Main Window");
		tabs = new JTabbedPane();
		
		//menu
		menuBar = new JMenuBar();
		fileMenu = new JMenu("File");
		openFileMenuItem = new JMenuItem("Open");
		openFileMenuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser openFileChooser = new JFileChooser();
				openFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				openFileChooser.setMultiSelectionEnabled(false);
				// TODO: убрать в релизе
				openFileChooser.setCurrentDirectory(new File("/Users/igorevsukov/Documents/DNU/ASA/data PSA/"));
				if (openFileChooser.showOpenDialog(getParent()) == JFileChooser.APPROVE_OPTION){
					try {
						String fileName = openFileChooser.getSelectedFile().getAbsolutePath();
						BufferedReader input = new BufferedReader(new FileReader(fileName));
						try {
							String line = input.readLine();
							if (line == null) throw new Exception("Can't read data: file "+fileName+"is empty");
							MDObject first_obj = new MDObject(line);
							sample = new ASASample(first_obj.getParams().length);
							sample.add(first_obj);
							while((line = input.readLine()) != null){
								try {
									sample.add(new MDObject(line));
								}catch (Exception ex) {
									System.out.println("can't add object to sample:" + ex.getMessage());
								}
							}
						}
						finally{
							input.close();
							sample.calculateParams();
							sampleTable.tableChanged(null);
							sampleParamsTable.tableChanged(null);
                            pairedCorelationTable.tableChanged(null);
                            partialCorelationsTable.tableChanged(null);
                            significantPartialCorelationsTable.tableChanged(null);
                            multipleCorelationsTable.tableChanged(null);
                            regressionCoeficientsTable.tableChanged(null);
                            regressionLineTable.tableChanged(null);
                            refershDiagnosticDiagramChart();
							setTitle("ASA - Lab 2 - " + fileName);
							ftestLabel.setText(sample.getFTestString());
							//refreshOriginalSampleGraphDataset();
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}

		});

		fileMenu.add(openFileMenuItem);
		fileMenu.addSeparator();
		
		exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		
		fileMenu.add(exitMenuItem);
		menuBar.add(fileMenu);
		
		setJMenuBar(menuBar);

        sampleTableModel = new AbstractTableModel(){
            private static final long serialVersionUID = 1L;

            @Override
            public int getColumnCount() {
                return sample == null ? 0 : sample.getDimension();
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return Double.class;
            }

            @Override
            public String getColumnName(int columnIndex) {
                return String.valueOf(columnIndex);
            }

            @Override
            public int getRowCount() {
                return sample == null ? 0 : sample.size();
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return sample.get(rowIndex).getParam(columnIndex);
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {	return false; }
        };
        //original table
		sampleTable = new JTable(sampleTableModel);
		
		sampleParamsTableModel = new AbstractTableModel(){
			private static final long serialVersionUID = 1L;

			@Override
			public int getColumnCount() {
				if (sample == null) return 0;
				else return sample.getDimension()+1;
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == 0) return String.class;
				else  return Double.class;
			}
			
			@Override
			public String getColumnName(int columnIndex) {
                return columnIndex == 0 ? "" : String.valueOf(columnIndex-1); 
            }

			@Override
			public int getRowCount() {
				if (sample == null) return 0;
				else return 6;
			}

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    switch (rowIndex) {
                        case 0: return "Mean";
                        case 1: return "Dispersion";
                        case 2: return "Min";
                        case 3: return "Max";
                        case 4: return "Assymetry";
                        case 5: return "Excess";
                        default: return null;
                    }
                }
                else {
                    switch (rowIndex) {
                        case 0: return sample.getMean()[columnIndex-1];
                        case 1: return sample.getDispersion()[columnIndex-1];
                        case 2: return sample.getMin()[columnIndex-1];
                        case 3: return sample.getMax()[columnIndex-1];
                        case 4: return sample.getAssymentry()[columnIndex-1];
                        case 5: return sample.getExces()[columnIndex-1];
                        default: return null;
                    }
                }

			}
			
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {	return false; }
		};
		sampleParamsTable = new JTable(sampleParamsTableModel);

        pairedCorelationTableModel = new AbstractTableModel() {
			@Override
			public int getColumnCount() { return sample == null ? 0 : sample.getPairedCorelations().length; }

			@Override
			public Class<?> getColumnClass(int columnIndex) { return String.class; }

			@Override
			public String getColumnName(int columnIndex) { return null; }

			@Override
			public int getRowCount() { return sample == null ? 0 : sample.getPairedCorelations().length; }

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
                Criterion c = sample.getPairedCorelations()[rowIndex][columnIndex];
                if (c.isSignificant())
                    return String.format("%s=%.3f*",c.getName(),c.getValue());
                else
                    return String.format("%s=%.3f ",c.getName(),c.getValue());
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {	return false; }
        };
        pairedCorelationTable = new JTable(pairedCorelationTableModel);

		JPanel samplePanel = new JPanel(new TableLayout(new double[][] {{0.3,20,0.7},{20,0.50,20,0.50}}));
        samplePanel.add(new JLabel("Sample"),"0, 0");
		samplePanel.add(new JScrollPane(sampleTable),"0, 1, 0, 3");
        samplePanel.add(new JLabel("Sample Params"),"2, 0");
		samplePanel.add(new JScrollPane(sampleParamsTable),"2, 1");
        samplePanel.add(new JLabel("Paired Corelations"),"2, 2");
        samplePanel.add(new JScrollPane(pairedCorelationTable),"2, 3");
		tabs.add("Sample",samplePanel);

        partialCorelationsTableModel = new AbstractTableModel() {
			@Override
			public int getColumnCount() { return sample == null ? 0 : sample.getPartialCorelations().length; }

			@Override
			public Class<?> getColumnClass(int columnIndex) { return String.class; }

			@Override
			public String getColumnName(int columnIndex) { return null; }

			@Override
			public int getRowCount() { return sample == null ? 0 : sample.getPartialCorelations().length; }

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
                Criterion c = sample.getPartialCorelations()[rowIndex][columnIndex];
                if (c.isSignificant())
                    return String.format("%s=%.3f*",c.getName(),c.getValue());
                else
                    return String.format("%s=%.3f ",c.getName(),c.getValue());
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {	return false; }
        };
        partialCorelationsTable = new JTable(partialCorelationsTableModel);

        significantPartialCorelationsTableModel = new AbstractTableModel() {
        	@Override
			public int getColumnCount() { return sample == null ? 0 : 6; }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return String.class;
                    case 5: return String.class;
                    case 4: return Boolean.class;
                    default: return Double.class;
                }
			}

			@Override
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "Name";
                    case 1: return "Value";
                    case 2: return "Staticstic";
                    case 3: return "Quantile";
                    case 4: return "Significant?";
                    case 5: return "Confidient Limits";
                    default: return "";
                }
            }

			@Override
			public int getRowCount() { return sample == null ? 0 : sample.getSignificantPartialCorelations().size(); }

			@Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Criterion c = sample.getSignificantPartialCorelations().get(rowIndex);
                switch (columnIndex) {
                    case 0: return c.getName();
                    case 1: return c.getValue();
                    case 2: return c.getStatistic();
                    case 3: return c.getQuantile();
                    case 4: return c.isSignificant();
                    case 5: return c.getConfidienceLimits();
                    default: return null;
                }
            }

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {	return false; }
        };
        significantPartialCorelationsTable = new JTable(significantPartialCorelationsTableModel);

        JPanel partialCorelationsPanel = new JPanel(new TableLayout(new double[][] {{TableLayout.FILL},{20,0.50,20,0.50}}));
        partialCorelationsPanel.add(new JLabel("Partial Corelations"),"0, 0");
		partialCorelationsPanel.add(new JScrollPane(partialCorelationsTable),"0, 1");
        partialCorelationsPanel.add(new JLabel("Significant Paired Corelations"),"0, 2");
        partialCorelationsPanel.add(new JScrollPane(significantPartialCorelationsTable),"0, 3");
        tabs.add("Partial Corelations",partialCorelationsPanel);


        multipleCorelationsTableModel = new AbstractTableModel() {
        	@Override
			public int getColumnCount() { return sample == null ? 0 : 5; }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return Integer.class;
                    case 5: return String.class;
                    case 4: return Boolean.class;
                    default: return Double.class;
                }
			}

			@Override
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "i";
                    case 1: return "Value";
                    case 2: return "Staticstic";
                    case 3: return "Quantile";
                    case 4: return "Significant?";
                    default: return "";
                }
            }

			@Override
			public int getRowCount() { return sample == null ? 0 : sample.getMultipleCorelations().length; }

			@Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Criterion c = sample.getMultipleCorelations()[rowIndex];
                switch (columnIndex) {
                    case 0: return rowIndex;
                    case 1: return c.getValue();
                    case 2: return c.getStatistic();
                    case 3: return c.getQuantile();
                    case 4: return c.isSignificant();
                    default: return null;
                }
            }

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {	return false; }
        };
        multipleCorelationsTable = new JTable(multipleCorelationsTableModel);
        tabs.add("Multiple Corelations",new JScrollPane(multipleCorelationsTable));


        regressionCoeficientsTableModel = new AbstractTableModel() {
        	@Override
			public int getColumnCount() { return sample == null ? 0 : 8; }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return String.class;
                    case 7: return String.class;
                    case 6: return Boolean.class;
                    default: return Double.class;
                }
			}

			@Override
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "Name";
                    case 1: return "Value";
                    case 2: return "Standartized";
                    case 3: return "Dispersion";
                    case 4: return "Staticstic";
                    case 5: return "Quantile";
                    case 6: return "Significant?";
                    case 7: return "Confidient Limits";
                    default: return "";
                }
            }

			@Override
			public int getRowCount() { return sample == null ? 0 : sample.getRegressionCoeficients().length; }

			@Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Criterion c = sample.getRegressionCoeficients()[rowIndex];
                switch (columnIndex) {
                    case 0: return c.getName();
                    case 1: return c.getValue();
                    case 2: return c.getStandartized();
                    case 3: return c.getDispersion();
                    case 4: return c.getStatistic();
                    case 5: return c.getQuantile();
                    case 6: return c.isSignificant();
                    case 7: return c.getConfidienceLimits();
                    default: return null;
                }
            }

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {	return false; }
        };
        regressionCoeficientsTable = new JTable(regressionCoeficientsTableModel);
        JPanel regressionCoeficientsPanel = new JPanel(new TableLayout(new double[][] {{TableLayout.FILL},{20,TableLayout.FILL}}));
        regressionCoeficientsPanel.add(ftestLabel, "0, 0");
        regressionCoeficientsPanel.add(new JScrollPane(regressionCoeficientsTable), "0, 1");
        tabs.add("Regression Params", regressionCoeficientsPanel);

        regressionLineTableModel = new AbstractTableModel() {
        	@Override
			public int getColumnCount() { return sample == null ? 0 : 5; }

			@Override
			public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: return Integer.class;
                    default: return Double.class;
                }
			}

			@Override
            public String getColumnName(int columnIndex) {
                switch (columnIndex) {
                    case 0: return "i";
                    case 1: return "y[i]";
                    case 2: return "y^(x1i,x2i,..xni)";
                    case 3: return "bottom y^(x1i,x2i,..xni)";
                    case 4: return "top y^(x1i,x2i,..xni)";
                    default: return "";
                }
            }

			@Override
			public int getRowCount() { return sample == null ? 0 : sample.size(); }

			@Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Criterion c = sample.getRegressionLine()[rowIndex];
                switch (columnIndex) {
                    case 0: return rowIndex;
                    case 1: return sample.getParam(rowIndex, sample.getDimension()-1);
                    case 2: return c.getValue();
                    case 3: return c.getBottomConfidienceLimit();
                    case 4: return c.getTopConfidienceLimit();
                    default: return null;
                }
            }

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {	return false; }
        };
        regressionLineTable = new JTable(regressionLineTableModel);
        tabs.add("Regression Line", new JScrollPane(regressionLineTable));

        JFreeChart diagnosticDiagramChart = ChartFactory.createScatterPlot("Diagnostic Diagram", "", "", diagnosticDiagramGraphDataset, PlotOrientation.VERTICAL, true, true, false);
		ChartPanel diagnosticDiagramChartPanel = new ChartPanel(diagnosticDiagramChart);
        tabs.add("Diagnostic Diagram", diagnosticDiagramChartPanel);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabs, BorderLayout.CENTER);
	}
	
	public void refreshOriginalSampleGraphDataset() {
//		sampleGraphDataset.removeAllSeries();
//		XYSeries series = new XYSeries("sample");
//		for (int i = 0; i < sample.size(); i++) {
//			series.add(sample.get(i).getParam(0), sample.get(i).getParam(1));
//		}
//		sampleGraphDataset.addSeries(series);
	}

    public void refershDiagnosticDiagramChart() {
        diagnosticDiagramGraphDataset.removeAllSeries();
        XYSeries diagnosticDiagram = new XYSeries("Diagnostic Diagram");
        for (int i=0; i< sample.size(); i++) {
            double yi = sample.getParam(i, sample.getDimension()-1); //y[i]
            double eps = yi;
            for (int j=0; j<(sample.getDimension()-1); j++)
                eps -= sample.getRegressionCoeficients()[j].getValue()*sample.getParam(i, j);

            diagnosticDiagram.add(yi, eps);
        }
        diagnosticDiagramGraphDataset.addSeries(diagnosticDiagram);
    }

}
