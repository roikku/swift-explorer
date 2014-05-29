/*
 * Copyright 2010 Loic Merckel
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

/* This file incorporates work covered by the following copyright and  
 * permission notice:  
 *  
 * Copyright 2009 IT Mill Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

package org.swiftexplorer.gui.util;

import org.swiftexplorer.swift.util.SwiftUtils;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.commons.io.FilenameUtils;

//http://docs.oracle.com/cd/E19146-01/819-2630/abumi/index.html
public class FileTypeIconFactory {
	
	private FileTypeIconFactory () { super () ; } ;
	
	private static final String iconBaseName = "file_extension_" ;
	private static final String iconResourcePath = "/icons/files/" ;
	private static final String iconResourceExt = ".png" ;
	

	// This block is Copyright 2009 IT Mill Ltd (see header).
	// Source: http://dev.vaadin.com/svn/branches/embedding/src/com/vaadin/service/FileTypeResolver.java
    /**
     * Initial file extension to mime-type mapping.
     */
    static private String initialExtToMIMEMap = "application/cu-seeme                            csm cu,"
            + "application/dsptype                             tsp,"
            + "application/futuresplash                        spl,"
            + "application/mac-binhex40                        hqx,"
            + "application/msaccess                            mdb,"
            + "application/msword                              doc dot,"
            + "application/octet-stream                        bin,"
            + "application/oda                                 oda,"
            + "application/pdf                                 pdf,"
            + "application/pgp-signature                       pgp,"
            + "application/postscript                          ps ai eps,"
            + "application/rtf                                 rtf,"
            + "application/vnd.ms-excel                        xls xlb,"
            + "application/vnd.ms-powerpoint                   ppt pps pot,"
            + "application/vnd.wap.wmlc                        wmlc,"
            + "application/vnd.wap.wmlscriptc                  wmlsc,"
            + "application/wordperfect5.1                      wp5,"
            + "application/zip                                 zip,"
            + "application/x-123                               wk,"
            + "application/x-bcpio                             bcpio,"
            + "application/x-chess-pgn                         pgn,"
            + "application/x-cpio                              cpio,"
            + "application/x-debian-package                    deb,"
            + "application/x-director                          dcr dir dxr,"
            + "application/x-dms                               dms,"
            + "application/x-dvi                               dvi,"
            + "application/x-xfig                              fig,"
            + "application/x-font                              pfa pfb gsf pcf pcf.Z,"
            + "application/x-gnumeric                          gnumeric,"
            + "application/x-gtar                              gtar tgz taz,"
            + "application/x-hdf                               hdf,"
            + "application/x-httpd-php                         phtml pht php,"
            + "application/x-httpd-php3                        php3,"
            + "application/x-httpd-php3-source                 phps,"
            + "application/x-httpd-php3-preprocessed           php3p,"
            + "application/x-httpd-php4                        php4,"
            + "application/x-ica                               ica,"
            + "application/x-java-archive                      jar,"
            + "application/x-java-serialized-object            ser,"
            + "application/x-java-vm                           class,"
            + "application/x-javascript                        js,"
            + "application/x-kchart                            chrt,"
            + "application/x-killustrator                      kil,"
            + "application/x-kpresenter                        kpr kpt,"
            + "application/x-kspread                           ksp,"
            + "application/x-kword                             kwd kwt,"
            + "application/x-latex                             latex,"
            + "application/x-lha                               lha,"
            + "application/x-lzh                               lzh,"
            + "application/x-lzx                               lzx,"
            + "application/x-maker                             frm maker frame fm fb book fbdoc,"
            + "application/x-mif                               mif,"
            + "application/x-msdos-program                     com exe bat dll,"
            + "application/x-msi                               msi,"
            + "application/x-netcdf                            nc cdf,"
            + "application/x-ns-proxy-autoconfig               pac,"
            + "application/x-object                            o,"
            + "application/x-ogg                               ogg,"
            + "application/x-oz-application                    oza,"
            + "application/x-perl                              pl pm,"
            + "application/x-pkcs7-crl                         crl,"
            + "application/x-redhat-package-manager            rpm,"
            + "application/x-shar                              shar,"
            + "application/x-shockwave-flash                   swf swfl,"
            + "application/x-star-office                       sdd sda,"
            + "application/x-stuffit                           sit,"
            + "application/x-sv4cpio                           sv4cpio,"
            + "application/x-sv4crc                            sv4crc,"
            + "application/x-tar                               tar,"
            + "application/x-tex-gf                            gf,"
            + "application/x-tex-pk                            pk PK,"
            + "application/x-texinfo                           texinfo texi,"
            + "application/x-trash                             ~ % bak old sik,"
            + "application/x-troff                             t tr roff,"
            + "application/x-troff-man                         man,"
            + "application/x-troff-me                          me,"
            + "application/x-troff-ms                          ms,"
            + "application/x-ustar                             ustar,"
            + "application/x-wais-source                       src,"
            + "application/x-wingz                             wz,"
            + "application/x-x509-ca-cert                      crt,"
            + "audio/basic                                     au snd,"
            + "audio/midi                                      mid midi,"
            + "audio/mpeg                                      mpga mpega mp2 mp3,"
            + "audio/mpegurl                                   m3u,"
            + "audio/prs.sid                                   sid,"
            + "audio/x-aiff                                    aif aiff aifc,"
            + "audio/x-gsm                                     gsm,"
            + "audio/x-pn-realaudio                            ra rm ram,"
            + "audio/x-scpls                                   pls,"
            + "audio/x-wav                                     wav,"
            + "image/bitmap                                    bmp,"
            + "image/gif                                       gif,"
            + "image/ief                                       ief,"
            + "image/jpeg                                      jpeg jpg jpe,"
            + "image/pcx                                       pcx,"
            + "image/png                                       png,"
            + "image/svg+xml                                   svg svgz,"
            + "image/tiff                                      tiff tif,"
            + "image/vnd.wap.wbmp                              wbmp,"
            + "image/x-cmu-raster                              ras,"
            + "image/x-coreldraw                               cdr,"
            + "image/x-coreldrawpattern                        pat,"
            + "image/x-coreldrawtemplate                       cdt,"
            + "image/x-corelphotopaint                         cpt,"
            + "image/x-jng                                     jng,"
            + "image/x-portable-anymap                         pnm,"
            + "image/x-portable-bitmap                         pbm,"
            + "image/x-portable-graymap                        pgm,"
            + "image/x-portable-pixmap                         ppm,"
            + "image/x-rgb                                     rgb,"
            + "image/x-xbitmap                                 xbm,"
            + "image/x-xpixmap                                 xpm,"
            + "image/x-xwindowdump                             xwd,"
            + "text/comma-separated-values                     csv,"
            + "text/css                                        css,"
            + "text/html                                       htm html xhtml,"
            + "text/mathml                                     mml,"
            + "text/plain                                      txt text diff,"
            + "text/richtext                                   rtx,"
            + "text/tab-separated-values                       tsv,"
            + "text/vnd.wap.wml                                wml,"
            + "text/vnd.wap.wmlscript                          wmls,"
            + "text/xml                                        xml,"
            + "text/x-c++hdr                                   h++ hpp hxx hh,"
            + "text/x-c++src                                   c++ cpp cxx cc,"
            + "text/x-chdr                                     h,"
            + "text/x-csh                                      csh,"
            + "text/x-csrc                                     c,"
            + "text/x-java                                     java,"
            + "text/x-moc                                      moc,"
            + "text/x-pascal                                   p pas,"
            + "text/x-setext                                   etx,"
            + "text/x-sh                                       sh,"
            + "text/x-tcl                                      tcl tk,"
            + "text/x-tex                                      tex ltx sty cls,"
            + "text/x-vcalendar                                vcs,"
            + "text/x-vcard                                    vcf,"
            + "video/dl                                        dl,"
            + "video/fli                                       fli,"
            + "video/gl                                        gl,"
            + "video/mpeg                                      mpeg mpg mpe,"
            + "video/quicktime                                 qt mov,"
            + "video/x-mng                                     mng,"
            + "video/x-ms-asf                                  asf asx,"
            + "video/x-msvideo                                 avi,"
            + "video/x-sgi-movie                               movie,"
            + "x-world/x-vrml                                  vrm vrml wrl";
	
    
    static final private Map<String, String> extToMineMap = new ConcurrentHashMap<String, String>();
    static final private Map<String, Set<String> > mineToExtSetMap = new ConcurrentHashMap<String, Set<String> >();
    static final private Map<String, Icon> extToIconMap = new ConcurrentHashMap<String, Icon>();

    static {
        final StringTokenizer lines = new StringTokenizer(initialExtToMIMEMap, ",");
        while (lines.hasMoreTokens()) 
        {
        	Set<String> extsSet = new HashSet<String> () ;
        	
            final String line = lines.nextToken();
            final StringTokenizer exts = new StringTokenizer(line);
            final String type = exts.nextToken();
            while (exts.hasMoreTokens()) 
            {
                final String ext = exts.nextToken();
                extToMineMap.put(ext, type);
                extsSet.add(ext) ;
            }
            mineToExtSetMap.put(type, extsSet) ;
        }
    } 
    
	public static Icon getFileTypeIcon (String contentType, String filename)
	{
		Icon ret = null ;
		
    	if (contentType != null && SwiftUtils.directoryContentType.equalsIgnoreCase(contentType))
    		return getIconFromIconFileName("folder.png");
		
		String ext = FilenameUtils.getExtension(filename);
		if (ext != null && !ext.isEmpty())
			ret = getIconFromFileExt (ext) ;
		if (ret != null)
			return ret ;
		
        if (contentType != null && !contentType.isEmpty())
        {
        	/*if (SwiftUtils.directoryContentType.equalsIgnoreCase(contentType))
        		ret = getIconFromIconFileName("folder.png");
        	else*/ if (mineToExtSetMap.containsKey(contentType))
        	{
        		Set<String> extsSet = mineToExtSetMap.get(contentType);
        		for (String ex : extsSet)
        		{
        			ret = getIconFromFileExt(ex) ;
        			if (ret != null)
        				break ;
        		}
        	}
        }
    	if (ret == null)
    		ret = getIconFromIconFileName("page_white.png");
        return ret ;
	}
	
	private static String getIconFileNameFromExt (String ext)
	{
        StringBuilder iconName = new StringBuilder () ;
        iconName.append(iconBaseName) ;
        iconName.append(ext) ;
        iconName.append(iconResourceExt) ;
        return iconName.toString() ;
	}
	
    private static Icon getIconFromFileExt(String ext) {
        
    	if (extToIconMap.containsKey(ext))
        	return extToIconMap.get(ext) ;
   
        Icon icon = getIconFromIconFileName (getIconFileNameFromExt (ext)) ;
        if (icon != null)
        	extToIconMap.put(ext, icon) ;
        return icon ;
    }
    
    private static Icon getIconFromIconFileName(String string) {
    	URL path = FileTypeIconFactory.class.getResource(iconResourcePath + string) ;
        return (path == null) ? (null) : (new ImageIcon(path));
    }

}
