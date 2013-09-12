package com.dotcms.enterprise.publishing.remote.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.HTMLPageWrapper;
import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.htmlpages.model.HTMLPageVersionInfo;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HTMLPageHandlerTest {
    
    @Test
    public void basicPageHandling() throws Exception {
        User user=APILocator.getUserAPI().getSystemUser();
        
        String salt=UUIDGenerator.generateUuid();
        String htmlpageName="test"+salt;
        String htmlpageURL="test"+salt+".html";
        
        
        Template template=new Template();
        template.setBody("this is the body of the template");
        template.setFriendlyName("template"+salt);
        template.setTitle("template"+salt);        
        Host host=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);        
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        
        Folder folder=APILocator.getFolderAPI().createFolders("/btest", host, user, false);
        String folderUri=APILocator.getIdentifierAPI().find(folder).getURI();
        
        Identifier ident=new Identifier();
        ident.setAssetName(htmlpageName);
        ident.setParentPath(folderUri+"/");
        ident.setId(UUIDGenerator.generateUuid());
        ident.setHostId(host.getIdentifier());
        ident.setAssetType("htmlpage");
        
        HTMLPage page=new HTMLPage();
        page.setFriendlyName(htmlpageName);
        page.setTitle(htmlpageName);
        page.setPageUrl(htmlpageURL);
        page.setTemplateId(template.getIdentifier());
        page.setShowOnMenu(true);
        page.setInode(UUIDGenerator.generateUuid());
        page.setIdentifier(ident.getId());
        page.setModDate(Calendar.getInstance().getTime());
        page.setModUser(user.getUserId());
        
        HTMLPageVersionInfo vi=new HTMLPageVersionInfo();
        vi.setIdentifier(ident.getId());
        vi.setWorkingInode(page.getInode());
        vi.setLiveInode(page.getInode());
        vi.setDeleted(false);
        vi.setVersionTs(Calendar.getInstance().getTime());
        
        HTMLPageWrapper wrapper = new HTMLPageWrapper(page, ident);
        wrapper.setOperation(Operation.PUBLISH);
        wrapper.setVi(vi);
        
        File bundleRoot=BundlerUtil.getBundleRoot(salt);
        bundleRoot.mkdirs();
        File wrapperFile = new File(bundleRoot, htmlpageName+".html.xml");
        FileOutputStream out=new FileOutputStream(wrapperFile);
        new XStream(new DomDriver()).toXML(wrapper, out);
        out.close();
        
        PushPublisherConfig config=new PushPublisherConfig();
        config.setId(salt);
        
        HTMLPageHandler handler=new HTMLPageHandler(config);
        
        handler.handle(bundleRoot);
        
        HTMLPage newp=APILocator.getHTMLPageAPI().loadLivePageById(ident.getId(), user, false);
        Assert.assertNotNull(newp);
        Assert.assertEquals(page.getInode(), newp.getInode());
        Assert.assertEquals(ident.getId(), page.getIdentifier());
        Assert.assertEquals(template.getIdentifier(), newp.getTemplateId());
        
        Identifier newi=APILocator.getIdentifierAPI().find(newp);
        Assert.assertEquals(host.getIdentifier(), newi.getHostId());
        Assert.assertEquals(folderUri+"/", newi.getParentPath());
        Assert.assertEquals(htmlpageName+".html", newi.getAssetName());
    }
}
