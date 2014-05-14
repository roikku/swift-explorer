/*
 * Copyright 2014 Loic Merckel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.swiftexplorer.gui.diff;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.gui.MainPanel;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.util.FileTypeIconFactory;
import org.swiftexplorer.gui.util.PopupTrigger;
import org.swiftexplorer.gui.util.ReflectionAction;
import org.swiftexplorer.swift.operations.DifferencesFinder;
import org.swiftexplorer.swift.operations.SwiftOperations;
import org.swiftexplorer.swift.operations.SwiftOperations.ComparisonItem;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.Pair;

public class DifferencesManagementPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final Logger logger = LoggerFactory.getLogger(DifferencesManagementPanel.class);
	
	private final Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> >  discrepancies ;
	
    private final DefaultListModel<ComparisonItem> localOnlyObjects = new DefaultListModel<ComparisonItem>();
    private final JList<ComparisonItem> localOnlyObjectsList = new JList<ComparisonItem>(localOnlyObjects);
    
    private final DefaultListModel<ComparisonItem> localAndRemoteDifferObjects = new DefaultListModel<ComparisonItem>();
    private final JList<ComparisonItem> localAndRemoteDifferObjectsList = new JList<ComparisonItem>(localAndRemoteDifferObjects);
    
    private final DefaultListModel<ComparisonItem> remoteOnlyObjects = new DefaultListModel<ComparisonItem>();
    private final JList<ComparisonItem> remoteOnlyObjectsList = new JList<ComparisonItem>(remoteOnlyObjects);
	
	private int colWidth = 200 ;
	private int listHeight = 400 ;
	
	private final Container container ;
	private final StoredObject srcStoredObject ;
	private final File srcFile ;
    private final SwiftOperations ops;
    private final SwiftCallback callback;
	
	private final HasLocalizedStrings stringsBundle ;
 	
	private Action localUploadAction = null ;
    private Action remoteDownloadAction = null ;
    private Action remoteDeleteAction = null ;
    private Action diffOverwriteAction = null ;
    
    private JButton closeButton = null ;
    private Action closeAction = null ;

    private final JDialog owner;
    
    /*
    private JButton localUploadButton = null ;
    private JButton remoteDownloadButton = null ;
    private JButton remoteDeleteButton = null ;
    private JButton diffOverwriteButton = null ;
    */
    
	public DifferencesManagementPanel (Container container, StoredObject srcStoredObject, File srcFile, Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> >  discrepancies, SwiftOperations ops, SwiftCallback callback, HasLocalizedStrings stringsBundle, JDialog owner, Dimension parentDim)
	{
		super (new BorderLayout(0, 0)) ;
		if (discrepancies == null)
			throw new IllegalArgumentException () ;
		
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		
		this.owner = owner ;
		this.discrepancies = discrepancies ;
		this.stringsBundle = stringsBundle ;
		this.container = container ;
		this.srcStoredObject = srcStoredObject ;
		this.srcFile = srcFile ;
		this.ops = ops ;
		this.callback = callback ;

		if (parentDim != null)
		{
			this.colWidth = (int) Math.max(colWidth, (0.8 * (parentDim.getWidth())) / 3.0 - 20) ;
			this.listHeight = (int) Math.min(listHeight, parentDim.getHeight() - 50) ;
		}
		
		initPanel () ;
	}
	
	
	private void initPanel ()
	{
		Box mainBox = Box.createHorizontalBox();
		Box boxCol1 = Box.createVerticalBox();
		Box boxCol2 = Box.createVerticalBox();
		Box boxCol3 = Box.createVerticalBox();
		
		localUploadAction = new ReflectionAction<DifferencesManagementPanel>(getLocalizedString("Upload"), this, "onUploadLocal");
		remoteDownloadAction = new ReflectionAction<DifferencesManagementPanel>(getLocalizedString("Download"), this, "onDownloadRemote");
		remoteDeleteAction = new ReflectionAction<DifferencesManagementPanel>(getLocalizedString("Delete"), this, "onDeleteRemote");
		diffOverwriteAction = new ReflectionAction<DifferencesManagementPanel>(getLocalizedString("Overwrite_Remote"), this, "onOverwriteRemote");
		
		localOnlyObjectsList.addMouseListener(new PopupTrigger<DifferencesManagementPanel>(createLocalPopupMenu(), this, "enableDisableLocalMenu"));
		remoteOnlyObjectsList.addMouseListener(new PopupTrigger<DifferencesManagementPanel>(createRemotePopupMenu(), this, "enableDisableRemoteMenu"));
		localAndRemoteDifferObjectsList.addMouseListener(new PopupTrigger<DifferencesManagementPanel>(createDiffPopupMenu(), this, "enableDisableDiffMenu"));

		/*
		localUploadButton = new JButton(localUploadAction);
		remoteDownloadButton = new JButton(remoteDownloadAction);  
		remoteDeleteButton = new JButton(remoteDownloadAction);  
		diffOverwriteButton = new JButton(diffOverwriteAction);  
		*/
		
		int sepWidth = 10 ;
		
		mainBox.add(boxCol1) ;
		mainBox.add (Box.createHorizontalStrut(sepWidth)) ;
		mainBox.add(boxCol2) ;
		mainBox.add (Box.createHorizontalStrut(sepWidth)) ;
		mainBox.add(boxCol3) ;
		
		setBoxCol (boxCol1, getLocalizedString("Local_Only"), localOnlyObjectsList) ;
		setBoxCol (boxCol2, getLocalizedString("Local_and_Remote_Differ"), localAndRemoteDifferObjectsList) ;
		setBoxCol (boxCol3, getLocalizedString("Remote_Only"), remoteOnlyObjectsList) ;

		/*
		boxCol1.add(localUploadButton) ;
		boxCol2.add(diffOverwriteButton) ;
		boxCol3.add(remoteDownloadButton) ;
		boxCol3.add(remoteDeleteButton) ;
		*/
		
		setListBehavior (localOnlyObjectsList) ;
		setListBehavior (localAndRemoteDifferObjectsList) ;
		setListBehavior (remoteOnlyObjectsList) ;
		
		for (Pair<? extends ComparisonItem, ? extends ComparisonItem> pair : discrepancies)
		{
			if (pair.getFirst() == null || !pair.getFirst().exists()
					&& pair.getSecond() != null && pair.getSecond().exists())
			{
				remoteOnlyObjects.addElement(pair.getSecond()) ;
			}
			else if (pair.getFirst() != null && pair.getFirst().exists()
					&& pair.getSecond() != null && pair.getSecond().exists())
			{
				localAndRemoteDifferObjects.addElement(pair.getFirst());
			}
			else if (pair.getSecond() == null || !pair.getSecond().exists()
					&& pair.getFirst() != null && pair.getFirst().exists())
			{
				localOnlyObjects.addElement(pair.getFirst()) ;
			}
		}
		
        closeAction = new ReflectionAction<DifferencesManagementPanel>(getLocalizedString("Close"), null, this, "onClose");        
        closeButton = new JButton(closeAction);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.setBorder(new EmptyBorder(5, 5, 5, 5));
        buttons.add(closeButton);
        
        add(mainBox, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
	}
	
	
    private JPopupMenu createLocalPopupMenu() {
        JPopupMenu pop = new JPopupMenu();
        pop.add(new JMenuItem(localUploadAction));
        return pop;
    }
    
    
    private JPopupMenu createRemotePopupMenu() {
        JPopupMenu pop = new JPopupMenu();
        pop.add(new JMenuItem(remoteDownloadAction));
        pop.addSeparator();
        pop.add(new JMenuItem(remoteDeleteAction));
        return pop;
    }
    
    
    private JPopupMenu createDiffPopupMenu() {
        JPopupMenu pop = new JPopupMenu();
        pop.add(new JMenuItem(diffOverwriteAction));
        return pop;
    }
	
	
	private void setBoxCol (Box boxCol, String title, JList<ComparisonItem> list)
	{
		Border marginBorder = new EmptyBorder(5, 5, 5, 5);
		
		boxCol.setBorder(new CompoundBorder(BorderFactory.createTitledBorder(title), marginBorder));
		boxCol.add(setPrefSize(new JScrollPane (list), colWidth, listHeight)) ;
	}
	
	
    public static Icon getContentTypeIcon(ComparisonItem ci) 
    {
    	if (ci == null)
    		return MainPanel.getIcon("folder_error.png"); ;
    	
    	Icon ret = null ;
        String name = ci.getName() ;
        String contentType = null ;
        if (DifferencesFinder.isDirectory(ci))
        	contentType = SwiftUtils.directoryContentType ;
        if (name != null && !name.isEmpty())
        {
        	ret = FileTypeIconFactory.getFileTypeIcon(contentType, name) ;
        }
        return ret ;
    }
	
	
	private void setListBehavior (JList<ComparisonItem> list)
	{
		final Border LR_PADDING = BorderFactory.createEmptyBorder(0, 2, 0, 2);
		 
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setCellRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) 
			{
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(LR_PADDING);
                ComparisonItem ci = (ComparisonItem) value;
                lbl.setText(ci.getName());
                lbl.setToolTipText(lbl.getText());
                lbl.setIcon(getContentTypeIcon (ci)) ;
                return lbl;
            }
        });
		list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {            	
            	enableDisable () ;
            }
        });
	}
	
	
    private JComponent setPrefSize (JComponent comp, int minPrefW, int minPrefH)
    {
    	if (comp == null)
    		return null ;
    	comp.setMinimumSize(new Dimension(100, 100));
    	comp.setPreferredSize(new Dimension(minPrefW, minPrefH));
    	comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    	return comp ;
    }
    
    
    private String getLocalizedString (String key)
    {
    	if (stringsBundle == null)
	    	return key.replace("_", " ") ;
    	return stringsBundle.getLocalizedString(key) ;
    }
    
    
    public boolean isSingleStoredObjectSelected(JList<ComparisonItem> list) {
        return list.getSelectedIndices().length == 1;
    }
    
    private boolean isItemSelected(JList<ComparisonItem> list)
    {
    	return list.getSelectedIndex() >= 0;
    }
    
    private boolean isLocalItemSelected() {
        return isItemSelected(localOnlyObjectsList) ;
    }
    
    
    private boolean isRemoteItemSelected() {
    	return isItemSelected(remoteOnlyObjectsList) ;
    }
    
    
    private boolean isDiffItemSelected() {
    	return isItemSelected(localAndRemoteDifferObjectsList) ;
    }
    
    
    @SuppressWarnings("unused")
	private boolean isSingleLocalItemSelected() {
        return isSingleStoredObjectSelected(localOnlyObjectsList) ;
    }
    
    
    @SuppressWarnings("unused")
	private boolean isSingleRemoteItemSelected() {
    	return isSingleStoredObjectSelected(remoteOnlyObjectsList) ;
    }
    
    
    @SuppressWarnings("unused")
	private boolean isSingleDiffItemSelected() {
    	return isSingleStoredObjectSelected(localAndRemoteDifferObjectsList) ;
    }
    
    
    private List<ComparisonItem> getSelectedItems(JList<ComparisonItem> list, DefaultListModel<ComparisonItem> listModel, boolean removeFromModel) {
        List<ComparisonItem> results = new ArrayList<ComparisonItem>();
        int[] indices = list.getSelectedIndices() ;
        for (int index : indices) 
        {
            results.add(listModel.get(index)) ;
        }
        if (removeFromModel)
        {
        	// we must remove the element in a correct order
        	for (int i = indices.length - 1 ; i >= 0 ; --i)
        	{
        		listModel.remove(indices[i]);
        	}
        }
        return results;
    }
    
    
    private List<ComparisonItem> getSelectedLocalItems (boolean removeFromModel)
    {
    	return getSelectedItems (localOnlyObjectsList, localOnlyObjects, removeFromModel) ;
    }
    
    
    private List<ComparisonItem> getSelectedRemoteItems (boolean removeFromModel)
    {
    	return getSelectedItems (remoteOnlyObjectsList, remoteOnlyObjects, removeFromModel) ;
    }
    
    
    private List<ComparisonItem> getSelectedDiffItems (boolean removeFromModel)
    {
    	return getSelectedItems (localAndRemoteDifferObjectsList, localAndRemoteDifferObjects,removeFromModel) ;
    }
    
    
    private void enableDisable ()
    {
    	enableDisableLocalMenu () ;
    	enableDisableRemoteMenu () ;
    	enableDisableDiffMenu () ;
    }
    
    
    private void enableDisableLocalMenu ()
    {
    	localUploadAction.setEnabled(isLocalItemSelected ());
    }
    
    
    private void enableDisableRemoteMenu ()
    {
    	boolean isSelected = isRemoteItemSelected () ;
    	remoteDeleteAction.setEnabled(isSelected);
    	remoteDownloadAction.setEnabled(isSelected);
    }
    
    
    private void enableDisableDiffMenu ()
    {
    	diffOverwriteAction.setEnabled(isDiffItemSelected ());
    }
    
    
    @SuppressWarnings("unused")
	private void onUploadLocal ()
    {
    	uploadSelection (getSelectedLocalItems (true), false) ;
    }
    
    
    @SuppressWarnings("unused")
    private void onDownloadRemote ()
    {
    	
    	List<ComparisonItem> listItems = getSelectedRemoteItems (true) ;
    	List<Pair<? extends StoredObject, ? extends File> > pairParentObjectFiles = new ArrayList<> () ;
    	for (ComparisonItem ri : listItems)
    	{
    		if (ri == null)
    			continue ;
    		if (!(ri instanceof DifferencesFinder.RemoteItem))
    			continue ;
    		pairParentObjectFiles.add(Pair.newPair(DifferencesFinder.getObject(container, ri, srcStoredObject), DifferencesFinder.getFile(ri, srcFile))) ;
    	}
    	try 
    	{
			ops.downloadStoredObject(container, pairParentObjectFiles, false, null, callback);
		} 
    	catch (IOException e) 
    	{
    		logger.error("Error occurred while downloading files.", e);
		}	 	
    }
    
    
    @SuppressWarnings("unused")
    private void onDeleteRemote ()
    {
    	List<ComparisonItem> listItems = getSelectedRemoteItems (true) ;
    	List<StoredObject> storedObjects = new ArrayList<> () ;
    	for (ComparisonItem ri : listItems)
    	{
    		if (ri == null)
    			continue ;
    		if (!(ri instanceof DifferencesFinder.RemoteItem))
    			continue ;
    		storedObjects.add (DifferencesFinder.getObject(container, ri, srcStoredObject)) ;
    	}
    	String msg = MessageFormat.format (getLocalizedString("confirm_many_files_delete_from_container"), String.valueOf(storedObjects.size()), container.getName()) ;
    	if (JOptionPane.showConfirmDialog(this, msg, getLocalizedString("Confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
    		ops.deleteStoredObjects(container, storedObjects, null, callback);
        }	
    }
    
    
    @SuppressWarnings("unused")
    private void onOverwriteRemote ()
    {
    	uploadSelection (getSelectedDiffItems (true), true) ;
    }
    
    
    private void uploadSelection (List<ComparisonItem> listItems, boolean overwrite)
    {
    	if (listItems == null || listItems.isEmpty())
    		return ;
    	List<Pair<? extends StoredObject, ? extends File> > pairParentObjectFiles = new ArrayList<> () ;
    	for (ComparisonItem ci : listItems)
    	{
    		if (ci == null)
    			continue ;
    		if (!(ci instanceof DifferencesFinder.LocalItem))
    			continue ;
    		pairParentObjectFiles.add(Pair.newPair(DifferencesFinder.getObject(container, ci, srcStoredObject), ((DifferencesFinder.LocalItem)ci).getFile().toFile())) ;
    	}
    	try 
    	{
			ops.uploadFiles(container, pairParentObjectFiles, overwrite, null, callback);
		} 
    	catch (IOException e) 
    	{
    		logger.error("Error occurred while uploading files.", e);
		}	
    }
    
    
    @SuppressWarnings("unused")
	private void onClose ()
    {
    	if (owner != null)
    		owner.setVisible(false);
    }
}
