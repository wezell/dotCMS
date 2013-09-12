package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.TemplateWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class TemplateBundlerTest {
    @Test
    public void testTemplateBundler() throws Exception {
        User user=APILocator.getUserAPI().getSystemUser();
        
        String templateName="testbundler"+UUIDGenerator.generateUuid();
        Template template=new Template();
        template.setBody("this is the body of the template");
        template.setFriendlyName(templateName);
        template.setTitle(templateName);
        
        Host host=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        
        template=APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        
        PushPublisherConfig pc=new PushPublisherConfig();
        pc.setId(UUIDGenerator.generateUuid());
        pc.setOperation(Operation.PUBLISH);
        Set<String> templates=new HashSet<String>();
        templates.add(template.getIdentifier());
        pc.setTemplates(templates);
        
        File bundleRoot=BundlerUtil.getBundleRoot("templatetest"+System.currentTimeMillis());
        
        TemplateBundler bundler=new TemplateBundler();
        bundler.setConfig(pc);
        bundler.generate(bundleRoot, new BundlerStatus(TemplateBundler.class.getCanonicalName()));
        
        File wrapperFile=new File(bundleRoot,"working"+File.separator+"demo.dotcms.com"+File.separator
                +APILocator.getIdentifierAPI().find(template).getURI().replaceAll("/", File.separator)+".template.xml");
        TemplateWrapper wrapper = (TemplateWrapper) new XStream(new DomDriver()).fromXML(wrapperFile);
        
        Assert.assertEquals(Operation.PUBLISH, wrapper.getOperation());
        Assert.assertEquals(template.getIdentifier(),wrapper.getVi().getIdentifier());
        Assert.assertEquals(template.getInode(), wrapper.getVi().getWorkingInode());
        Assert.assertNull(wrapper.getVi().getLiveInode());
        Assert.assertEquals("this is the body of the template", wrapper.getTemplate().getBody());
        Assert.assertEquals(templateName, wrapper.getTemplate().getTitle());
        Assert.assertEquals(template.getInode(),wrapper.getTemplate().getInode());
        Assert.assertEquals(template.getIdentifier(), wrapper.getTemplate().getIdentifier());
    }
}
