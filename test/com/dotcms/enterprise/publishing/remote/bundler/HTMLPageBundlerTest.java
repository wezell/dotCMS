package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.HTMLPageWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class HTMLPageBundlerTest {
    
    @Test
    public void htmlpageBundlerBasicTest() throws Exception {
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
        
        HTMLPage page=new HTMLPage();
        page.setFriendlyName(htmlpageName);
        page.setTitle(htmlpageName);
        page.setPageUrl(htmlpageURL);
        page.setTemplateId(template.getIdentifier());
        page.setShowOnMenu(true);
        page = APILocator.getHTMLPageAPI().saveHTMLPage(page, template, folder, user, false);
        
        PushPublisherConfig pc=new PushPublisherConfig();
        pc.setId(UUIDGenerator.generateUuid());
        pc.setOperation(Operation.PUBLISH);
        Set<String> pages=new HashSet<String>();
        pages.add(page.getIdentifier());
        pc.setHTMLPages(pages);
        
        File bundleRoot=BundlerUtil.getBundleRoot("htmltest"+System.currentTimeMillis());
        
        HTMLPageBundler bundler=new HTMLPageBundler();
        bundler.setConfig(pc);
        bundler.generate(bundleRoot, new BundlerStatus(HTMLPage.class.getCanonicalName()));
        
        File wrapperFile=new File(bundleRoot,"working"+File.separator+"demo.dotcms.com"+File.separator
                +APILocator.getIdentifierAPI().find(page).getURI().replaceAll("/", File.separator)+".html.xml");
        HTMLPageWrapper wrapper = (HTMLPageWrapper) new XStream(new DomDriver()).fromXML(wrapperFile);
        
        Assert.assertEquals(Operation.PUBLISH, wrapper.getOperation());
        Assert.assertEquals(htmlpageName, wrapper.getPage().getTitle());
        Assert.assertEquals(htmlpageName, wrapper.getPage().getFriendlyName());
        Assert.assertEquals(page.getInode(), wrapper.getPage().getInode());
        Assert.assertEquals(page.getIdentifier(), wrapper.getPage().getIdentifier());
        Assert.assertEquals(page.getIdentifier(), wrapper.getVi().getIdentifier());
        Assert.assertEquals(page.getInode(), wrapper.getVi().getWorkingInode());
        Assert.assertFalse(InodeUtils.isSet(wrapper.getVi().getLiveInode()));
        Assert.assertFalse(wrapper.getVi().isDeleted());
        Assert.assertEquals(APILocator.getIdentifierAPI().find(folder).getURI()+"/", wrapper.getPageId().getParentPath());
        Assert.assertEquals(host.getIdentifier(), wrapper.getPageId().getHostId());
        
    }
}
