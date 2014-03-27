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

import static org.junit.Assert.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.javaswift.joss.model.Container;
import org.javaswift.joss.model.StoredObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.swiftexplorer.gui.StoredObjectsTreeModel.TreeNode;
import org.swiftexplorer.swift.util.SwiftUtils;

public class StoredObjectsTreeModelTest {
	
	private Container rootContainer ;
	private final List<StoredObject> storedObjectList = new ArrayList<StoredObject> ();
	
	private final int numberOfLevel = 4 ;
	private final int numberOfChildren = 8 ;
	
	
	private static class Pair<L, O>
	{
		private final L l ;
		private final O o ;
		private Pair(L l, O o) {
			super();
			this.l = l;
			this.o = o;
		}
		public L getL() {
			return l;
		}
		public O getO() {
			return o;
		}
		
		public static <L, O> Pair<L, O> newPair (L l, O o)
		{
			return new Pair<L, O> (l, o) ;
		}
	}
	
	
	private String buildNodeName (String parentName, int level, int row)
	{
		StringBuilder nodeNameBuilder = new StringBuilder () ;
		nodeNameBuilder.append(buildBaseNodeName (parentName, level)) ;
		nodeNameBuilder.append(row) ;
		
		return nodeNameBuilder.toString() ;
	}
	
	
	private String buildBaseNodeName (String parentName, int level)
	{
		StringBuilder nodeNameBuilder = new StringBuilder () ;
		if (parentName != null && !parentName.isEmpty())
		{
			nodeNameBuilder.append(parentName) ;
			nodeNameBuilder.append(SwiftUtils.separator) ;
		}
		nodeNameBuilder.append("node_") ;
		nodeNameBuilder.append(level) ;
		nodeNameBuilder.append("-") ;
		
		return nodeNameBuilder.toString() ;
	}
	
	
    @Before
    public void init() {

    	// the root
    	rootContainer =  Mockito.mock(Container.class) ;
    	Mockito.when(rootContainer.getName()).thenReturn("root") ;
    	
    	// initialize the list
    	// just adapt a "breadth-first search" method
    	Queue<Pair<Integer, StoredObject> > queue = new  ArrayDeque<Pair<Integer, StoredObject> > () ;
    	
    	for (int i = 0 ; i < numberOfChildren ; ++i)
    	{
    		int level = 1 ;
    		StoredObject so = Mockito.mock(StoredObject.class) ;
    		Mockito.when(so.getName()).thenReturn(buildNodeName (null, level, i)) ;
    		queue.add(Pair.newPair(level, so));
    	}
    	while (!queue.isEmpty())
    	{
    		Pair<Integer, StoredObject> currNode = queue.poll() ;
    		storedObjectList.add(currNode.getO());
    			
    		int level = currNode.getL() + 1 ;
    		if (level > numberOfLevel)
    			continue ;
    		
    		// children
        	for (int i = 0 ; i < numberOfChildren ; ++i)
        	{
	    		StoredObject so = Mockito.mock(StoredObject.class) ;	    		
	    		String name = buildNodeName (currNode.getO().getName(), level, i) ;
	    		Mockito.when(so.getName()).thenReturn(name) ;
	    		queue.add(Pair.newPair(level, so));
        	}
    	}
    	
    	// Let's shuffle the list to add some pigment!
    	long seed = System.nanoTime();
    	Collections.shuffle(storedObjectList, new Random(seed));
    }
    
    
    private void verifyTreeStructure (StoredObjectsTreeModel treeModel)
    {
    	// First, let's check the root
    	TreeNode root = (TreeNode) treeModel.getRoot() ;
    	
    	assertTrue (root.isRoot()) ;
    	assertTrue (root.getContainer() == rootContainer) ;
    	assertTrue (treeModel.getChildCount(root) == numberOfChildren) ;
    	
    	// a "depth-first search" method to check all the nodes
    	// (Note the tree was generated using a "breadth-first search")
    	Deque<Pair<Integer, TreeNode> > stack = new  ArrayDeque<Pair<Integer, TreeNode> > () ;
    	stack.add(Pair.newPair(0, root)) ;
    	while (!stack.isEmpty())
    	{
    		Pair<Integer, TreeNode>  currPairLevelTreeNode = stack.poll() ;
    		
    		TreeNode currTreeNode = currPairLevelTreeNode.getO() ;
    		int level = currPairLevelTreeNode.getL() + 1 ;
    		
    		// check the number of children
    		assertTrue (treeModel.getChildCount(currTreeNode) == numberOfChildren) ;
    		
    		// check that no node, apart from the root, is marked as root.
    		if (level > 1)
    			assertFalse (currTreeNode.isRoot()) ;
    		
    		String previousChildName = null ;
    		for (int i = 0 ; i < numberOfChildren ; i++)
    		{
    			TreeNode currChildTreeNode = (TreeNode) treeModel.getChild(currTreeNode, i) ;
    			String currChildName = currChildTreeNode.getObjectName() ;
    			
    			// check the base name
    			String baseName = buildBaseNodeName ((currTreeNode.isRoot())?(null):(currTreeNode.getObjectName()), level) ;
    			assertTrue (currChildName.startsWith(baseName)) ;
    			
    			// The name must be lexicographically ordered in the tree
    			// Note that in the init method, the list is shuffled, just to be sure...
    			if (previousChildName != null)
    				assertTrue (previousChildName.compareTo(currChildName) < 0) ;
    			previousChildName = currChildName ;
    			
	    		if (level <  numberOfLevel)
	    			stack.add(Pair.newPair(level, currChildTreeNode)) ;
	    		else
	    			// here, it must be a leaf
	    			assertTrue (treeModel.isLeaf(currChildTreeNode)) ;
    		}
    	}
    }
	
    
    @Test
    public void shouldCreateWellStructuredTreeModel() {
    	
    	StoredObjectsTreeModel treeModel = new StoredObjectsTreeModel (rootContainer, storedObjectList) ;
    	verifyTreeStructure (treeModel) ;
    }
    
    
    @Test
    public void shouldCreateAndAddWellStructuredTreeModel() {
    	
    	int size = storedObjectList.size() ;
    	
    	// let's make sure that the test has some significance
    	assertTrue (size > 10) ;
    	
    	int first = size / 3 ;
    	int second = 2 * first ;
    	int third = size ;
    	
    	StoredObjectsTreeModel treeModel = new StoredObjectsTreeModel (rootContainer, storedObjectList.subList(0, first)) ;
    	treeModel.addAll(storedObjectList.subList(first, second));
    	treeModel.addAll(storedObjectList.subList(second, third));
    	
    	verifyTreeStructure (treeModel) ;
    }
    
    
    @Test
    public void shouldCreateAndOverAddWellStructuredTreeModel() {
    	
    	int size = storedObjectList.size() ;
    	
    	// let's make sure that the test has some significance
    	assertTrue (size > 10) ;
    	
    	int first = size / 3 ;
    	int second = 2 * first ;
    	int third = size ;
    	
    	StoredObjectsTreeModel treeModel = new StoredObjectsTreeModel (rootContainer, storedObjectList.subList(0, first)) ;
    	treeModel.addAll(storedObjectList.subList(second, third));
    	treeModel.addAll(storedObjectList.subList(second, third));
    	
    	// Add again
    	treeModel.addAll(storedObjectList.subList(second, third));
    	
    	// and again
    	treeModel.addAll(storedObjectList.subList(first, third));
    	
    	// Still the tree should look good
    	verifyTreeStructure (treeModel) ;
    }
}
