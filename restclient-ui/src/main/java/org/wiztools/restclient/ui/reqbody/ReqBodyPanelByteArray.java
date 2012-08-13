package org.wiztools.restclient.ui.reqbody;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.wiztools.commons.FileUtil;
import org.wiztools.restclient.XMLException;
import org.wiztools.restclient.bean.ReqEntity;
import org.wiztools.restclient.bean.ReqEntityByteArray;
import org.wiztools.restclient.bean.ReqEntityByteArrayBean;
import org.wiztools.restclient.ui.FileChooserType;
import org.wiztools.restclient.ui.RCFileView;
import org.wiztools.restclient.ui.RESTUserInterface;
import org.wiztools.restclient.ui.UIUtil;
import org.wiztools.restclient.util.HexDump;
import org.wiztools.restclient.util.Util;
import org.wiztools.restclient.util.XMLUtil;

/**
 *
 * @author subwiz
 */
public class ReqBodyPanelByteArray extends JPanel implements ReqBodyPanel {
    @Inject private RESTUserInterface ui;
    @Inject private ContentTypeCharsetComponent jp_content_type_charset;
    
    private JButton jb_body = new JButton(UIUtil.getIconFromClasspath(RCFileView.iconBasePath + "load_from_file.png"));
    
    private JTextArea jta = new JTextArea();
    
    private byte[] body;
    
    @PostConstruct
    protected void init() {
        setLayout(new BorderLayout());
        
        JPanel jp_north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        jp_north.add(jp_content_type_charset);
        
        jb_body.setToolTipText("Select file having body content");
        jb_body.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                fileOpen();
            }
        });
        jp_north.add(jb_body);
        
        add(jp_north, BorderLayout.NORTH);
        
        jta.setEditable(false);
        
        add(jta, BorderLayout.CENTER);
    }
    
    private void fileOpen() {
        File f = ui.getOpenFile(FileChooserType.OPEN_REQUEST_BODY);
        if(f == null){ // Pressed cancel?
            return;
        }
        if(!f.canRead()){
            JOptionPane.showMessageDialog(ui.getFrame(),
                    "File not readable: " + f.getAbsolutePath(),
                    "IO Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        final String mime = FileUtil.getMimeType(f);
        if(!mime.equals("content/unknown")) {
            final String origContentType = jp_content_type_charset.getContentType().getContentType();
            if(!mime.equals(origContentType)) {
                final int result = JOptionPane.showConfirmDialog(ui.getFrame(),
                        "The content-type selected (" + origContentType + ") does NOT match\n"
                        + "the computed file mime type (" + mime + ")\n"
                        + "Do you want to update the content-type to `" + mime + "'?",
                        "Mime-type mismatch correction",
                        JOptionPane.YES_NO_OPTION);
                if(result == JOptionPane.YES_OPTION) {
                    // Set content type
                    jp_content_type_charset.setContentType(mime);
                    
                    // Check if XML content type:
                    if(XMLUtil.XML_MIME.equals(mime)){
                        try{
                            String charset = XMLUtil.getDocumentCharset(f);
                            if(charset != null && !(charset.equals(jp_content_type_charset.getCharsetString()))) {
                                final int charsetYesNo = JOptionPane.showConfirmDialog(ui.getFrame(),
                                        "Change charset to `" + charset + "'?",
                                        "Change charset?",
                                        JOptionPane.YES_NO_OPTION);
                                if(charsetYesNo == JOptionPane.YES_OPTION) {
                                    jp_content_type_charset.setCharset(Charset.forName(charset));
                                }
                            }
                        }
                        catch(IOException ex){
                            // Do nothing!
                        }
                        catch(XMLException ex){
                            // Do nothing!
                        }
                    }
                }
            }
        }
        
        final long fileSizeMB = f.length() / (1024l*1024l);
        if(fileSizeMB > 2) {
            final int yesNoOption = JOptionPane.showConfirmDialog(ui.getFrame(),
                    "File size is more than 2 MB.\nDo you want to continue loading (may take some time!)?",
                    "File exceeds threshold size",
                    JOptionPane.YES_NO_OPTION);
            if(yesNoOption == JOptionPane.NO_OPTION) {
                return;
            }
        }
        
        try {
            byte[] data = FileUtil.getContentAsBytes(f);
            body = data;
            jta.setText(HexDump.getHexDataDumpAsString(data));
            jta.setCaretPosition(0);
        }
        catch(IOException ex) {
            ui.getView().doError(Util.getStackTrace(ex));
        }
    }

    @Override
    public void enableBody() {
        jp_content_type_charset.enableComponent();
        jb_body.setEnabled(true);
        jta.setEnabled(true);
    }

    @Override
    public void disableBody() {
        jp_content_type_charset.disableComponent();
        jb_body.setEnabled(false);
        jta.setEnabled(false);
    }

    @Override
    public void setEntity(ReqEntity entity) {
        if(entity instanceof ReqEntityByteArray) {
            ReqEntityByteArray e = (ReqEntityByteArray) entity;
            
            // content-type charset
            jp_content_type_charset.setContentTypeCharset(e.getContentType());
            
            // Set body:
            jta.setText(HexDump.getHexDataDumpAsString(e.getBody()));
        }
    }

    @Override
    public ReqEntity getEntity() {
        ReqEntityByteArrayBean out = new ReqEntityByteArrayBean(body,
                jp_content_type_charset.getContentType());
        return out;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void clear() {
        jp_content_type_charset.clearComponent();
        jta.setText("");
    }
    
}
