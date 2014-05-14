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

package org.swiftexplorer.gui.log;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class LogPanel extends JPanel {

	public static enum LogType
	{
		ERROR,
		WARNING,
		INFO,
	}
	
	public static class LogItem
	{
		private final Date date ;
		private final String message ;
		private final LogType type ;
		
		public LogItem(Date date, String message, LogType type) {
			super();
			this.date = date;
			this.message = message;
			this.type = type;
		}

		public Date getDate() {
			return date;
		}

		public String getMessage() {
			return message;
		}

		public LogType getType() {
			return type;
		}
		
		public Icon getIcon ()
		{
			return new ImageIcon(LogPanel.class.getResource("/icons/" + type.toString().toLowerCase() + ".png")) ;
		}
		
		public String toString ()
		{
			StringBuilder sb = new StringBuilder () ;
			sb.append(date.toString()) ;
			sb.append ("  -  ") ;
			sb.append (message) ;
			return sb.toString () ;
		}
	}
	
	private static final long serialVersionUID = 1L;
	
    private final DefaultListModel<LogItem> logsObjects = new DefaultListModel<LogItem>();
    private final JList<LogItem> logsList = new JList<LogItem>(logsObjects);
	
    
	public LogPanel ()
	{
		super (new BorderLayout(0, 0)) ;
		setListBehavior (logsList) ;
		add (new JScrollPane (logsList)) ;
	}
	
	
	public synchronized void clear ()
	{
		logsObjects.clear();
	}

	
	public synchronized void add (String str, LogType type)
	{
		logsObjects.addElement(new LogItem (new Date (), str, type)) ;
		
		int index = logsObjects.getSize() - 1 ;
		logsList.setSelectedIndex(index);
		logsList.ensureIndexIsVisible(index);
	}
	
	
	private final void setListBehavior (JList<LogItem> list)
	{
		final Border LR_PADDING = BorderFactory.createEmptyBorder(0, 2, 0, 2);
		 
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) 
			{
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBorder(LR_PADDING);
                LogItem li = (LogItem) value;
                lbl.setText("<html>" + li.toString() + "</html>");
                lbl.setToolTipText(lbl.getText().replaceAll("\\<[^>]*>",""));
                lbl.setIcon(li.getIcon()) ;
                return lbl;
            }
        });
		list.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {            	
            }
        });
	}
}
