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

package org.swiftexplorer.gui;


import org.swiftexplorer.swift.util.SwiftUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;

public class StoredObjectsTreeModel implements TreeModel
{	
	public static class TreeUtils{
		
		private TreeUtils () { super () ; } ;
	    
	    public static List<TreePath> getTreeExpansionState (JTree tree)
	    {
	    	if (tree == null)
	    		return null ;
	    	List<TreePath> ret = new ArrayList<TreePath> () ;
	    	for (int i = 0 ; i < tree.getRowCount() ; i++)
	    	{
	    		TreePath path = tree.getPathForRow(i) ;
	    		if (tree.isExpanded(path))
	    			ret.add (path) ;
	    	}
	    	return ret ;
	    }
	    
	    public static void setTreeExpansionState (JTree tree, List<TreePath> list)
	    {
	    	if (list == null || tree == null)
	    		return ;
	    	for (TreePath path : list)
	    	{
	    		tree.expandPath(path);
	    	}
	    }
	}
	
	
	public static interface TreeNode extends Comparable <TreeNode> 
	{
        /**
         * @return the node name (the one shown on the tree)
         */
		public String getNodeName () ;
		
        /**
         * @return the object name, that UNIQUELY identifies this node (it is typically the StoredObject's name)
         */
		public String getObjectName () ;
		
        /**
         * @return true if this node is the root of the tree, false otherwise
         */
		public boolean isRoot () ;
		
        /**
         * OpenStack Swift does not really have the notion of directory (this is a key-object map).
         * However, the directories can be somehow "simulated" (see "Pseudo-hierarchical folders and directories" at 
         * http://docs.openstack.org/api/openstack-object-storage/1.0/content/pseudo-hierarchical-folders-directories.html).
         * Basically, the trick is to use forward slash characters ('/') in the object name.
         * 
         * Alternatively, it is also possible to create empty objects with the content type "application/directory". 
         * Such objects, though they are not directories as commonly understood, they exist in the storage, and therefore
         * they can be manipulated (removed, move, etc.). In contrast, a purely virtual directory simulated using the 
         * characters ('/') in the name of objects cannot be manipulated, for it does not exist at all.
         * 
         * @return true if this node is associated with a purely virtual directory that is not embodied by any object, false otherwise. 
         */
		public boolean isVirtual () ;
		
        /**
         * @return the storedObject associated with this node, or null if there is no such an object
         */
		public StoredObject getStoredObject () ;
		
        /**
         * @return the container associated with this node, or null (only the root have a container object)
         */
		public Container getContainer () ;
	}
	
	
	private static class TreeNodeImpl implements TreeNode
	{
		private final StoredObject storedObject ;
		private final String objectName ;
		private final String nodeName ;
		private final boolean isRoot ;
		private final Container container ;
		
		
		public static TreeNodeImpl buildRootNode (Container container)
		{
			String contName = (container == null) ? ("") : (container.getName()) ;
			return new TreeNodeImpl (null, contName, contName, true, container) ;
		}
		
		
		public static TreeNodeImpl buildObjectNode (StoredObject storedObject, String objectName, String nodeName)
		{
			return new TreeNodeImpl (storedObject, objectName, nodeName, false, null) ;
		}
		
		
		private TreeNodeImpl(StoredObject storedObject, String objectName, String nodeName, boolean isRoot, Container container) {
			super();
			if (isRoot == true && storedObject != null)
				throw new AssertionError ("A root node cannot have a non-null storedObject") ;
			if (objectName == null)
				throw new AssertionError ("A node must have a non-null objectName") ;
			if (nodeName == null)
				throw new AssertionError ("A node must have a non-null nodeName") ;
			
			this.storedObject = storedObject;
			this.nodeName = nodeName ;
			this.objectName = objectName ;
			this.isRoot = isRoot ;
			this.container = container ;
		}

		
		@Override
		public StoredObject getStoredObject ()
		{
			return storedObject ;
		}
		
		
		@Override
		public String getObjectName() {
			return objectName;
		}
		
		
		@Override
		public String getNodeName() {
			return nodeName;
		}

		
		@Override
		public boolean isRoot() {
			return isRoot;
		}

		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (isRoot ? 1231 : 1237);
			result = prime * result
					+ ((objectName == null) ? 0 : objectName.hashCode());
			return result;
		}
		

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TreeNodeImpl other = (TreeNodeImpl) obj;
			if (isRoot != other.isRoot)
				return false;
			if (objectName == null) {
				if (other.objectName != null)
					return false;
			} else if (!objectName.equals(other.objectName))
				return false;
			return true;
		}

		
		@Override
		public int compareTo(TreeNode o) {
			return objectName.compareTo(o.getObjectName());
		}

		
		@Override
		public String toString() {
			return nodeName ;
		}

		
		@Override
		public boolean isVirtual() {
			return !isRoot && storedObject == null ;
		}

		
		@Override
		public Container getContainer() {
			return container;
		}
	}
	
	private Set<TreeModelListener> treeModelListeners = Collections.synchronizedSet(new HashSet<TreeModelListener>());
	
	private volatile TreeNode rootNode= null ;
	
	private final String delimiter = SwiftUtils.separator ;
	
	private final Map<TreeNode, Set<TreeNode> > mapParentChildMap = Collections.synchronizedMap(new TreeMap<TreeNode, Set<TreeNode> >()) ;
	
	
	public StoredObjectsTreeModel(Container rootContainer, Collection<StoredObject> allStoredObjects) {
		super();
		rootNode = TreeNodeImpl.buildRootNode(rootContainer) ; 
		initialize (allStoredObjects) ;
	}
	
	
    protected void fireTreeStructureChanged() {
    	if (rootNode == null)
    		return ;
        TreeModelEvent e = new TreeModelEvent(this, new Object[] {rootNode});
        for (TreeModelListener tml : treeModelListeners) {
            tml.treeStructureChanged(e);
        }
    }
	
    
	private synchronized void initialize (Collection<StoredObject> storedObjectsList)
	{
		if (rootNode == null)
			throw new AssertionError ("The root cannot be null when initializing") ;
		mapParentChildMap.clear();
		mapParentChildMap.put(rootNode, new TreeSet<TreeNode> ()) ;
		processList (storedObjectsList) ;
	}
	
	
	public synchronized void addAll (Collection<StoredObject> newStoredObjectsList)
	{		
		processList (newStoredObjectsList) ;
		fireTreeStructureChanged () ;
	}
	
	
	private void processList (Collection<StoredObject> list)
	{
		if (list == null)
			return ;
		Map<String, StoredObject> objectMap = new HashMap<String, StoredObject> () ;
		for (StoredObject so : list)
		{
			objectMap.put(so.getName(), so) ;
		}
		
		for (StoredObject so : list)
		{
			String path = so.getName() ;
			String [] splitted = path.split(delimiter) ;
			
			if (splitted.length == 1)
			{
				TreeNode newNode = TreeNodeImpl.buildObjectNode(so, splitted[0], splitted[0]) ;
				if (!mapParentChildMap.containsKey(newNode))
					mapParentChildMap.put(newNode, new TreeSet<TreeNode> ()) ;
				mapParentChildMap.get(rootNode).add(newNode) ;
			}
			else
			{
				StringBuilder objectName = new StringBuilder () ;
				for (int i = 0 ; i < splitted.length - 1 ; ++i)
				{			
					// Current Object
					if (i > 0)
						objectName.append(delimiter) ;
					objectName.append(splitted[i]) ;
					String currObjName = objectName.toString() ;
					TreeNode newNode = TreeNodeImpl.buildObjectNode(objectMap.get(currObjName), currObjName, splitted[i]) ;
					if (i == 0)
						mapParentChildMap.get(rootNode).add(newNode) ;
					
					// Next object
					StringBuilder childObjNameBuilder = new StringBuilder () ;
					childObjNameBuilder.append(currObjName) ;
					childObjNameBuilder.append(delimiter) ;
					childObjNameBuilder.append(splitted[i+1]) ;
					String childObjName = childObjNameBuilder.toString() ;
					TreeNode childNode = TreeNodeImpl.buildObjectNode(objectMap.get(childObjName), childObjName, splitted[i+1]) ;
					
					if (!mapParentChildMap.containsKey(newNode))
						mapParentChildMap.put(newNode, new TreeSet<TreeNode> ()) ;
					mapParentChildMap.get(newNode).add(childNode) ; 
				}
				// the last one
				if (splitted.length > 0)
				{
					String name = splitted[splitted.length - 1] ;
					StoredObject sobj = objectMap.get(name) ;
					if (sobj != null && SwiftUtils.directoryContentType.equalsIgnoreCase(sobj.getContentType()))
					{
						TreeNode newNode = TreeNodeImpl.buildObjectNode(sobj, name, name) ;
						if (!mapParentChildMap.containsKey(newNode))
							mapParentChildMap.put(newNode, new TreeSet<TreeNode> ()) ;
					}
				}
			}
		}
	}

	
	@Override
	public synchronized Object getRoot() {
		return rootNode ;
	}

	
	@Override
	public synchronized Object getChild(Object parent, int index) {

		if (!(parent instanceof TreeNode))
			return null ;
		if (!mapParentChildMap.containsKey(parent))
			return null;
		Set<TreeNode> children = mapParentChildMap.get(parent);
		if (children == null)
			return null;
		
		// TODO: really bad! Should be improved!
		// ...
		if (index < children.size())
		{
			int count = 0 ;
			for (TreeNode t : children)
			{
				if (count >= index)
					return t ;
				++count ;
			}
			return null ;
			// even worse...
			//return children.toArray()[index] ;
		}
		else
			return null ;
	}

	
	@Override
	public synchronized int getChildCount(Object parent) {
		if (!(parent instanceof TreeNode))
			return 0 ;
		if (!mapParentChildMap.containsKey(parent))
			return 0;
		Set<TreeNode> children = mapParentChildMap.get(parent);
		if (children == null)
			return 0;
		return children.size() ;	
	}
	

	@Override
	public synchronized boolean isLeaf(Object node) {
		if (!(node instanceof TreeNode))
			return false ;
		if (((TreeNode)node).isRoot())
			return mapParentChildMap.size() == 1 ;
		if(!mapParentChildMap.containsKey((TreeNode)node))
			return true ;
		if (mapParentChildMap.get((TreeNode)node) == null)
			return true ;
		if (!mapParentChildMap.get((TreeNode)node).isEmpty())
			return false ;
		StoredObject so = ((TreeNode)node).getStoredObject() ;
		if (so == null)
			return false ; // a folder (virtual)
		if ("application/directory".equalsIgnoreCase(so.getContentType()))
			return false ; // a folder
		return true;
	}

	
	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
	}

	
	@Override
	public synchronized int getIndexOfChild(Object parent, Object child) {
		if (parent == null || child == null)
			return -1 ;
		if (!(parent instanceof TreeNode) || !(child instanceof TreeNode))
			return -1 ;

		if (!mapParentChildMap.containsKey((TreeNode)parent))
			return -1 ;
		
		Set<TreeNode> children = mapParentChildMap.get(parent);
		if (children == null)
			return -1;
		if (!children.contains(child))
			return -1 ;
		if (children instanceof TreeSet)
		{
			// this works because here we are sure that child is in children
			return ((TreeSet<TreeNode>)children).headSet((TreeNode) child).size() ;
		}
		else
		{
			// not very nice! But never used, for the Set should be a TreeSet...
			int count = 0 ;
			for (TreeNode tn : children)
			{
				if (tn.equals(child))
					break ;
				++count ;
			}
			return count;
		}
	}

	
	@Override
	public void addTreeModelListener(TreeModelListener l) {
		treeModelListeners.add(l) ;
	}
	

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.remove(l) ;
	}
}
