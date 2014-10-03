/*
 * Copyright 2014 Loic Merckel
 * Copyright 2012-2013 E.Hooijmeijer
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

/*
*
* The original version of this file (i.e., the one that is copyrighted 2012-2013 E.Hooijmeijer) 
* can  be found here:
*
*  https://github.com/javaswift/cloudie
*  package (src/main/java): org.javaswift.cloudie;
*  
* Note: the class has been renamed from CloudiePanel
* to MainPanel
*  
*  Various changes were made, such as:
*  - adding new menu items 
*  - adding the tree view
*  - adding the proxy setting
*  - adding the hubiC login function
*  - adding simulator login function
*  - adding localization
*  - adding various options 
*  - and many others...
*
*/


package org.swiftexplorer.gui;

import org.swiftexplorer.SwiftExplorer;
import org.swiftexplorer.config.HasConfiguration;
import org.swiftexplorer.config.localization.HasLocalizationSettings.LanguageCode;
import org.swiftexplorer.config.localization.HasLocalizationSettings.RegionCode;
import org.swiftexplorer.config.proxy.Proxy;
import org.swiftexplorer.config.swift.SwiftParameters;
import org.swiftexplorer.gui.diff.DifferencesManagementPanel;
import org.swiftexplorer.gui.localization.HasLocalizedStrings;
import org.swiftexplorer.gui.log.ErrorDlg;
import org.swiftexplorer.gui.log.LogPanel;
import org.swiftexplorer.gui.login.CloudieCallbackWrapper;
import org.swiftexplorer.gui.login.CredentialsStore;
import org.swiftexplorer.gui.login.CredentialsStore.Credentials;
import org.swiftexplorer.gui.login.LoginPanel;
import org.swiftexplorer.gui.login.LoginPanel.LoginCallback;
import org.swiftexplorer.gui.preview.PreviewPanel;
import org.swiftexplorer.gui.settings.PreferencesPanel;
import org.swiftexplorer.gui.settings.PreferencesPanel.PreferencesCallback;
import org.swiftexplorer.gui.settings.ProxiesStore;
import org.swiftexplorer.gui.settings.ProxyPanel;
import org.swiftexplorer.gui.settings.ProxyPanel.ProxyCallback;
import org.swiftexplorer.gui.settings.SwiftPanel;
import org.swiftexplorer.gui.util.AsyncWrapper;
import org.swiftexplorer.gui.util.ComputerSleepingManager;
import org.swiftexplorer.gui.util.DoubleClickListener;
import org.swiftexplorer.gui.util.FileTypeIconFactory;
import org.swiftexplorer.gui.util.GuiTreadingUtils;
import org.swiftexplorer.gui.util.LabelComponentPanel;
import org.swiftexplorer.gui.util.PopupTrigger;
import org.swiftexplorer.gui.util.ReflectionAction;
import org.swiftexplorer.gui.util.SwiftOperationStopRequesterImpl;
import org.swiftexplorer.gui.util.SwingUtils;
import org.swiftexplorer.swift.SwiftAccess;
import org.swiftexplorer.swift.client.factory.AccountConfigFactory;
import org.swiftexplorer.swift.client.impl.HttpClientFactoryImpl;
import org.swiftexplorer.swift.client.impl.HubicAccessProvider;
import org.swiftexplorer.swift.operations.ContainerSpecification;
import org.swiftexplorer.swift.operations.SwiftOperations;
import org.swiftexplorer.swift.operations.SwiftOperations.ComparisonItem;
import org.swiftexplorer.swift.operations.SwiftOperations.ResultCallback;
import org.swiftexplorer.swift.operations.SwiftOperations.SwiftCallback;
import org.swiftexplorer.swift.operations.SwiftOperationsImpl;
import org.swiftexplorer.swift.util.HubicSwift;
import org.swiftexplorer.swift.util.SwiftUtils;
import org.swiftexplorer.util.FileUtils;
import org.swiftexplorer.util.Pair;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.accessibility.AccessibleContext;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.exception.CommandExceptionError;
import org.javaswift.joss.model.Access;
import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.Directory;
import org.javaswift.joss.model.StoredObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;


/**
 * MainPanel.
 * 
 * @author Erik Hooijmeijer
 * @author Loic Merckel
 * 
 */
public class MainPanel extends JPanel implements SwiftOperations.SwiftCallback {
		
	private static final long serialVersionUID = 1L;
	
	final private Logger logger = LoggerFactory.getLogger(MainPanel.class);
	
	private HasConfiguration config = null ;
	
	private static final Border LR_PADDING = BorderFactory.createEmptyBorder(0, 2, 0, 2);
    private final DefaultListModel<Container> containers = new DefaultListModel<Container>();
    private final JList<Container> containersList = new JList<Container>(containers);
    
    private final DefaultListModel<StoredObject> storedObjects = new DefaultListModel<StoredObject>();
    private final JList<StoredObject> storedObjectsList = new JList<StoredObject>(storedObjects);
    private final Collection<StoredObject> allStoredObjects ;

    private final JTree tree = new JTree (new Object[] {}) ; 
    private List<TreePath> treeExpansionState = null ; 
    private final Set<String> treeHasBeenExpandedNodeSet = new HashSet<> () ;
    
    private final JTextField searchTextField = new JTextField(16);
    private final JButton progressButton = new JButton() ;
    private final ProgressPanel progressPanel = new ProgressPanel () ;
    
    private JDialog differencesManagementDlg = null ;
    
    private final JButton stopButton = new JButton() ;
    
    private final JTabbedPane objectViewTabbedPane = new JTabbedPane ();
    @SuppressWarnings("unused")
	private final int treeviewTabIndex ;
    private final int listviewTabIndex ;

    private Action accountHubicLoginAction = null ;
    private Action accountGenericLoginAction = null ;
    private Action accountSimulatorLoginAction = null ;
    private Action accountLogoutAction = null ;
    private Action accountQuitAction = null ;

    private Action containerRefreshAction = null ;
    private Action containerCreateAction = null ;
    private Action containerDeleteAction = null ;
    private Action containerPurgeAction = null ;
    private Action containerEmptyAction = null ;
    private Action containerViewMetaData = null ;
    private Action containerGetInfoAction = null ;

    private Action storedObjectOpenAction = null ;
    private Action storedObjectPreviewAction = null ;
    private Action storedObjectUploadFilesAction = null ;
    private Action storedObjectDownloadFilesAction = null ;
    private Action storedObjectDeleteFilesAction = null ;
    private Action storedObjectDeleteDirectoryAction = null ;
    private Action storedObjectViewMetaData = null ;
    private Action storedObjectGetInfoAction = null ;
    private Action storedObjectUploadDirectoryAction = null ;
    private Action storedObjectCreateDirectoryAction = null ;
    private Action storedObjectDownloadDirectoryAction = null ;
    private Action storedObjectCompareFilesAction = null ;
    private Action storedObjectCompareDirectoriesAction = null ;

    private Action settingProxyAction = null ;
    private Action settingPreferencesAction = null ;
    private Action settingSwiftAction = null ;

    private Action aboutAction = null ;
    private Action jvmInfoAction = null ;

    private Action searchAction = null ;
    
    private Action progressButtonAction = null ;
    private Action stopButtonAction = null ;

    private JFrame owner;

    private final SwiftOperations ops;
    private SwiftOperations.SwiftCallback callback;
    private volatile SwiftOperationStopRequesterImpl.Stopper stopper = new SwiftOperationStopRequesterImpl.Stopper () ;
    private PreviewPanel previewPanel = new PreviewPanel();
    private StatusPanel statusPanel;
    private boolean loggedIn;
    private final AtomicInteger busyCnt = new AtomicInteger();

    private CredentialsStore credentialsStore = new CredentialsStore();
    
    private File lastFolder = null;
    
    private final HasLocalizedStrings stringsBundle ;
    
    private final boolean nativeMacOsX = SwiftExplorer.isMacOsX() ; 
    
    private final boolean createDefaultContainerInMockMode = true ;
    private final boolean allowCustomeSwiftSettings = true ;
    
    private volatile boolean hideSegmentsContainers = false ; 
    
    private final LogPanel errorLogPanel = new LogPanel () ;
    private ErrorDlg errorDialog ;
    
    
    /**
     * creates MainPanel and immediately logs in using the given credentials.
     * @param login the login credentials.
     */
    public MainPanel(List<String> login, HasConfiguration config, HasLocalizedStrings stringsBundle) {
        this(config, stringsBundle);
        ops.login(AccountConfigFactory.getKeystoneAccountConfig(), login.get(0), login.get(1), login.get(2), login.get(3), callback);
    }
    
    
   public MainPanel(SwiftAccess swiftAccess, HasConfiguration config, HasLocalizedStrings stringsBundle) {
       this(config, stringsBundle);
       ops.login(AccountConfigFactory.getHubicAccountConfig(swiftAccess), config.getHttpProxySettings(), callback);
   }

   
    /**
     * creates MainPanel and immediately logs in using the given previously stored
     * profile.
     * @param profile the profile.
     */
    public MainPanel(String profile, HasConfiguration config, HasLocalizedStrings stringsBundle) {
        this(config, stringsBundle);
        CredentialsStore store = new CredentialsStore();
        Credentials found = null;
        for (Credentials cr : store.getAvailableCredentials()) {
            if (cr.toString().equals(profile)) {
                found = cr;
            }
        }
        if (found == null) {
            throw new ParameterException("Unknown profile '" + profile + "'.");
        } else {
            ops.login(AccountConfigFactory.getKeystoneAccountConfig(), found.authUrl, found.tenant, found.username, String.valueOf(found.password), callback);
        }
    }

    
    /**
     * creates MainPanel and does not login.
     */
    public MainPanel(HasConfiguration config, HasLocalizedStrings stringsBundle) {
        super(new BorderLayout());
        //
        this.config = config ;
        this.stringsBundle = stringsBundle ;
        
    	allStoredObjects = Collections.synchronizedSortedSet(new TreeSet<StoredObject>());
                
        initMenuActions () ;
        //
        ops = createSwiftOperations();
        callback = GuiTreadingUtils.guiThreadSafe(SwiftCallback.class, this);
        //
        statusPanel = new StatusPanel(ops, callback);
        //
        JScrollPane left = new JScrollPane(containersList);
        //
        
        if (config != null && config.getSwiftSettings() != null)
        	hideSegmentsContainers = config.getSwiftSettings().hideSegmentsContainers() ;
        
        tree.setEditable(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        
        tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {

				// we reflect the change to storedObjectsList
				TreePath tps = tree.getSelectionPath() ;
				if (tps == null)
					return ;
				Object sel = tps.getLastPathComponent() ;
				if (sel == null || !(sel instanceof StoredObjectsTreeModel.TreeNode))
					return ;
				StoredObjectsTreeModel.TreeNode node = (StoredObjectsTreeModel.TreeNode)sel;
				if (node.isRoot())
					storedObjectsList.clearSelection();
				else if (node.getStoredObject() != null)
					storedObjectsList.setSelectedValue(node.getStoredObject(), true); 

			}});
        
     	
    	tree.addTreeWillExpandListener(new TreeWillExpandListener () {

			@Override
			public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException 
			{
				Container container = getSelectedContainer();
				if (container == null)
					return ;
				TreePath tps = event.getPath() ;
				expandNode (container, tps) ;
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event)
					throws ExpandVetoException {
				// here, we do nothing
			}});
        
        tree.setCellRenderer(new DefaultTreeCellRenderer () {

			private static final long serialVersionUID = 1L;
			
			@Override
			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean selected, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {

                JLabel lbl = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                lbl.setBorder(LR_PADDING);
				if (value == null || !(value instanceof StoredObjectsTreeModel.TreeNode))
					return lbl ;
				StoredObject storedObject = ((StoredObjectsTreeModel.TreeNode)value).getStoredObject() ;
                lbl.setText(((StoredObjectsTreeModel.TreeNode)value).getNodeName());
                lbl.setToolTipText(lbl.getText());
                if (((StoredObjectsTreeModel.TreeNode)value).isRoot())
                	lbl.setIcon(getContainerIcon (((StoredObjectsTreeModel.TreeNode)value).getContainer())) ;
                else if (((StoredObjectsTreeModel.TreeNode)value).isVirtual())
                	lbl.setIcon(getVirtualDirectoryIcon ()) ;
                else
                	lbl.setIcon(getContentTypeIcon (storedObject)) ;
                return lbl;
			}
        });
        
        // set the pane
        objectViewTabbedPane.add(getLocalizedString("Tree_View"), new JScrollPane (tree)) ;
        objectViewTabbedPane.add(getLocalizedString("List_View"), new JScrollPane (storedObjectsList)) ;
        // set the index accordingly
        treeviewTabIndex = 0 ;
        listviewTabIndex = 1 ;
        
        //
        storedObjectsList.setMinimumSize(new Dimension(420, 320));
        JSplitPane center = new JSplitPane(JSplitPane.VERTICAL_SPLIT, objectViewTabbedPane, new JScrollPane(previewPanel));
        center.setDividerLocation(450);
        //
        
        add(createToolBar (), BorderLayout.NORTH);
        
        //
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, center);
        main.setDividerLocation(272);
        add(main, BorderLayout.CENTER);
        //
        add(statusPanel, BorderLayout.SOUTH);
        //
        searchTextField.setAction(searchAction);
        
        progressButton.setAction(progressButtonAction) ;
        progressButton.setEnabled(false);
        progressButton.setToolTipText(getLocalizedString("Show_Progress"));
        
        
        stopButton.setAction(stopButtonAction) ;
        //stopButton.setEnabled(false);
        stopButton.setToolTipText(getLocalizedString("Stop"));
        
        //
        createLists();
        //
        storedObjectsList.addMouseListener(new PopupTrigger<MainPanel>(createStoredObjectPopupMenu(), this, "enableDisableStoredObjectMenu"));
        storedObjectsList.addMouseListener(new DoubleClickListener(storedObjectPreviewAction));
        containersList.addMouseListener(new PopupTrigger<MainPanel>(createContainerPopupMenu(), this, "enableDisableContainerMenu"));
        
        tree.addMouseListener(new PopupTrigger<MainPanel>(createStoredObjectPopupMenu(), this, "enableDisableStoredObjectMenu"));
        tree.addMouseListener(new DoubleClickListener(storedObjectPreviewAction));
       
        
		if (nativeMacOsX) 
		{
			//new MacOsMenuHandler(this);
			// http://www.kfu.com/~nsayer/Java/reflection.html
			try 
			{
				Object[] args = {this};
				@SuppressWarnings("rawtypes")
				Class[] arglist = {MainPanel.class} ;
				Class<?> mac_class ;
				mac_class = Class.forName("org.swiftexplorer.gui.MacOsMenuHandler") ;
				Constructor<?> new_one = mac_class.getConstructor(arglist) ;
				new_one.newInstance(args) ;
			} 
			catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) 
			{
				logger.error("Error occurred while creating mac os specific menu", ex);
			}
		}

        //
        bind();
        //
        enableDisable();
    }
    
    
    private void expandNode (Container container, final TreePath treePath)
    {
		if (treePath == null || container == null)
			return ;
					
		Object sel = treePath.getLastPathComponent() ;
		if (sel == null || !(sel instanceof StoredObjectsTreeModel.TreeNode))
			return ;
		StoredObjectsTreeModel.TreeNode node = (StoredObjectsTreeModel.TreeNode)sel;	
		
		if (node == null || node.isRoot()){
			return ;
		}
		
		TreeModel tm = tree.getModel() ;
		if (tm != null && tm.getChildCount(node) > 0)
		{
			treeHasBeenExpandedNodeSet.add(node.getObjectName()) ;
			return ;
		}
		
		// intercept the callback with wrapper to expand the node
		final SwiftCallback cb = GuiTreadingUtils.guiThreadSafe(
				SwiftCallback.class, new CloudieCallbackWrapper(callback) {
				    @Override
				    public void onAppendStoredObjects(final Container container, int page, final Collection<StoredObject> sos) {
				    	
				    	if (treeExpansionState != null)
				    		treeExpansionState.add(treePath) ;
				    	
				    	super.onAppendStoredObjects(container, page, sos) ;
				    }
				});
		
		if (node.isVirtual()){
			loadDirectory (container, node.getObjectName(), cb) ;
		}
		else if (node.getStoredObject() != null){
			StoredObject obj = node.getStoredObject() ;
			if (SwiftUtils.isDirectory(obj))
				loadDirectory (container, obj.getName(), cb) ;
		}
    }
    
    
    private void loadDirectory (Container container, String dirName, SwiftCallback clbk)
    {
    	if (container == null)
    		return ;
    	
		if (treeHasBeenExpandedNodeSet.contains(dirName))
			return ;
		treeHasBeenExpandedNodeSet.add(dirName) ;

		treeExpansionState = StoredObjectsTreeModel.TreeUtils.getTreeExpansionState (tree) ;
		
		Directory d = new Directory (dirName, SwiftUtils.separator.charAt(0)) ;
		ops.refreshDirectoriesOrStoredObjects(container, d, clbk);	
    }
    
    
    private JToolBar createToolBar ()
    {
        JToolBar toolBar = new JToolBar(getLocalizedString("StoredObject"));
        
        toolBar.add(storedObjectPreviewAction).setToolTipText(getLocalizedString("Preview"));
        toolBar.add(storedObjectOpenAction).setToolTipText(getLocalizedString("Open_in_Browser"));
        toolBar.add(storedObjectViewMetaData).setToolTipText(getLocalizedString("View_Metadata"));
        toolBar.addSeparator();
        toolBar.add(storedObjectGetInfoAction).setToolTipText(getLocalizedString("Get_Info"));
        toolBar.addSeparator();
        toolBar.add(storedObjectUploadFilesAction).setToolTipText(getLocalizedString("Upload_Files"));
        toolBar.add(storedObjectDownloadFilesAction).setToolTipText(getLocalizedString("Download_Files"));
        toolBar.addSeparator();
        toolBar.add(storedObjectCreateDirectoryAction).setToolTipText(getLocalizedString("Create_Directory"));
        toolBar.add(storedObjectUploadDirectoryAction).setToolTipText(getLocalizedString("Upload_Directory"));
        toolBar.add(storedObjectDownloadDirectoryAction).setToolTipText(getLocalizedString("Download_Directory"));
        
        toolBar.addSeparator();
        toolBar.add(progressButton) ;
        
        toolBar.addSeparator();
        toolBar.add(stopButton) ;
        
        //if (nativeMacOsX)
        	addSearchPanel (toolBar) ;
        
        return (toolBar) ;
    }
    
    
    private void initMenuActions () 
    {
        accountHubicLoginAction = new ReflectionAction<MainPanel>(getLocalizedString("hubiC_Login"), getIcon("server_connect.png"), this, "onHubicLogin");
        accountGenericLoginAction = new ReflectionAction<MainPanel>(getLocalizedString("Generic_Login"), getIcon("server_connect.png"), this, "onLogin");
        accountLogoutAction = new ReflectionAction<MainPanel>(getLocalizedString("Logout"), getIcon("disconnect.png"), this, "onLogout");
        accountQuitAction = new ReflectionAction<MainPanel>(getLocalizedString("Quit"), getIcon("weather_rain.png"), this, "onQuit");
        accountSimulatorLoginAction= new ReflectionAction<MainPanel>(getLocalizedString("Simulator_Login"), getIcon("server_connect.png"), this, "onSimulatorLogin");
        
        containerRefreshAction = new ReflectionAction<MainPanel>(getLocalizedString("Refresh"), getIcon("arrow_refresh.png"), this, "onRefreshContainers");
        containerCreateAction = new ReflectionAction<MainPanel>(getLocalizedString("Create"), getIcon("folder_add.png"), this, "onCreateContainer");
        containerDeleteAction = new ReflectionAction<MainPanel>(getLocalizedString("Delete"), getIcon("folder_delete.png"), this, "onDeleteContainer");
        containerPurgeAction = new ReflectionAction<MainPanel>(getLocalizedString("Purge"), getIcon("delete.png"), this, "onPurgeContainer");
        containerEmptyAction = new ReflectionAction<MainPanel>(getLocalizedString("Empty"), getIcon("bin_empty.png"), this, "onEmptyContainer");
        containerViewMetaData = new ReflectionAction<MainPanel>(getLocalizedString("View_Metadata"), getIcon("page_gear.png"), this, "onViewMetaDataContainer");
        containerGetInfoAction = new ReflectionAction<MainPanel>(getLocalizedString("Get_Info"), getIcon("information.png"), this, "onGetInfoContainer");
        
        storedObjectOpenAction = new ReflectionAction<MainPanel>(getLocalizedString("Open_in_Browser"), getIcon("application_view_icons.png"), this, "onOpenInBrowserStoredObject");
        storedObjectPreviewAction = new ReflectionAction<MainPanel>(getLocalizedString("Preview"), getIcon("images.png"), this, "onPreviewStoredObject");
        storedObjectUploadFilesAction = new ReflectionAction<MainPanel>(getLocalizedString("Upload_Files"), getIcon("file_upload.png"), this, "onCreateStoredObject");
        storedObjectDownloadFilesAction = new ReflectionAction<MainPanel>(getLocalizedString("Download_Files"), getIcon("file_download.png"), this, "onDownloadStoredObject");
        storedObjectDeleteFilesAction = new ReflectionAction<MainPanel>(getLocalizedString("Delete_Files"), getIcon("page_delete.png"), this, "onDeleteStoredObject");
        storedObjectViewMetaData = new ReflectionAction<MainPanel>(getLocalizedString("View_Metadata"), getIcon("page_gear.png"), this, "onViewMetaDataStoredObject");
        storedObjectGetInfoAction = new ReflectionAction<MainPanel>(getLocalizedString("Get_Info"), getIcon("information.png"), this, "onGetInfoStoredObject"); 
        storedObjectUploadDirectoryAction = new ReflectionAction<MainPanel>(getLocalizedString("Upload_Directory"), getIcon("folder_upload.png"), this, "onUploadDirectory");
        storedObjectCreateDirectoryAction = new ReflectionAction<MainPanel>(getLocalizedString("Create_Directory"), getIcon("folder_add.png"), this, "onCreateDirectory");
        storedObjectDownloadDirectoryAction = new ReflectionAction<MainPanel>(getLocalizedString("Download_Directory"), getIcon("folder_download.png"), this, "onDownloadStoredObjectDirectory");
        storedObjectDeleteDirectoryAction = new ReflectionAction<MainPanel>(getLocalizedString("Delete_Directory"), getIcon("folder_delete.png"), this, "onDeleteStoredObjectDirectory");
        storedObjectCompareFilesAction = new ReflectionAction<MainPanel>(getLocalizedString("Compare_Files"), getIcon("compare_files.png"), this, "onCompareFiles") ;
        storedObjectCompareDirectoriesAction = new ReflectionAction<MainPanel>(getLocalizedString("Compare_Directories"), getIcon("compare_directories.png"), this, "onCompareDirectories");
        
        settingProxyAction = new ReflectionAction<MainPanel>(getLocalizedString("Proxy"), getIcon("server_link.png"), this, "onProxy");
        settingPreferencesAction = new ReflectionAction<MainPanel>(getLocalizedString("Preferences"), getIcon("wrench.png"), this, "onPreferences");
        settingSwiftAction = new ReflectionAction<MainPanel>(getLocalizedString("Swift_Parameters"), getIcon("mixer.png"), this, "onSwiftParameters");
        
        
        aboutAction = new ReflectionAction<MainPanel>(getLocalizedString("About"), getIcon("logo_16.png"), this, "onAbout");
        
        jvmInfoAction = new ReflectionAction<MainPanel>(getLocalizedString("JVM_Info"), getIcon("chart_bar.png"), this, "onJvmInfo");
        
        searchAction = new ReflectionAction<MainPanel>(getLocalizedString("Search"), null, this, "onSearch");
    
        progressButtonAction = new ReflectionAction<MainPanel>("", getIcon("progressbar.png"), this, "onProgressButton");
        
        stopButtonAction = new ReflectionAction<MainPanel>("", getIcon("stop.png"), this, "onStop");
    }
    
    
    private SwiftOperations createSwiftOperations() 
    {	    
        SwiftOperationsImpl lops = new SwiftOperationsImpl();
        return AsyncWrapper.async(lops);
    }
    

    public void setOwner(JFrame owner) {
        this.owner = owner;
    }

    
    private void createLists() {
        containersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        containersList.setCellRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(LR_PADDING);
                Container c = (Container) value;
                lbl.setText(c.getName());
                lbl.setToolTipText(lbl.getText());
                
                // Add the icon
                lbl.setIcon(getContainerIcon (c));
                
                return lbl;
            }
        });
        containersList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableContainerMenu();
                updateStatusPanelForContainer();
            }
        });
        //
        storedObjectsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        storedObjectsList.setCellRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(LR_PADDING);
                StoredObject so = (StoredObject) value;
                lbl.setText(so.getName());
                lbl.setToolTipText(lbl.getText());
                lbl.setIcon(getContentTypeIcon (so)) ;
                return lbl;
            }
        });
        storedObjectsList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableStoredObjectMenu();
                updateStatusPanelForStoredObject();
            }
        });
    }
    
    
    public static Icon getContainerIcon(Container container) 
    {
    	if (container == null)
    		return MainPanel.getIcon("box.png") ;
    	if (!container.isPublic())
    		return MainPanel.getIcon("box_closed.png") ;
    	else
    		return MainPanel.getIcon("box_open.png") ;
    }
    
    
    public static Icon getVirtualDirectoryIcon() 
    {
    	return MainPanel.getIcon("folder_error.png") ;
    }
    
    
    public static Icon getContentTypeIcon(StoredObject storedObject) 
    {
    	if (storedObject == null)
    		return MainPanel.getIcon("folder_error.png"); 
    	
    	Icon ret = null ;
        String contentType = storedObject.getContentType() ;
        if (contentType != null && !contentType.isEmpty())
        {
        	ret = FileTypeIconFactory.getFileTypeIcon(contentType, storedObject.getName()) ;
        }
        return ret ;
    }
   	
	
	public void onJvmInfo ()
	{
		StringBuilder sb = new StringBuilder();

		boolean si = true;

		// Total number of processors or cores available to the JVM
		sb.append(getLocalizedString("Available_processors") + ": ");
		sb.append(Runtime.getRuntime().availableProcessors());
		sb.append(System.lineSeparator());

		// Total amount of free memory available to the JVM
		sb.append(getLocalizedString("Free_memory_available_to_the_JVM") + ": ");
		sb.append(FileUtils.humanReadableByteCount(Runtime.getRuntime().freeMemory(), si));
		sb.append(System.lineSeparator());

		// Maximum amount of memory the JVM will attempt to use
		long maxMemory = Runtime.getRuntime().maxMemory();
		sb.append(getLocalizedString("Maximum_memory_the_JVM_will_attempt_to_use") + ": ");
		sb.append((maxMemory == Long.MAX_VALUE ? "no limit" : FileUtils.humanReadableByteCount(maxMemory, si)));
		sb.append(System.lineSeparator());

		// Total memory currently in use by the JVM
		sb.append(getLocalizedString("Total_memory_currently_in_use_by_the_JVM") + ": ");
		sb.append(FileUtils.humanReadableByteCount(Runtime.getRuntime().totalMemory(), si));
		sb.append(System.lineSeparator());

		JOptionPane.showMessageDialog(this, sb.toString(), getLocalizedString("Information"), JOptionPane.INFORMATION_MESSAGE);
	}
    
    
    private void buildTree (boolean restore)
    {
    	Container selectedContainer = getSelectedContainer() ;
    	if (selectedContainer == null)
    	{
	        tree.setRootVisible(false) ;
	        tree.setModel(new StoredObjectsTreeModel (null, null)) ;
    		return ;
    	}
    	
    	final Collection<StoredObject> storedObjectsList ;
    	String filter = searchTextField.getText();

		if (restore)
			//StoredObjectsTreeModel.TreeUtils.getTreeExpansionState (tree, treeExpansionState) ;
			treeExpansionState = StoredObjectsTreeModel.TreeUtils.getTreeExpansionState(tree) ;
		
    	if (filter != null && !filter.isEmpty())
    	{
    		storedObjectsList = new ArrayList<StoredObject> () ;
            for (StoredObject storedObject : allStoredObjects) {
                if (isFilterIncluded(storedObject)) {
                	storedObjectsList.add(storedObject);
                }
            }
    	}
    	else
    		storedObjectsList = allStoredObjects ;
    	
        StoredObjectsTreeModel nm = new StoredObjectsTreeModel (selectedContainer, storedObjectsList) ;
        tree.setRootVisible(!allStoredObjects.isEmpty()) ;
        tree.setModel(nm) ;

        if (restore && treeExpansionState != null && !treeExpansionState.isEmpty())
        	StoredObjectsTreeModel.TreeUtils.setTreeExpansionState(tree, treeExpansionState);
    }

    
    private void updateTree (Collection<StoredObject> list)
    {
    	TreeModel tm = tree.getModel() ;
    	if (tm == null)
    		return ;
    	if (!(tm instanceof StoredObjectsTreeModel))
    		return ;
    	((StoredObjectsTreeModel)tm).addAll(list) ;
    	tree.setRootVisible(!allStoredObjects.isEmpty()) ;
    }

    
    public static Icon getIcon(String string) {
        return new ImageIcon(MainPanel.class.getResource("/icons/" + string));
    }
    
    
    private String getLocalizedString (String key)
    {
    	if (stringsBundle == null)
	    	return key.replace("_", " ") ;
    	return stringsBundle.getLocalizedString(key) ;
    }
    

    public boolean isContainerSelected() {
        return containersList.getSelectedIndex() >= 0;
    }

    
    public Container getSelectedContainer() {
        return isContainerSelected() ? (Container) containers.get(containersList.getSelectedIndex()) : null;
    }

    
    public boolean isSingleStoredObjectSelected() {
        return storedObjectsList.getSelectedIndices().length == 1;
    }

    
    public boolean isStoredObjectsSelected() {
        return storedObjectsList.getSelectedIndices().length > 0;
    }

    
    public List<StoredObject> getSelectedStoredObjects() {
        List<StoredObject> results = new ArrayList<StoredObject>();
        for (int idx : storedObjectsList.getSelectedIndices()) {
            results.add((StoredObject) storedObjects.get(idx));
        }
        return results;
    }

    
    public <A> A single(List<A> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    
    public void enableDisable() {
        enableDisableAccountMenu();
        enableDisableContainerMenu();
        enableDisableStoredObjectMenu();
        enableSettingMenu() ;
        enableOperationMenu () ;
    }

    
    public void enableDisableAccountMenu() {
        accountHubicLoginAction.setEnabled(!loggedIn);
        accountGenericLoginAction.setEnabled(!loggedIn);
        accountSimulatorLoginAction.setEnabled(!loggedIn);
        accountLogoutAction.setEnabled(loggedIn);
        accountQuitAction.setEnabled(true);
    }
    
    
    public void enableSettingMenu() {
    	settingSwiftAction.setEnabled(!loggedIn);
    }

    public void enableOperationMenu() {
    	
    	boolean isBusy = busyCnt.get() > 0 ;
    	boolean isStoppable = (stopper.countObservers() > 0) ; // (stopRequester != null && !stopRequester.isStopRequested()) ;
    	
    	stopButtonAction.setEnabled(loggedIn && isBusy && isStoppable);
    }
    
    public void enableDisableContainerMenu() {
        boolean containerSelected = isContainerSelected();
        Container selected = getSelectedContainer();

        boolean isBusy = busyCnt.get() > 0 ;
        
        containerRefreshAction.setEnabled(loggedIn && !isBusy);
        containerCreateAction.setEnabled(loggedIn && !isBusy);
        containerDeleteAction.setEnabled(containerSelected && !isBusy);
        containerPurgeAction.setEnabled(containerSelected  && !isBusy);
        containerEmptyAction.setEnabled(containerSelected  && !isBusy);
        containerViewMetaData.setEnabled(containerSelected && selected.isInfoRetrieved() && !selected.getMetadata().isEmpty()  && !isBusy);
        containerGetInfoAction.setEnabled(containerSelected);
    }

    
    public void enableDisableStoredObjectMenu() {
        boolean singleObjectSelected = isSingleStoredObjectSelected();
        boolean objectsSelected = isStoredObjectsSelected();
        boolean containerSelected = isContainerSelected();
        StoredObject selected = single(getSelectedStoredObjects());
        Container selectedContainer = getSelectedContainer();
        
        boolean isBusy = busyCnt.get() > 0 ;
        boolean isSelectedDir = SwiftUtils.isDirectory(selected) ;
        
        storedObjectPreviewAction.setEnabled(singleObjectSelected && selected.isInfoRetrieved()  && !isBusy);

        storedObjectUploadFilesAction.setEnabled(containerSelected && (singleObjectSelected || !objectsSelected) && !isBusy);
        storedObjectDownloadFilesAction.setEnabled(containerSelected && objectsSelected && !isBusy && !isSelectedDir);
        storedObjectViewMetaData.setEnabled(containerSelected && singleObjectSelected && selected.isInfoRetrieved() && !selected.getMetadata().isEmpty()  && !isBusy);
        
        storedObjectOpenAction.setEnabled(objectsSelected && containerSelected && selectedContainer.isPublic() && !isBusy);
        storedObjectDeleteFilesAction.setEnabled(containerSelected && objectsSelected && !isBusy && !isSelectedDir);
        
        storedObjectUploadDirectoryAction.setEnabled(containerSelected && (singleObjectSelected || !objectsSelected) && !isBusy); 
        storedObjectGetInfoAction.setEnabled(containerSelected && objectsSelected);
        
        storedObjectCreateDirectoryAction.setEnabled(containerSelected && (singleObjectSelected || !objectsSelected) && !isBusy);
        
        storedObjectDownloadDirectoryAction.setEnabled(containerSelected && singleObjectSelected && !isBusy && isSelectedDir);
        storedObjectDeleteDirectoryAction.setEnabled(containerSelected && singleObjectSelected && !isBusy && isSelectedDir);
        
        storedObjectCompareFilesAction.setEnabled(containerSelected && singleObjectSelected && !isBusy && !isSelectedDir);
        storedObjectCompareDirectoriesAction.setEnabled(containerSelected && singleObjectSelected && !isBusy && isSelectedDir);
    }

    
    protected void updateStatusPanelForStoredObject() {
        if (isStoredObjectsSelected()) {
            statusPanel.onSelectStoredObjects(getSelectedStoredObjects());
        }
    }

    
    protected void updateStatusPanelForContainer() {
        if (isContainerSelected()) {
            statusPanel.onSelectContainer(getSelectedContainer());
        }
    }
    

    @Override
    public void onNumberOfCalls(int nrOfCalls) {
        statusPanel.onNumberOfCalls(nrOfCalls);
    }


    public void onHubicLogin() {
    	
    	accountHubicLoginAction.setEnabled(false);
    	accountGenericLoginAction.setEnabled(false);
    	accountSimulatorLoginAction.setEnabled(false);
    	
		new SwingWorker<SwiftAccess, Object>() {

			@Override
			protected SwiftAccess doInBackground() throws Exception {
				
	    	    return HubicSwift.getSwiftAccess() ;
			}

			@Override
			protected void done() {
			
				SwiftAccess sa = null ;
				try 
				{
					sa = get();
				} 
				catch (InterruptedException | ExecutionException e) 
				{
					logger.error("Error occurred while authentifying.", e);	
				}
				accountHubicLoginAction.setEnabled(true);
				accountGenericLoginAction.setEnabled(true);
				accountSimulatorLoginAction.setEnabled(false);
				
				if (sa == null)
				{
					JOptionPane.showMessageDialog(null,
							getLocalizedString("Login_Failed") + "\n",
							getLocalizedString("Error"), JOptionPane.ERROR_MESSAGE);
				}
				else
				{
					SwiftCallback cb = GuiTreadingUtils.guiThreadSafe(
							SwiftCallback.class, new CloudieCallbackWrapper(
									callback) {
								@Override
								public void onLoginSuccess() {
									super.onLoginSuccess();
								}
	
								@Override
								public void onError(CommandException ex) {
									JOptionPane.showMessageDialog(null,
											getLocalizedString("Login_Failed") + "\n" + ex.toString(),
											getLocalizedString("Error"), JOptionPane.ERROR_MESSAGE);
								}
							});

					if (allowCustomeSwiftSettings)
						ops.login(AccountConfigFactory.getHubicAccountConfig(sa), config.getSwiftSettings(), config.getHttpProxySettings(), cb);
					else
						ops.login(AccountConfigFactory.getHubicAccountConfig(sa), config.getHttpProxySettings(), cb);
				}
				enableDisable();
				if (owner != null)
					owner.toFront();
			}

		}.execute();
    }
    
    
    public void onLogin() {
        final JDialog loginDialog = new JDialog(owner, getLocalizedString("Login"));
        final LoginPanel loginPanel = new LoginPanel(new LoginCallback() {
            @Override
            public void doLogin(String authUrl, String tenant, String username, char[] pass) {
            	SwiftCallback cb = GuiTreadingUtils.guiThreadSafe(SwiftCallback.class, new CloudieCallbackWrapper(callback) {
                    @Override
                    public void onLoginSuccess() {
                        loginDialog.setVisible(false);
                        super.onLoginSuccess();
                    }

                    @Override
                    public void onError(CommandException ex) {
                        JOptionPane.showMessageDialog(loginDialog,  getLocalizedString("Login_Failed") + "\n" + ex.toString(), getLocalizedString("Error"), JOptionPane.ERROR_MESSAGE);
                    }
                });
            	if (allowCustomeSwiftSettings)
            		ops.login(AccountConfigFactory.getKeystoneAccountConfig(), config.getSwiftSettings(), authUrl, tenant, username, new String(pass), cb);
            	else
            		ops.login(AccountConfigFactory.getKeystoneAccountConfig(), authUrl, tenant, username, new String(pass), cb);
            }
        }, credentialsStore, stringsBundle);
        try {
            loginPanel.setOwner(loginDialog);
            loginDialog.getContentPane().add(loginPanel);
            loginDialog.setModal(true);
            loginDialog.setSize(480, 280);
            loginDialog.setResizable(false);
            loginDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            center(loginDialog);
            loginDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    loginPanel.onCancel();
                }

                @Override
                public void windowOpened(WindowEvent e) {
                    loginPanel.onShow();
                }
            });
            loginDialog.setVisible(true);
        } finally {
            loginDialog.dispose();
        }
    }

    public void onSimulatorLogin() {
    	
    	SwiftCallback cb = GuiTreadingUtils.guiThreadSafe(SwiftCallback.class, new CloudieCallbackWrapper(callback) {
            @Override
            public void onLoginSuccess() {
                super.onLoginSuccess();
                
                if (createDefaultContainerInMockMode)
                	ops.createContainer(new ContainerSpecification("default", true), callback);
            }

            @Override
            public void onError(CommandException ex) {
                JOptionPane.showMessageDialog(null,  getLocalizedString("Login_Failed") + "\n" + ex.toString(), getLocalizedString("Error"), JOptionPane.ERROR_MESSAGE);
            }
        });
    	
    	if (confirm (getLocalizedString("confirm_connection_to_simulator")))
    	{
    		if (allowCustomeSwiftSettings)
    			ops.login(AccountConfigFactory.getMockAccountConfig(), config.getSwiftSettings(), "http://localhost:8080/", "user", "pass", "secret", cb);
    		else
    			ops.login(AccountConfigFactory.getMockAccountConfig(), "http://localhost:8080/", "user", "pass", "secret", cb);
    	}
    }
    
    
	private void center(JDialog dialog) {
        int x = owner.getLocation().x + (owner.getWidth() - dialog.getWidth()) / 2;
        int y = owner.getLocation().y + (owner.getHeight() - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
    }

	
    @Override
    public void onLoginSuccess() {
        this.onNewStoredObjects();
        ops.refreshContainers(callback);
        loggedIn = true;
        enableDisable();
    }
    

    public void onAbout() {
        AboutDlg.show(this, stringsBundle);
    }
    
    
    public void onLogout() {
        ops.logout(callback);

        clearStoredObjectViews () ;
    }
    

    @Override
    public void onLogoutSuccess() {
        this.onUpdateContainers(Collections.<Container> emptyList());
        this.onNewStoredObjects();
        statusPanel.onDeselect();
        loggedIn = false;
        enableDisable();
    }

    
    public void onQuit() {
        if (onClose()) {
            System.exit(0);
        }
    }
    
    
    public void onPreferences ()
    {
        final JDialog prefDialog = new JDialog(owner, getLocalizedString("Preferences"));
        final PreferencesPanel prefPanel = new PreferencesPanel(new PreferencesCallback() {
			@Override
			public void setLanguage(LanguageCode lang, RegionCode reg) {
				
				// set the new language setting
				if (config != null)
				{
					config.updateLanguage(lang, reg);
			        info (getLocalizedString("info_new_setting_effective_next_start")) ;
				}
				prefDialog.setVisible(false);
			}
        }, config, stringsBundle);
        try {
        	prefPanel.setOwner(prefDialog);
            prefDialog.getContentPane().add(prefPanel);
            prefDialog.setModal(true);
            //prefDialog.setSize(300, 150);
            prefDialog.setResizable(false);
            prefDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            prefDialog.pack();
            center(prefDialog);
            prefDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                	prefPanel.onCancel();
                }

                @Override
                public void windowOpened(WindowEvent e) {
                	prefPanel.onShow();
                }
            });
            prefDialog.setVisible(true);
        } finally {
        	prefDialog.dispose();
        }
    }
    
    
    public void onSwiftParameters ()
    {
        final JDialog swiftParamDialog = new JDialog(owner, getLocalizedString("Swift_Parameters"));
        final SwiftPanel swiftPanel = new SwiftPanel(new SwiftPanel.SwiftSettingsCallback() {

			@Override
			public void setSwiftParameters(SwiftParameters newParameters) {

				if (config == null)
					return ;
				if (newParameters == null)
					return ;
				
				// set the new swift settings
				config.updateSwiftParameters(newParameters);
				hideSegmentsContainers = newParameters.hideSegmentsContainers() ;
				swiftParamDialog.setVisible(false);
			}

        }, config.getSwiftSettings(), stringsBundle);
        try {
        	swiftPanel.setOwner(swiftParamDialog);
        	swiftParamDialog.getContentPane().add(swiftPanel);
        	swiftParamDialog.setModal(true);
        	swiftParamDialog.setResizable(false);
        	swiftParamDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        	swiftParamDialog.pack();
            center(swiftParamDialog);
            swiftParamDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                	swiftPanel.onCancel();
                }

                @Override
                public void windowOpened(WindowEvent e) {
                	swiftPanel.onShow();
                }
            });
            swiftParamDialog.setVisible(true);
        } finally {
        	swiftParamDialog.dispose();
        }
    }
    
    
    public void onProxy() {
    	
    	final ProxiesStore proxiesStore = new ProxiesStore (config) ;
    	
        final JDialog proxyDialog = new JDialog(owner, getLocalizedString("Proxy_Setting"));
        final ProxyPanel proxyPanel = new ProxyPanel(new ProxyCallback() {
			@Override
			public void setProxy(Set<Proxy> newProxies) {
				
				if (config == null)
					return ;
				if (newProxies == null || newProxies.isEmpty())
					return ;
				
				for (Proxy proxy : newProxies)
					config.updateProxy(proxy) ;
				
				proxyDialog.setVisible(false);
			}
        }, proxiesStore, stringsBundle);
        try {
            proxyPanel.setOwner(proxyDialog);
            proxyDialog.getContentPane().add(proxyPanel);
            proxyDialog.setModal(true);
            proxyDialog.setSize(600, 210);
            proxyDialog.setResizable(false);
            proxyDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            center(proxyDialog);
            proxyDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    proxyPanel.onCancel();
                }

                @Override
                public void windowOpened(WindowEvent e) {
                    proxyPanel.onShow();
                }
            });
            proxyDialog.setVisible(true);
        } finally {
        	proxyDialog.dispose();
        }
    }

    
    private JPopupMenu createContainerPopupMenu() {
        JPopupMenu pop = new JPopupMenu("Container");
        pop.add(setAccessibleContext(new JMenuItem(containerRefreshAction)));
        pop.add(setAccessibleContext(new JMenuItem(containerViewMetaData)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(containerCreateAction)));
        pop.add(setAccessibleContext(new JMenuItem(containerDeleteAction)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(containerEmptyAction)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(containerPurgeAction)));
        return pop;
    }

    
    protected void onPurgeContainer() {
        Container c = getSelectedContainer();
        
        String msg = MessageFormat.format (getLocalizedString("confirm_msg_purging_container"), c.getName()) ;
        if (confirm(msg)) {
            ops.purgeContainer(c, getNewSwiftStopRequester(), callback);   
            clearStoredObjectViews () ;
        }
    }

    
    protected void onEmptyContainer() {
        Container c = getSelectedContainer();
        
        String msg = MessageFormat.format (getLocalizedString("confirm_msg_empty_container"), c.getName()) ;
        if (confirm(msg)) {
            ops.emptyContainer(c, getNewSwiftStopRequester(), callback);
        }
    }

    
    private void clearStoredObjectViews ()
    {
        storedObjects.clear();
        allStoredObjects.clear();
        buildTree (false) ;
    }
    
    
    protected void onRefreshContainers() {
        int idx = containersList.getSelectedIndex();
        refreshContainers();
        containersList.setSelectedIndex(idx);
        
        refreshFiles((idx < containers.size() && idx >= 0) ? ((Container) containers.get(idx)) : (null)) ;
    }

    
    protected void onDeleteContainer() {
        Container c = getSelectedContainer();
        
        String msg = MessageFormat.format (getLocalizedString("confirm_msg_delete_container"), c.getName()) ;
        if (confirm(msg)) {
            ops.deleteContainer(c, callback);
        }
    }

    
    protected void onCreateContainer() {
        ContainerSpecification spec = doGetContainerSpec();
        ops.createContainer(spec, callback);
    }

    
    private ContainerSpecification doGetContainerSpec() {
    	JTextField nameTf = new JTextField();        
        JCheckBox priv = new JCheckBox(getLocalizedString("private_container"));
        while (true)
        {
	        if (JOptionPane.showConfirmDialog(this, new Object[] { getLocalizedString("Name"), nameTf, priv }, getLocalizedString("Create_Container"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) 
	        {
	        	String name = nameTf.getText() ;
	        	if (name == null || name.trim().isEmpty())
	        	{
	        		JOptionPane.showMessageDialog(this, getLocalizedString("please_enter_non_empty_directory_name"));
	        		continue ;
	        	}
	        	if (name.endsWith(SwiftUtils.segmentsContainerPostfix))
	        	{
	        		String msg = MessageFormat.format (getLocalizedString("please_enter_a_directory_name_that_does_not_end_with__segments"), SwiftUtils.segmentsContainerPostfix) ;
	        		JOptionPane.showMessageDialog(this, msg);
	        		continue ;
	        	}
	        	return new ContainerSpecification(name.trim(), priv.isSelected());
	        }
	        break ;
        }
        return null;
    }

    
    /**
     * @return
     */
    private JPopupMenu createStoredObjectPopupMenu() {
        JPopupMenu pop = new JPopupMenu(getLocalizedString("StoredObject"));
        pop.add(setAccessibleContext(new JMenuItem(storedObjectPreviewAction)));
        pop.add(setAccessibleContext(new JMenuItem(storedObjectOpenAction)));
        pop.add(setAccessibleContext(new JMenuItem(storedObjectViewMetaData)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(storedObjectGetInfoAction)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(storedObjectUploadFilesAction)));
        pop.add(setAccessibleContext(new JMenuItem(storedObjectDownloadFilesAction)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(storedObjectCreateDirectoryAction))) ;
        pop.add(setAccessibleContext(new JMenuItem(storedObjectUploadDirectoryAction)));
        pop.add(setAccessibleContext(new JMenuItem(storedObjectDownloadDirectoryAction)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(storedObjectDeleteFilesAction)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(storedObjectDeleteDirectoryAction)));
        pop.addSeparator();
        pop.add(setAccessibleContext(new JMenuItem(storedObjectCompareFilesAction))) ;
        pop.add(setAccessibleContext(new JMenuItem(storedObjectCompareDirectoriesAction))) ;
        return pop;
    }

    
    private <T extends Component> T setAccessibleContext (T comp)
    {
    	return setAccessibleContext (comp, null) ;
    }
    
    
    private <T extends Component> T setAccessibleContext (T comp, String name)
    {
    	return SwingUtils.setAccessibleContext(comp, name) ;
    }
    
    
    public JMenuBar createMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu accountMenu = setAccessibleContext(new JMenu(getLocalizedString("Account")));
        JMenu containerMenu = setAccessibleContext(new JMenu(getLocalizedString("Container")));
        JMenu storedObjectMenu = setAccessibleContext(new JMenu(getLocalizedString("StoredObject")));
        JMenu settingsMenu = setAccessibleContext(new JMenu(getLocalizedString("Settings")));
        JMenu helpMenu = setAccessibleContext(new JMenu(getLocalizedString("Help")));
        accountMenu.setMnemonic('A');
        containerMenu.setMnemonic('C');
        storedObjectMenu.setMnemonic('O');
        helpMenu.setMnemonic('H');
        bar.add(accountMenu);
        bar.add(containerMenu);
        bar.add(storedObjectMenu);
        bar.add(settingsMenu);
        bar.add(helpMenu);
        
        //if (!nativeMacOsX)
        //	addSearchPanel (bar) ;
        	
        //
        accountMenu.add(setAccessibleContext(new JMenuItem(accountHubicLoginAction)));
        accountMenu.add(setAccessibleContext(new JMenuItem(accountGenericLoginAction)));
        accountMenu.add(setAccessibleContext(new JMenuItem(accountSimulatorLoginAction)));
        accountMenu.addSeparator();
        accountMenu.add(setAccessibleContext(new JMenuItem(accountLogoutAction)));
        if (!nativeMacOsX) 
        {
	    	accountMenu.addSeparator();
	    	accountMenu.add(setAccessibleContext(new JMenuItem(accountQuitAction)));
        }

        //
        containerMenu.add(setAccessibleContext(new JMenuItem(containerRefreshAction)));
        containerMenu.add(setAccessibleContext(new JMenuItem(containerViewMetaData)));
        containerMenu.addSeparator();
        containerMenu.add(setAccessibleContext(new JMenuItem(containerGetInfoAction)));
        containerMenu.addSeparator();
        containerMenu.add(setAccessibleContext(new JMenuItem(containerCreateAction)));
        containerMenu.add(setAccessibleContext(new JMenuItem(containerDeleteAction)));
        containerMenu.addSeparator();
        containerMenu.add(setAccessibleContext(new JMenuItem(containerEmptyAction)));
        containerMenu.addSeparator();
        containerMenu.add(setAccessibleContext(new JMenuItem(containerPurgeAction)));
        //
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectPreviewAction)));
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectOpenAction)));
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectViewMetaData)));
        storedObjectMenu.addSeparator();
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectGetInfoAction)));
        storedObjectMenu.addSeparator();
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectUploadFilesAction)));
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectDownloadFilesAction)));
        storedObjectMenu.addSeparator();
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectCreateDirectoryAction))) ;
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectUploadDirectoryAction)));
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectDownloadDirectoryAction)));
        storedObjectMenu.addSeparator();
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectDeleteFilesAction)));
        storedObjectMenu.addSeparator();
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectDeleteDirectoryAction)));
        storedObjectMenu.addSeparator();
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectCompareFilesAction))) ;
        storedObjectMenu.add(setAccessibleContext(new JMenuItem(storedObjectCompareDirectoriesAction))) ;
        
        //
        settingsMenu.add(setAccessibleContext(new JMenuItem(settingProxyAction))) ;
        if (allowCustomeSwiftSettings)
        	settingsMenu.add(setAccessibleContext(new JMenuItem(settingSwiftAction))) ;
        if (!nativeMacOsX) 
        {
	        settingsMenu.addSeparator();
	        settingsMenu.add(setAccessibleContext(new JMenuItem(settingPreferencesAction))) ;
        }
        //
        helpMenu.add(new JMenuItem(jvmInfoAction));
        if (!nativeMacOsX) 
        {
	        helpMenu.addSeparator();
	        helpMenu.add(new JMenuItem(aboutAction));
        }
        //
        return bar;
    }
    
    
    private void addSearchPanel (JComponent parent)
    {
        JPanel panel = new JPanel(new FlowLayout(SwingConstants.RIGHT, 0, 0));
        JLabel label = new JLabel(getIcon("zoom.png"));
        label.setLabelFor(searchTextField);
        label.setDisplayedMnemonic('f');
        panel.add(label);
        panel.add(searchTextField);
        parent.add(panel);
    }

       
    protected void onOpenInBrowserStoredObject() {
        Container container = getSelectedContainer();
        List<StoredObject> objects = getSelectedStoredObjects();
        if (container.isPublic()) 
        {
            for (StoredObject obj : objects) 
            {
                String publicURL = obj.getPublicURL();
                if (Desktop.isDesktopSupported()) {
                    try 
                    {
                        Desktop.getDesktop().browse(new URI(publicURL));
                    } 
                    catch (Exception e) 
                    {
                    	logger.error("Error occurred while opening the browser.", e);
                    }
                }
            }
        }
    }

    
    protected void onPreviewStoredObject() {    	

		StoredObject obj = single(getSelectedStoredObjects());
		if (SwiftUtils.isDirectory(obj))
		{
			Container container = getSelectedContainer();
			if (container == null)
				return ;
			loadDirectory (container, obj.getName(), callback) ;	
		}
		else
		{
	        if (obj.getContentLength() < 16 * 1024 * 1024) {
	            previewPanel.preview(obj.getContentType(), obj.downloadObject());
	        }
		}
    }

    
    protected void onViewMetaDataStoredObject() {
        StoredObject obj = single(getSelectedStoredObjects());
        Map<String, Object> metadata = obj.getMetadata();
        List<LabelComponentPanel> panels = buildMetaDataPanels(metadata);
        JOptionPane.showMessageDialog(this, panels.toArray(), obj.getName() + " metadata.", JOptionPane.INFORMATION_MESSAGE);
    }

    
    private List<LabelComponentPanel> buildMetaDataPanels(Map<String, Object> metadata) {
        List<LabelComponentPanel> panels = new ArrayList<LabelComponentPanel>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            JLabel comp = new JLabel(String.valueOf(entry.getValue()));
            comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
            panels.add(new LabelComponentPanel(entry.getKey(), comp));
        }
        return panels;
    }

    
    protected void onViewMetaDataContainer() {
        Container obj = getSelectedContainer();
        Map<String, Object> metadata = obj.getMetadata();
        List<LabelComponentPanel> panels = buildMetaDataPanels(metadata);
        JOptionPane.showMessageDialog(this, panels.toArray(), obj.getName() + " metadata.", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    protected void onDownloadStoredObjectDirectory() 
    {
    	List<StoredObject> sltObj = getSelectedStoredObjects() ;
    	if (sltObj != null && sltObj.size() != 1)
    		return ; // should never happen, because in such a case the menu is disable
    	StoredObject obj = sltObj.get(0) ;
    	if (!SwiftUtils.isDirectory(obj))
    		return ; // should never happen, because in such a case the menu is disable
    	
        Container container = getSelectedContainer();
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(lastFolder);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) 
        {
            File selectedDest = chooser.getSelectedFile();
            File target = new File(selectedDest, obj.getName()); 
            if (target.exists()) 
            {
            	String msg = MessageFormat.format (getLocalizedString("confirm_file_already_exists_overwite"), target.getName()) ;
                if (!confirm(msg)) 
                    return ;
            } 
            doSaveStoredObject(selectedDest, container, obj);
            //onProgressButton() ;
            lastFolder = selectedDest;
        }
    }

    
    protected void onDownloadStoredObject() 
    {    	
        Container container = getSelectedContainer();
        List<StoredObject> objList = getSelectedStoredObjects();
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(lastFolder);
        if (objList.size() == 1) {
            //chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            // set the default value
            StoredObject obj = objList.iterator().next() ;
            setDefaultFileChooserValue (chooser, obj) ;
        } else {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            for (StoredObject so : objList) 
            {
            	// We skip the directories
            	if (SwiftUtils.isDirectory(so))
            		continue ;
            	
                File target = selected;
                if (target.isDirectory()) {
                    target = new File(selected, so.getName());
                }
                if (target.exists()) {
                	
                	String msg = MessageFormat.format (getLocalizedString("confirm_file_already_exists_overwite"), target.getName()) ;
                    if (confirm(msg)) 
                    {
                        doSaveStoredObject(target, container, so);
                    }
                } 
                else 
                {
                    doSaveStoredObject(target, container, so);
                }
            }
            lastFolder = selected.isFile() ? selected.getParentFile() : selected;
        }
    }
    

    private void doSaveStoredObject(File target, Container container, StoredObject obj) {    	
        try 
        {
        	boolean stoppable = SwiftUtils.isDirectory(obj) && target.isDirectory() ;
			ops.downloadStoredObject(container, obj, target, (stoppable) ? (getNewSwiftStopRequester()) : (null), callback);
		} 
        catch (IOException e) 
        {
			logger.error(String.format("Error occurred while downloading the objects '%s'", obj.getName()), e);
		}
    }

    
    private void setDefaultFileChooserValue (JFileChooser chooser, StoredObject obj)
    {
        if (obj != null && obj.getName() != null && !obj.getName().isEmpty())
        {
        	String fileName = obj.getName() ;
        	int index = fileName.lastIndexOf(SwiftUtils.separator) ;
        	if (index > 0)
        		fileName = fileName.substring(index + 1) ;
        	chooser.setSelectedFile(new File (fileName));
        }
    }
    
    
    private FileFilter getComparisonFileFilter (final String name, final boolean directories)
    {
    	return new FileFilter () {

			@Override
			public boolean accept(File f) {
				
				if (f.isDirectory())
					return true ;				
				return f.getName().equals(name);
			}

			@Override
			public String getDescription() {
				String msg = MessageFormat.format (getLocalizedString((!directories) ? ("File_Named_") : ("Directories_Named_")), name) ;
				return String.format(msg);
			}};
    }
    
    
    private boolean checkFileNameIdentical (String name1, String name2)
    {
    	if (name1 != null && name1.equals(name2))
    		return true ;
    	String newLine = System.getProperty("line.separator");
    	StringBuilder sb = new StringBuilder () ;
    	sb.append(getLocalizedString("The_names_does_not_match")) ;
    	sb.append (": ") ;
    	sb.append(newLine) ;
    	sb.append ("- ") ;
    	sb.append (name1) ;
    	sb.append(newLine) ;
    	sb.append ("- ") ;
    	sb.append (name2) ;
		JOptionPane.showMessageDialog(null, sb.toString(), getLocalizedString("Invalid_Selection"), JOptionPane.ERROR_MESSAGE);
		return false ;
    }
    
    
    protected void onCompareFiles ()
    {
    	List<StoredObject> sltObj = getSelectedStoredObjects() ;
    	if (sltObj != null && sltObj.size() != 1)
    		return ; // should never happen, because in such a case the menu is disable
    	final StoredObject obj = sltObj.get(0) ;
    	if (SwiftUtils.isDirectory(obj))
    		return ; // should never happen, because in such a case the menu is disable
        final Container container = getSelectedContainer();
        
        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(lastFolder);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false) ;
        setDefaultFileChooserValue (chooser, obj) ;

        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(getComparisonFileFilter(obj.getBareName(), false));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
        {	
        	if (!checkFileNameIdentical (chooser.getSelectedFile().getName(), obj.getBareName()))
        		return ;
        	
        	@SuppressWarnings("unchecked")
			ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > crcb = GuiTreadingUtils.guiThreadSafe(ResultCallback.class, new ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > () {
				@Override
				public void onResult(Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > discrepancies) {
	
					showDifferencesManagementDlg (container, obj, chooser.getSelectedFile(), discrepancies, false) ;
				}}) ;
        	try 
        	{
				ops.findDifferences(container, obj, chooser.getSelectedFile(), crcb, null, callback);
			} 
        	catch (IOException e) 
        	{
        		logger.error("Error occurred while comparing files.", e);
			}
        }
        lastFolder = chooser.getCurrentDirectory();
    }
    
    
    private void showDifferencesManagementDlg (Container container, StoredObject srcStoredObject, File srcFile, Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > discrepancies, boolean directories)
    {
    	if (differencesManagementDlg != null && differencesManagementDlg.isShowing()) 
    		differencesManagementDlg.setVisible(false) ;
    	
    	if (discrepancies == null || discrepancies.isEmpty())
    	{
    		String keyMsg = "Files_are_identical" ;
    		String keyTitle = "Files_Comparison" ;
    		if (directories)
    		{
        		keyMsg = "Directories_are_identical" ;
        		keyTitle = "Directories_Comparison" ;
    		}
    		JOptionPane.showMessageDialog(this, getLocalizedString(keyMsg), getLocalizedString(keyTitle), JOptionPane.INFORMATION_MESSAGE);
    		return ;
    	}
    
    	differencesManagementDlg = new JDialog (owner) ;
    	DifferencesManagementPanel differencesManagementPanel = new DifferencesManagementPanel (container, srcStoredObject, srcFile, discrepancies, ops, this, stringsBundle, differencesManagementDlg, owner.getSize()) ;
    	differencesManagementDlg.setContentPane(differencesManagementPanel);
    	differencesManagementDlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    	differencesManagementDlg.setTitle(getLocalizedString("Differences_View_And_Management"));
    	differencesManagementDlg.pack();
    	center (differencesManagementDlg) ;
    	differencesManagementDlg.setVisible(true);
    }
    
    
    protected void onCompareDirectories ()
    {
    	List<StoredObject> sltObj = getSelectedStoredObjects() ;
    	if (sltObj != null && sltObj.size() != 1)
    		return ; // should never happen, because in such a case the menu is disable
    	final StoredObject obj = sltObj.get(0) ;
    	if (!SwiftUtils.isDirectory(obj))
    		return ; // should never happen, because in such a case the menu is disable
        final Container container = getSelectedContainer();
        
        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(lastFolder);
        //chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false) ;
        setDefaultFileChooserValue (chooser, obj) ;

        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(getComparisonFileFilter(obj.getBareName(), true));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
        {	
        	if (!checkFileNameIdentical (chooser.getSelectedFile().getName(), obj.getBareName()))
        		return ;
        	
        	@SuppressWarnings("unchecked")
			ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > crcb = GuiTreadingUtils.guiThreadSafe(ResultCallback.class, new ResultCallback<Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > > () {
				@Override
				public void onResult(Collection<Pair<? extends ComparisonItem, ? extends ComparisonItem> > discrepancies) {

					showDifferencesManagementDlg (container, obj, chooser.getSelectedFile(), discrepancies, true) ;
				}}) ;
        	try 
        	{
				ops.findDifferences(container, obj, chooser.getSelectedFile(), crcb, getNewSwiftStopRequester(), callback);
        		//onProgressButton();
			} 
        	catch (IOException e) 
        	{
        		logger.error("Error occurred while comparing files.", e);
			}
        }
        lastFolder = chooser.getCurrentDirectory();
    }
    
    
    protected void onDeleteStoredObjectDirectory () 
    {
        Container container = getSelectedContainer();
    	List<StoredObject> sltObj = getSelectedStoredObjects() ;
    	if (sltObj != null && sltObj.size() != 1)
    		return ; // should never happen, because in such a case the menu is disable
    	StoredObject obj = sltObj.get(0) ;
    	if (!SwiftUtils.isDirectory(obj))
    		return ; // should never happen, because in such a case the menu is disable
    	
    	doDeleteObjectDirectory(container, obj) ;
    	//onProgressButton() ;
    }
    
    
    private SwiftOperationStopRequesterImpl getNewSwiftStopRequester ()
    {
    	SwiftOperationStopRequesterImpl stopRequester = new SwiftOperationStopRequesterImpl () ;
    	stopper.addObserver(stopRequester);
    	return stopRequester ;
    }
    
    
    protected void doDeleteObjectDirectory(Container container, StoredObject obj) {
    	
    	String msg = MessageFormat.format (getLocalizedString("confirm_one_directory_delete_from_container"), obj.getName(), container.getName()) ;
        if (confirm(msg)) {
        	//onProgressButton () ;
        	ops.deleteDirectory(container, obj, getNewSwiftStopRequester (), callback) ;
        }
    }
    
    
    protected void onDeleteStoredObject() {
        Container container = getSelectedContainer();
        List<StoredObject> objects = getSelectedStoredObjects();
        if (objects.size() == 1) {
            doDeleteSingleObject(container, single(objects));
        } else {
            doDeleteMultipleObjects(container, objects);
        }
    }
    

    protected void doDeleteSingleObject(Container container, StoredObject obj) {
    	
    	String msg = MessageFormat.format (getLocalizedString("confirm_one_file_delete_from_container"), obj.getName(), container.getName()) ;
        if (confirm(msg)) {
            ops.deleteStoredObjects(container, Collections.singletonList(obj), getNewSwiftStopRequester(), callback);
        }
    }

    
    protected void doDeleteMultipleObjects(Container container, List<StoredObject> obj) {
    	
    	String msg = MessageFormat.format (getLocalizedString("confirm_many_files_delete_from_container"), String.valueOf(obj.size()), container.getName()) ;
    	if (confirm(msg)) {
    		//onProgressButton () ;
            ops.deleteStoredObjects(container, obj, getNewSwiftStopRequester(), callback);
        }
    }

    
    protected void onCreateStoredObject() 
    {
    	List<StoredObject> sltObj = getSelectedStoredObjects() ;
    	if (sltObj != null && sltObj.size() > 1)
    		return ; // should never happen, because in such a case the menu is disable
    	StoredObject parentObject = (sltObj == null || sltObj.isEmpty()) ? (null) : (sltObj.get(0)) ;
    	
        Container container = getSelectedContainer();
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setCurrentDirectory(lastFolder);
        
        final JPanel optionPanel = getOptionPanel ();
        chooser.setAccessory(optionPanel);
        AbstractButton overwriteCheck = setOverwriteOption (optionPanel) ;
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
        {
            File[] selectedFiles = chooser.getSelectedFiles();
            try 
            {
            	boolean overwriteAll = overwriteCheck.isSelected() ;
            	if (overwriteAll)
            	{
            		if (!confirm(getLocalizedString("confirm_overwrite_any_existing_files")))
            			return ;
            	}
				ops.uploadFiles(container, parentObject, selectedFiles, overwriteAll, getNewSwiftStopRequester (), callback);
				
				// We open the progress window, for it is likely that this operation
				// will take a while
				//if (selectedFiles != null /*&& selectedFiles.length > 1*/)
				//{
		            //onProgressButton () ;
				//}
			} 
            catch (IOException e) 
			{
				logger.error("Error occurred while uploading a files.", e);
			}
            lastFolder = chooser.getCurrentDirectory();
        }
    }
    
    
    private LabelComponentPanel buildLabelComponentPanel (String str, Object obj)
    {
        JLabel comp = new JLabel(String.valueOf(obj));
        comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
        return new LabelComponentPanel(str, comp);
    }
   
    
    protected void onGetInfoContainer() 
    {
        Container obj = getSelectedContainer();
        
        List<LabelComponentPanel> panels = new ArrayList<LabelComponentPanel>();
        panels.add(buildLabelComponentPanel("Container name", obj.getName()));
        panels.add(buildLabelComponentPanel("Bytes used", FileUtils.humanReadableByteCount(obj.getBytesUsed(), true)));
        panels.add(buildLabelComponentPanel("Object count", obj.getCount()));
        panels.add(buildLabelComponentPanel("Path", obj.getPath()));
        panels.add(buildLabelComponentPanel("Max page size", obj.getMaxPageSize()));
        panels.add(buildLabelComponentPanel("Is Public?", ((obj.isPublic())?("Yes"):("No"))));
        
        //TODO: more info
        // ...
        
        JOptionPane.showMessageDialog(this, panels.toArray(), obj.getName() + " Info", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    protected void onGetInfoStoredObject() 
    {
    	String charset = "UTF-8";
    	
        StoredObject obj = single(getSelectedStoredObjects());
        List<LabelComponentPanel> panels = new ArrayList<LabelComponentPanel>();
        try 
        {	
	        panels.add(buildLabelComponentPanel("Object name", obj.getName()));
	        panels.add(buildLabelComponentPanel("Object path", URLDecoder.decode(obj.getPath(), charset)));
	        panels.add(buildLabelComponentPanel("Content type", obj.getContentType()));
	        panels.add(buildLabelComponentPanel("Content length", FileUtils.humanReadableByteCount(obj.getContentLength(), true)));
	        panels.add(buildLabelComponentPanel("E-tag", obj.getEtag()));
	        panels.add(buildLabelComponentPanel("Last modified", obj.getLastModified()));
	        
	        if (obj.getManifest() != null)
	        {
	        	panels.add(buildLabelComponentPanel("Segmented", true));
				panels.add(buildLabelComponentPanel("Manifest", URLDecoder.decode(obj.getManifest(), charset)));
	        }
	        else
	        	panels.add(buildLabelComponentPanel("Segmented", false));
		} 
        catch (UnsupportedEncodingException e) 
		{
			logger.error("Error occurred while showing information", e);
		}

        //TODO: more info
        // ...
        
        JOptionPane.showMessageDialog(this, panels.toArray(), obj.getName() + " Info", JOptionPane.INFORMATION_MESSAGE);
    }
    
    
    protected void onCreateDirectory() 
    {
    	Container container = getSelectedContainer();
    	if (container == null)
    		return ;
    	List<StoredObject> sltObj = getSelectedStoredObjects() ;
    	if (sltObj != null && sltObj.size() > 1)
    		return ; // should never happen, because in such a case the menu is disable
    	StoredObject parentObject = (sltObj == null || sltObj.isEmpty()) ? (null) : (sltObj.get(0)) ;

        JTextField directoryName = new JTextField();
        while (true)
        {
	        if (JOptionPane.showConfirmDialog(this, new Object[] { getLocalizedString("Directory_Name"), directoryName}, getLocalizedString("Create_Directory"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) 
	        {
	        	String name = directoryName.getText() ;
	        	if (name == null || name.trim().isEmpty())
	        	{
	        		JOptionPane.showMessageDialog(this, getLocalizedString("please_enter_non_empty_directory_name"));
	        		continue ;
	        	}
	        	else
	        		ops.createDirectory(container, parentObject, name.trim(), callback);
	        }
	        break ;
        }
    }
    
    
    private AbstractButton setOverwriteOption (JPanel optionPanel)
    {
    	if (optionPanel == null)
    		return null ;
        final JCheckBox overwriteCheck = new JCheckBox(getLocalizedString("Overwrite_existing_files"));
        overwriteCheck.addItemListener(new ItemListener (){
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (overwriteCheck.isSelected())
				{
					overwriteCheck.setOpaque(true);
					overwriteCheck.setBackground(Color.RED);
				}
				else
					overwriteCheck.setOpaque(false);
			}}) ;
        optionPanel.add(overwriteCheck);
        return overwriteCheck ;
    }
    
    
    private JPanel getOptionPanel ()
    {
        final JPanel optionPanel = new JPanel();
        optionPanel.setBorder(BorderFactory.createTitledBorder(getLocalizedString("Options")));
        return optionPanel ;
    }
    
    
    protected void onUploadDirectory() 
    {
    	List<StoredObject> sltObj = getSelectedStoredObjects() ;
    	if (sltObj != null && sltObj.size() > 1)
    		return ; // should never happen, because in such a case the menu is disable
    	StoredObject parentObject = (sltObj == null || sltObj.isEmpty()) ? (null) : (sltObj.get(0)) ;
    	
        final Container container = getSelectedContainer();
        final JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setCurrentDirectory(lastFolder);
        chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY);
        
        final JPanel optionPanel = getOptionPanel ();
        chooser.setAccessory(optionPanel);
        AbstractButton overwriteCheck = setOverwriteOption (optionPanel) ;
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
        {
            final File selectedDir = chooser.getSelectedFile();
            try 
            {
            	boolean overwriteAll = overwriteCheck.isSelected() ;
            	if (overwriteAll)
            	{
            		if (!confirm(getLocalizedString("confirm_overwrite_any_existing_files")))
            			return ;
            	}
				ops.uploadDirectory(container, parentObject, selectedDir, overwriteAll, getNewSwiftStopRequester (), callback);
				
				// We open the progress window, for it is likely that this operation
				// will take a while
	            //onProgressButton () ;
			} 
            catch (IOException e) 
			{
				logger.error("Error occurred while uploading a directory.", e);
			}
            lastFolder = chooser.getCurrentDirectory();
        }
    }
    

    public void bind() {
        containersList.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                } else {
                    int idx = containersList.getSelectedIndex();
                    if (idx >= 0) {
                        refreshFiles((Container) containers.get(idx));
                    }
                }
            }
        });
        //
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable ex) {
                if (ex instanceof CommandException) {
                    showError((CommandException) ex);
                } else if (ex instanceof OutOfMemoryError) {
                	System.gc() ; // pointless at this stage, but anyway...
                	showError(new CommandException ("The JVM ran out of memory - <font color=red><b>You must exit</b></font>")) ;
                } else {
                	//showError(new CommandException ("An unidentified error has occurred")) ;
                    logger.error("Error occurred", ex);
                }
            }

        });
        //
        containersList.getInputMap().put(KeyStroke.getKeyStroke("F5"), "refresh");
        containersList.getActionMap().put("refresh", containerRefreshAction);
        //
        storedObjectsList.getInputMap().put(KeyStroke.getKeyStroke("F5"), "refresh");
        storedObjectsList.getActionMap().put("refresh", containerRefreshAction);
        //
    }

    
    public void refreshContainers() {
        containers.clear();
        storedObjects.clear();
        ops.refreshContainers(callback);
    }

    
    public void refreshFiles(Container selected) {
    	
        //storedObjects.clear();
        //if (selected != null)
        //	ops.refreshStoredObjects(selected, callback);

        storedObjects.clear();
        treeHasBeenExpandedNodeSet.clear();
        if (selected != null)
        	ops.refreshDirectoriesOrStoredObjects(selected, null, callback);
    }
    
    
    public void onProgressButton ()
    {    	
    	if (progressPanel.isShowing())
    		return ;
    
    	JDialog dlg = new JDialog (owner) ;
    	dlg.setContentPane(progressPanel);
    	dlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    	dlg.setTitle(getLocalizedString("Progress"));
    	dlg.pack();
    	center (dlg) ;
    	dlg.setVisible(true);
    }
    
    
    public void onStop ()
    {
		if (confirm (getLocalizedString("confirm_stop_operation")))
			stopper.stop() ;
		enableOperationMenu() ;
    }
    
    
    public void onSearch() {
        storedObjects.clear();
        synchronized (allStoredObjects)
        {
	        for (StoredObject storedObject : allStoredObjects) {
	            if (isFilterIncluded(storedObject)) {
	                storedObjects.addElement(storedObject);
	            }
	        }
        }
        //set the tablist
        if (allStoredObjects.size() != storedObjects.size())
        	objectViewTabbedPane.setSelectedIndex(listviewTabIndex);
        buildTree (true) ;
    }
    
    
    private boolean isFilterIncluded(StoredObject obj) {
        String filter = searchTextField.getText();
        if (filter.isEmpty()) {
            return true;
        } else {
            return obj.getName().contains(filter);
        }
    }

    //
    // Callback
    //

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onStart() {

        if (busyCnt.getAndIncrement() == 0) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            statusPanel.onStart();

            progressButton.setEnabled(true);
            progressPanel.start();

            enableDisable();
            
            ComputerSleepingManager.INSTANCE.keepAwake(true);
            
            onProgressButton();
        }
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public void onDone() {

        if (busyCnt.decrementAndGet() == 0) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            statusPanel.onEnd();
            
            stopper.deleteObservers(); // because busyCnt equals zero
            
            ComputerSleepingManager.INSTANCE.keepAwake(false);
            
            progressButton.setEnabled(false);
            progressPanel.done() ;
            Window progressWindow = SwingUtilities.getWindowAncestor(progressPanel);            
            if (progressWindow != null)
            	progressWindow.setVisible(false) ;
        }
        enableDisable();
    }

    
    private void closeOpsDependantDialogs ()
    {
    	if (differencesManagementDlg != null && differencesManagementDlg.isShowing()) 
    		differencesManagementDlg.setVisible(false) ;
    }
    
    
    /**
     * {@inheritDoc}.
     */
    @Override
    public void onError(final CommandException ex) {
    	
    	closeOpsDependantDialogs () ;
    	
    	showError(ex);
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public void onUpdateContainers(final Collection<Container> cs) {

        containers.clear();
        for (Container container : cs) {
        	if (hideSegmentsContainers)
        	{
        		if (container.getName().endsWith(SwiftUtils.segmentsContainerPostfix))
        			continue ;
        	}
            containers.addElement(container);
        }
        statusPanel.onDeselectContainer();
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public void onStoredObjectDeleted(final Container container, final StoredObject storedObject) {

        if (isContainerSelected() && getSelectedContainer().equals(container)) {
            int idx = storedObjects.indexOf(storedObject);
            if (idx >= 0) {
                storedObjectsList.getSelectionModel().removeIndexInterval(idx, idx);
            }
            storedObjects.removeElement(storedObject);
            allStoredObjects.remove(storedObject);
            
            buildTree (true) ;
        }
    }
    
    
    /**
     * {@inheritDoc}.
     */
    @Override
    public void onStoredObjectDeleted(final Container container, final Collection<StoredObject> storedObjects) {

        if (isContainerSelected() && getSelectedContainer().equals(container)) 
        {
        	allStoredObjects.removeAll(storedObjects);
	    	StoredObject obj = storedObjectsList.getSelectedValue() ;
	    	this.storedObjects.clear();
		    for (StoredObject storedObject : allStoredObjects) {
		        if (isFilterIncluded(storedObject)) {
		        	this.storedObjects.addElement(storedObject);
		        }
		    }
		    if (obj != null && obj.exists())
		    	storedObjectsList.setSelectedValue(obj, true);
		    
		    buildTree (true) ;
        }
    }
    
    
    /**
     * {@inheritDoc}.
     */
    @Override
    public void onNewStoredObjects() {
	
        searchTextField.setText("");
        storedObjects.clear();
        allStoredObjects.clear();
        statusPanel.onDeselectStoredObject();
        treeHasBeenExpandedNodeSet.clear();

        buildTree (true) ;
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public void onAppendStoredObjects(final Container container, int page, final Collection<StoredObject> sos) {

		if (isContainerSelected() && getSelectedContainer().equals(container)) {
		    allStoredObjects.addAll(sos);

	    	/*
		    for (StoredObject storedObject : sos) {
		        if (isFilterIncluded(storedObject)) {
		            storedObjects.addElement(storedObject);
		        }
		    }
		    
	    	updateTree (sos) ;
	    	StoredObjectsTreeModel.TreeUtils.setTreeExpansionState(tree, treeExpansionState);

		    System.gc() ;
		    */
	    	
	    	StoredObject obj = storedObjectsList.getSelectedValue() ;
	    	storedObjects.clear();
		    for (StoredObject storedObject : allStoredObjects) {
		        if (isFilterIncluded(storedObject)) {
		            storedObjects.addElement(storedObject);
		        }
		    }
		    if (obj != null)
		    	storedObjectsList.setSelectedValue(obj, true);
		    
	    	updateTree (sos) ;
	    	StoredObjectsTreeModel.TreeUtils.setTreeExpansionState(tree, treeExpansionState);
		}
    }
        
    
    /**
     * {@inheritDoc}.
     */
	@Override
	public void onProgress(final double totalProgress, String totalMsg, final double currentProgress, final String currentMsg) {

		if (stopper.isStopRequested())
		{
			StringBuilder sb = new StringBuilder () ;
			sb.append ("<html>") ;
			sb.append (totalMsg) ;
			sb.append ("<font color=red><b> (is stopping...)</b></font>") ;
			sb.append ("</html>") ;
			totalMsg = sb.toString() ;
		}
		progressPanel.setProgress(totalProgress, totalMsg, currentProgress, currentMsg);
	}

	
    /**
     * {@inheritDoc}.
     */
	@Override
	public void onStopped() {

		closeOpsDependantDialogs () ;
		
	}
	

    /**
     * {@inheritDoc}.
     */
    @Override
    public void onContainerUpdate(final Container container) {
    	statusPanel.onSelectContainer(container);
    }

    
    /**
     * {@inheritDoc}.
     */
    @Override
    public void onStoredObjectUpdate(final StoredObject obj) {
    	statusPanel.onSelectStoredObjects(Collections.singletonList(obj));
    }


    /**
     * @return true if the window can close.
     */
    public boolean onClose() {
        return (loggedIn && confirm(getLocalizedString("confirm_quit_application"))) || (!loggedIn);
    }

    
    public boolean confirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, getLocalizedString("Confirmation"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
    
    
    public void info(String message) {
        JOptionPane.showMessageDialog(this, message) ;
    }
    

    protected void showError(CommandException ex) {

    	String msg = ex.getMessage() ;
    	if (msg == null)
    	{
    		if (ex.getError() == CommandExceptionError.UNAUTHORIZED)
    			msg = "Authorization Anomaly" ;
    		else
    			msg = "Unknown Error" ;
    		
    	}
    	errorLogPanel.add(msg, LogPanel.LogType.ERROR); 
    	
    	if (errorDialog != null && errorDialog.isVisible())
    		errorDialog.setVisible(false);
    	
		errorDialog = new ErrorDlg (owner, errorLogPanel, stringsBundle) ;
		errorDialog.setTitle(getLocalizedString("Error"));
		errorDialog.setResizable(true);
		errorDialog.setModal(true);
		errorDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		errorDialog.pack();	
		center (errorDialog) ;
		errorDialog.setVisible(true);
    }
}
