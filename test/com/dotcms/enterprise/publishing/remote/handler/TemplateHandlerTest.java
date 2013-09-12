package com.dotcms.enterprise.publishing.remote.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.TemplateWrapper;
import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.templates.model.TemplateVersionInfo;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class TemplateHandlerTest {
    
    @Test
    public void basicTemplateHandlingTest() throws Exception {
        User user=APILocator.getUserAPI().getSystemUser();
        String salt=UUIDGenerator.generateUuid();
        String templateName="pushedTemplate"+salt;
        
        Host host=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        
        String identId=UUIDGenerator.generateUuid();
        String inode=UUIDGenerator.generateUuid();
        
        Identifier ident=new Identifier();
        ident.setAssetName(templateName);
        ident.setAssetType("template");
        ident.setHostId(host.getIdentifier());
        ident.setId(identId);
        ident.setParentPath("/");
        
        Template template=new Template();
        template.setBody("<html><body>Test template</body></html>");
        template.setIdentifier(ident.getId());
        template.setInode(inode);
        template.setTitle(templateName);
        
        TemplateVersionInfo vi=new TemplateVersionInfo();
        vi.setIdentifier(identId);
        vi.setWorkingInode(inode);
        vi.setLiveInode(inode);
        vi.setVersionTs(Calendar.getInstance().getTime());
        
        TemplateWrapper wrapper=new TemplateWrapper(ident, template);
        wrapper.setOperation(Operation.PUBLISH);
        wrapper.setVi(vi);
        
        File bundleRoot=BundlerUtil.getBundleRoot(salt);
        bundleRoot.mkdirs();
        
        File wrapperFile=new File(bundleRoot,templateName+".template.xml");
        FileOutputStream out=new FileOutputStream(wrapperFile);
        new XStream(new DomDriver()).toXML(wrapper, out);
        out.close();
        
        PushPublisherConfig conf=new PushPublisherConfig();
        conf.setId(salt);
        
        TemplateHandler handler=new TemplateHandler(conf);
        handler.handle(bundleRoot);
        
        Template newt=APILocator.getTemplateAPI().findLiveTemplate(identId, user, false);
        Assert.assertNotNull(newt);
        Assert.assertEquals(identId, newt.getIdentifier());
        Assert.assertEquals(inode, newt.getInode());
        Assert.assertEquals("<html><body>Test template</body></html>", newt.getBody());
        Assert.assertEquals(templateName, newt.getTitle());
        Assert.assertTrue(newt.isLive() && newt.isWorking() && !newt.isDeleted());
        
    }
}
