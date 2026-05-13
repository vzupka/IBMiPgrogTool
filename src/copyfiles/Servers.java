package copyfiles;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 *
 * @author Vladimír Župka 2025
 */
public class Servers extends JDialog {
    
    // Path to IBMiServersList.lib file
    Path serversPath = Paths.get(System.getProperty("user.dir"), "workfiles", "IBMiServersList.lib");

    DefaultTableModel tableModel;
    JTable table;
    
    // Model for selection of rows in the table
    ListSelectionModel rowSelectionModel;
    ListSelectionModel rowIndexList;
    int rowIndex;
    
    JTextField tfName = new JTextField("");
    JTextField tfText = new JTextField("");

    JScrollPane scrollPane;

    JButton setSelected = new JButton("Set selected");
    JButton remove = new JButton("Remove selected");
    JButton addNew = new JButton("Add new");
    JButton close = new JButton("Close");

    JLabel portLabel = new JLabel("Port: ");
    JTextField tfPort = new JTextField("");
    
    JLabel sslTypeLabel = new JLabel("SSL Type: ");
    JComboBox sslTypeBox = new JComboBox();
    String selectedText;
    
    int windowWidth = 550;
    int windowHeight = 285;

    int scrollWidth = 540;

    static final Color GRAY_LIGHTER = Color.getHSBColor(0.25f, 0.015f, 0.95f);
    static final Color DIM_BLUE = new Color(50, 60, 160);

    String hostName;  // IP address or DNS name
    String hostText;  // text for host
    Vector vector;  // necessary for reading from table


    // Constant for properties
    final String PROP_COMMENT = "Copy files between IBM i and PC, edit and compile.";
    static Path parPath = Paths.get(System.getProperty("user.dir"), "paramfiles", "Parameters.txt");
    static String encoding = System.getProperty("file.encoding");

    Properties properties = new Properties();
    BufferedReader infile;
    BufferedWriter outfile;
            
    /**
     * Constructor
     */
    Servers(MainWindow mainWindow) {
        super();
        super.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        this.setTitle("IBM i Servers");

        // Register WindowListener for closing Servers window (to save PORT property)
        this.addWindowListener(new ServersWindowAdapter());

        properties = new Properties();
        try {
            // Get values from properties and set variables and text fields
            infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
            properties.load(infile);
        } catch (IOException exc) {
            exc.printStackTrace();
        }
        
        hostName = mainWindow.hostTextField.getText();  // from the input field
        
        // Table model and table definition
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableModel.addColumn("IP address or DNS");  // column header
        tableModel.addColumn("Text description");  // column header
        JTableHeader header = table.getTableHeader();
        header.setBackground(GRAY_LIGHTER);
        header.setForeground(DIM_BLUE);
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 30));
        
        // scroll pane with the table
        scrollPane = new JScrollPane(table);
        
        // Data panel
        JPanel dataPanel = new JPanel();
        dataPanel.setLayout(new BoxLayout(dataPanel, BoxLayout.PAGE_AXIS));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        dataPanel.add(scrollPane);

        // Button panel for table
        JPanel buttonPanelTable = new JPanel();
        buttonPanelTable.setLayout(new BoxLayout(buttonPanelTable, BoxLayout.LINE_AXIS));
        buttonPanelTable.add(setSelected);
        setSelected.setToolTipText("Data from the selected row is copied to input fields"
                + " as well as to the main window.");

        buttonPanelTable.add(close);

        // Text field panel
        JPanel textFieldPanel =  new JPanel();
        textFieldPanel.setLayout(new BoxLayout(textFieldPanel, BoxLayout.LINE_AXIS));
        tfName.setToolTipText("Enter IP address or DNS to add.");
        tfName.setMaximumSize(new Dimension(scrollWidth/2, 30));
        tfText.setToolTipText("Enter Text description to add.");
        tfText.setMaximumSize(new Dimension(scrollWidth/2, 30));
        tfName.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        tfText.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        textFieldPanel.add(tfName);
        textFieldPanel.add(tfText);

        // Button panel for Port and SSL type
        JPanel buttonPanelSSL = new JPanel();
        buttonPanelSSL.setLayout(new BoxLayout(buttonPanelSSL, BoxLayout.LINE_AXIS));
        buttonPanelSSL.add(addNew);
        addNew.setToolTipText("Address and Text from input fields are added"
                + " if not present in tne list.");
        buttonPanelSSL.add(remove);
        buttonPanelSSL.add(new JLabel("         "));
        buttonPanelSSL.add(portLabel);
        tfPort.setMinimumSize(new Dimension(50, 20));
        tfPort.setMaximumSize(new Dimension(50, 20));
        buttonPanelSSL.add(tfPort);
        selectedText = properties.getProperty("PORT");
        tfPort.setText(selectedText);

        String SSL_TYPE_NONE = "NONE";
        String SSL_TYPE_SSLv2 = "SSLv2";
        String SSL_TYPE_SSLv3 = "SSLv3";
        String SSL_TYPE_TLS = "TLS";
        String[] SSL_TYPES = {SSL_TYPE_NONE,
            SSL_TYPE_SSLv2,
            SSL_TYPE_SSLv3,
            SSL_TYPE_TLS};     
        for (String SSL_TYPE : SSL_TYPES) {
            sslTypeBox.addItem(SSL_TYPE);
        }
        selectedText = properties.getProperty("SSL_TYPE");  // Gets property
        sslTypeBox.setSelectedItem(selectedText);

        buttonPanelSSL.add(new JLabel("  "));
        buttonPanelSSL.add(sslTypeLabel);
        sslTypeBox.setMaximumSize(new Dimension(100, 20));
        buttonPanelSSL.add(sslTypeBox);

        // Overall panel
        JPanel overallPanel = new JPanel();
        // layout
        GroupLayout layout = new GroupLayout(overallPanel);
        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout
                                .createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(dataPanel)
                                .addGap(4)
                                .addComponent(buttonPanelTable)
                                .addComponent(textFieldPanel)
                                .addGap(10)
                                .addComponent(buttonPanelSSL)
                                .addGap(10)
                        )
        );
        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(dataPanel)
                                .addGap(4)
                                .addComponent(buttonPanelTable) 
                                .addComponent(textFieldPanel)
                                .addGap(10)
                                .addComponent(buttonPanelSSL)
                                .addGap(10)
                        )
        );
        overallPanel.setLayout(layout);

        // Fill table with host names from the file "IBMiServersList.lib"
        try {
            List<String> lines = Files.readAllLines(serversPath);
            for  (int idx = 0; idx < lines.size(); idx++) {
                //Object[] arr = lines.get(idx).split(",");
                tableModel.addRow(lines.get(idx).split(","));
            }
        } catch (IOException ioe) {
        }

        // Create and register row selection model (for selecting a single row)
        rowSelectionModel = table.getSelectionModel();
        rowSelectionModel.addListSelectionListener(lsl -> {
            rowIndexList = (ListSelectionModel) lsl.getSource();
        });
        
        // "Close" button listener
        close.addActionListener(al -> {
            // Update properties from Library, File, Member input fields
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("PORT", tfPort.getText());
                properties.setProperty("SSL_TYPE", selectedText);
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
            this.dispose();
        });

        setSelected.addActionListener(al -> {
            rowIndex = rowIndexList.getLeadSelectionIndex();
            //if (rowIndex >= 0) {
                vector = tableModel.getDataVector().elementAt(rowIndex);  // table row
                hostName = vector.elementAt(0).toString();  // column 0 to String
                hostText = vector.elementAt(1).toString();  // column 1 to String
                tableModel.setValueAt(hostName, rowIndex, 0);  // set column 0
                tableModel.setValueAt(hostText, rowIndex, 1);  // set column 1
                mainWindow.hostTextField.setText(hostName);
                tfName.setText(hostName);
                tfText.setText(hostText);
                buildHostNameFile();  // Write file of host names 
            //}
        });

        // "Remove" button listener
        remove.addActionListener(al -> {
            rowIndex = rowIndexList.getLeadSelectionIndex();
            tableModel.removeRow(rowIndex);  // remove from the table
            buildHostNameFile();  // Write file of host names
        });

        // "Add new" button listener
        addNew.addActionListener(al -> {
            //hostName = mainWindow.hostTextField.getText();  // from the input field
            hostName = tfName.getText();  // from the input field
            hostText = tfText.getText();  // from the input field
            // Check if the host name is not already in the table
            int lastIndex = table.getRowCount();
            boolean foundInList = false;
            // Find out if name  matches any list item
            for (int idx = 0; idx < lastIndex; idx++) {
                vector = tableModel.getDataVector().elementAt(idx);  // table row
                String str1 = vector.elementAt(0).toString();  // column 0 to String
                String str2 = vector.elementAt(1).toString();  // column 1 to String
                if (str1.equals(hostName) & str2.equals(hostText)  // both equal or 
                        || hostName.equals("") & hostText.equals("")) {  // both blank 
                    foundInList = true;  // are not added
                }
            }
            // If the host name is not in the table, add it at the end.
            if (!foundInList) {
                //Object[] arr = { hostName, vector.elementAt(1).toString() };
                Object[] arr = { hostName, hostText };
                tableModel.addRow(arr);
            }
            buildHostNameFile();  // Write file of host names 
        });

        tfPort.addActionListener((ActionEvent al) -> {

            // Update property PORT
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("PORT", tfPort.getText());
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });
                
        // "sslType" combo box listener
        sslTypeBox.addItemListener(il -> {
            JComboBox<String[]> source = (JComboBox) il.getSource();
            selectedText = (String) source.getSelectedItem();            

            // Update propertty SSL_TYPE
            try {
                infile = Files.newBufferedReader(parPath, Charset.forName(encoding));
                properties.load(infile);
                infile.close();
                properties.setProperty("SSL_TYPE", selectedText);
                outfile = Files.newBufferedWriter(parPath, Charset.forName(encoding));
                properties.store(outfile, PROP_COMMENT);
                outfile.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        });

        
        // Complete window building
        Container cont = getContentPane();
        cont.add(overallPanel);
        setSize(windowWidth, windowHeight);
        setLocation(mainWindow.mainWindowX, mainWindow.mainWindowY + 90);
        setVisible(true);
        pack();
    }  // End of Constructor
    
    /**
     * Write host names and texts to text file "IBMiServersList.lib"
     */
    void buildHostNameFile() {
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(serversPath, StandardCharsets.UTF_8)) {
                for (int idx = 0; idx < tableModel.getRowCount(); idx++) {
                    //((Vector)getDataVector().elementAt(idx)).elementAt(0);
                    vector = tableModel.getDataVector().elementAt(idx);  // table row
                    hostName = vector.elementAt(0).toString();  // column 0 host name
                    hostText = vector.elementAt(1).toString();  // column 1 host text
                    writer.write(hostName + ',' + hostText + '\n');  // append "new line" character
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
        }
    }

    /**
     * Window adapter clicks button Close and disposes the window.
     */
    class ServersWindowAdapter extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent swa) {
            close.doClick();
            swa.getWindow().dispose();
        }
    }

}
