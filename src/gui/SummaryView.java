package gui;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import support.FileDrop;
import synchive.Globals;

/**
 * View displaying source & destination location as well as program status.
 * 
 * @author Tony Hsu
 * @structure Handles user input and notifies controller of event
 */
public class SummaryView
{
    /**
     * Delegate methods for SummaryView
     */
    public interface SummaryViewDelegate {
        /**
         * Event notifier if text changed for source text field
         * @param label JTextField of text changed
         * @param text New text
         */
        public void sourceTextChanged(JTextField label, String text);
        /**
         * Event notifier if text changed for destination text field
         * @param label JTextField of text changed
         * @param text New text
         */
        public void destinationTextChanged(JTextField label, String text);
    }
    
    /**
     * Common methods for controller to implement to handle events
     */
    private SummaryViewDelegate delegate;
    /**
     * SummaryView's frame
     */
    private JFrame synchiveFrame;
    /**
     * Source location's textField
     */
    private JTextField sourceTextField;
    /**
     * Destination location's textField
     */
    private JTextField destinationTextField;
    /**
     * Running Time Label
     */
    private JLabel totalRunningTimeLabel;
    /**
     * Current Status of Program
     */
    private JLabel statusLabel;
    
    /**
     * Initialize the view.
     * @param delegate Controller to handle events
     */
    public SummaryView(SummaryViewDelegate delegate)
    {
        this.delegate = delegate;
        initialize();
    }
    
    /**
     * Initialize the contents of the view.
     */
    private void initialize()
    {
        synchiveFrame = new JFrame();
        synchiveFrame.setTitle("Synchive " + Globals.VERSION);
        synchiveFrame.setBounds(100, 100, 526, 322);
        synchiveFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        JLabel sourceLabel = new JLabel("Source:");
        sourceLabel.setPreferredSize(new Dimension(57, 14));
        sourceLabel.setMinimumSize(new Dimension(57, 14));
        sourceLabel.setMaximumSize(new Dimension(57, 14));
        sourceLabel.setBounds(7, 7, 67, 22);
        sourceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        sourceLabel.setFont(new Font("Arial", Font.BOLD, 11));
        
        sourceTextField = new JTextField();
        sourceTextField.setBounds(93, 7, 352, 22);
        sourceTextField.setMinimumSize(new Dimension(50, 20));
        sourceTextField.setToolTipText("");
        sourceTextField.setColumns(10);
        sourceTextField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                delegate.sourceTextChanged(sourceTextField, sourceTextField.getText());
            }
            
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                delegate.sourceTextChanged(sourceTextField, sourceTextField.getText());
            }
            
            @Override
            public void changedUpdate(DocumentEvent e)
            {
            }
        });
        new FileDrop(sourceTextField, new FileDrop.Listener()
        {
            @Override
            public void filesDropped(File[] files)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        sourceTextField.setText(files[0].getPath());
                    }
                });
            } // end filesDropped
        }); // end FileDrop.Listener
        
        JButton sourceMoreButton = new JButton("...");
        sourceMoreButton.setBounds(463, 7, 40, 22);
        sourceMoreButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        sourceMoreButton.setMinimumSize(new Dimension(23, 23));
        sourceMoreButton.setMaximumSize(new Dimension(45, 22));
        sourceMoreButton.setDefaultCapable(false);
        sourceMoreButton.setFocusPainted(false);
        sourceMoreButton.setEnabled(false);
        
        JLabel destinationLabel = new JLabel("Destination:");
        destinationLabel.setBounds(7, 33, 67, 22);
        destinationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        destinationLabel.setFont(new Font("Arial", Font.BOLD, 11));
        
        destinationTextField = new JTextField();
        destinationTextField.setBounds(93, 33, 352, 22);
        destinationTextField.setMinimumSize(new Dimension(50, 20));
        destinationTextField.setColumns(10);
        destinationTextField.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                delegate.destinationTextChanged(destinationTextField, destinationTextField.getText());
            }
            
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                delegate.destinationTextChanged(destinationTextField, destinationTextField.getText());
            }
            
            @Override
            public void changedUpdate(DocumentEvent e)
            {
            }
        });
        new FileDrop(destinationTextField, new FileDrop.Listener()
        {
            @Override
            public void filesDropped(File[] files)
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        destinationTextField.setText(files[0].getPath());
                    }
                });
            } // end filesDropped
        }); // end FileDrop.Listener
        
        JButton destinationMoreButton = new JButton("...");
        destinationMoreButton.setBounds(463, 33, 40, 22);
        destinationMoreButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        destinationMoreButton.setMinimumSize(new Dimension(23, 23));
        destinationMoreButton.setMaximumSize(new Dimension(45, 22));
        destinationMoreButton.setDefaultCapable(false);
        destinationMoreButton.setFocusTraversalKeysEnabled(false);
        destinationMoreButton.setFocusPainted(false);
        destinationMoreButton.setEnabled(false);
        
        statusLabel = new JLabel("Status: Waiting");
        statusLabel.setBounds(7, 258, 113, 14);
        synchiveFrame.getContentPane().add(statusLabel);
        
        totalRunningTimeLabel = new JLabel("Running Time:");
        totalRunningTimeLabel.setBounds(130, 258, 151, 14);
        synchiveFrame.getContentPane().add(totalRunningTimeLabel);
        
        synchiveFrame.getContentPane().setLayout(null);
        synchiveFrame.getContentPane().add(sourceLabel);
        synchiveFrame.getContentPane().add(sourceTextField);
        synchiveFrame.getContentPane().add(sourceMoreButton);
        synchiveFrame.getContentPane().add(destinationLabel);
        synchiveFrame.getContentPane().add(destinationTextField);
        synchiveFrame.getContentPane().add(destinationMoreButton);
    }
    
    // ~~~~~ Getters & Setters ~~~~~~ //
    /**
     * @return Frame window of view
     */
    public JFrame getFrame()
    {
        return synchiveFrame;
    }
    
    /**
     * Set the visibility of the window
     * @param flag True for hiding, false for showing
     */
    public void setVisible(boolean flag) 
    {
        synchiveFrame.setVisible(flag);
    }
    
    /**
     * Set the running progress status
     * @param str Status to display. "Status: (Type)"
     */
    public void setStatus(String str) 
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                statusLabel.setText(str);
            }
        });
    }
    
    /**
     * Set the running time
     * @param str Time to display. "Running Time - (Time)"
     */
    public void setRunningTime(String str)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                totalRunningTimeLabel.setText(str);
            }
        });
    }
    
    /**
     * Set the source and destination location
     * @param sourceText Source path
     * @param destinationText Destination path
     */
    public void loadSettings(String sourceText, String destinationText)
    {
        sourceTextField.setText(sourceText);
        destinationTextField.setText(destinationText);
    }
}
