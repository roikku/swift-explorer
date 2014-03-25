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

package org.swiftexplorer.gui.preview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swiftexplorer.gui.preview.PreviewPanel.PreviewComponent;

import javax.swing.JPanel;

public class PdfPanel  extends JPanel implements PreviewComponent{

	private static final long serialVersionUID = 1L;
	
	private final Logger logger = LoggerFactory.getLogger(PdfPanel.class);
	
	private final int maxPageToPreview = 10 ;
	
	private final List<BufferedImage> listImagePages = new ArrayList<BufferedImage> ();
	
	public PdfPanel ()
	{
		super  () ;
	}
	
	@Override
	public boolean supports(String type) {
		return "application/pdf".equals(type) ; 
	}

	
	@Override
	public void displayPreview(String contentType, ByteArrayInputStream in) 
	{	
		try 
		{
			setPdf (PDDocument.load(in)) ;
		} 
		catch (IOException e) 
		{
			clearPdf() ;
			logger.error("Error occurred while opening the pdf document", e);
		}
	}
	
	
    public synchronized void setPdf(PDDocument pdf) 
    {
    	listImagePages.clear();
        if (pdf == null)
        	return ;
		try 
		{
			if (pdf.isEncrypted())
			{
				logger.info("Failed attempt at previewing an encrypted PDF");
				return ;
			}
			PDDocumentCatalog cat = pdf.getDocumentCatalog() ;
			@SuppressWarnings("unchecked")
			List<PDPage> pages = cat.getAllPages() ;
	        if (pages != null && !pages.isEmpty()) 
	        {
	        	for (PDPage page : pages)
	        	{
	        		listImagePages.add(page.convertToImage()) ;
	        		if (listImagePages.size() >= maxPageToPreview)
	        			break ;
	        	}
	        } 
		} 
		catch (IOException e) 
		{
			logger.error("Error occurred while opening the pdf document", e);
		}
		finally 
		{
			if (pdf != null)
			{
				try 
				{
					pdf.close();
				} 
				catch (IOException ex) 
				{
					logger.error("Error occurred while closing the pdf document", ex);
				}
			}
		}
        repaint();
    }

    
    public synchronized void clearPdf() {
    	listImagePages.clear();
        repaint();
    }
    
    
    private BufferedImage getBestFit (BufferedImage bi, int maxWidth, int maxHeight)
    {
		if (bi == null)
			return null ;
		
    	Mode mode = Mode.AUTOMATIC ;
    	int maxSize = Math.min(maxWidth, maxHeight) ;
    	double dh = (double)bi.getHeight() ;
    	if (dh > Double.MIN_VALUE)
    	{
    		double imageAspectRatio = (double)bi.getWidth() / dh ;
        	if (maxHeight * imageAspectRatio <=  maxWidth)
        	{
        		maxSize = maxHeight ;
        		mode = Mode.FIT_TO_HEIGHT ;
        	}
        	else
        	{
        		maxSize = maxWidth ;
        		mode = Mode.FIT_TO_WIDTH ;
        	}	
    	}
    	return Scalr.resize(bi, Method.QUALITY, mode, maxSize, Scalr.OP_ANTIALIAS) ; 
    }
    
    
    /**
     * {@inheritDoc}.
     */
    @Override
    protected synchronized void paintComponent(Graphics g) 
    {
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (!listImagePages.isEmpty()) 
        {
        	int W = this.getWidth() ;
        	int H = this.getHeight() ;
        	int nImg = listImagePages.size() ;
        	int maxImgPerRow = 5 ;
        	int margin = 10 ;
        	int space = 5 ;
        	int nRows = nImg / maxImgPerRow + ((nImg%maxImgPerRow == 0)?(0):(1)) ;

    		int iW = (W - 2 * margin - (Math.min(maxImgPerRow, nImg) - 1) * space) / Math.min(maxImgPerRow, nImg)  ;
    		int iH = (H - 2 * margin - (nRows - 1) * space) / nRows  ;
        	
    		int iX = margin ;
    		int iY = margin ;
    		int count = 0 ;
    		for (BufferedImage bi : listImagePages)
        	{ 
    			if (bi == null)
    				continue ;
    			
            	BufferedImage scaledImg = getBestFit (bi, iW, iH) ; 
    			g.drawImage(scaledImg, iX, iY, scaledImg.getWidth(), scaledImg.getHeight(), this);
        		
        		++count ;
        		iX += (scaledImg.getWidth() + space) ;
        		if (count%maxImgPerRow == 0)
        		{
        			iX = margin ;
        			iY += (scaledImg.getHeight() + space) ;
        		}
        	}
        } 
    }
}
