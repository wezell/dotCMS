package com.dotcms.enterprise.publishing.remote.bundler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.dotcms.publisher.pusher.PushPublisherConfig;
import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.ContainerWrapper;
import com.dotcms.publishing.BundlerStatus;
import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ContainerBundlerTest {

    
    @Test
    public void containerBundle() throws Exception {
        Structure wpc = StructureCache.getStructureByVelocityVarName("webPageContent");
        User user = APILocator.getUserAPI().getSystemUser();
        Host host = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        
        // create some contentlets
        final int len=20;
        Contentlet[] contentlets=new Contentlet[len];
        for(int i=0;i<len;i++) {
            Contentlet cont1 = new Contentlet();
            cont1.setStringProperty("title", "content"+(i+1));
            cont1.setStringProperty("body", "body"+(i+1));
            cont1.setHost(host.getIdentifier());
            cont1.setFolder("SYSTEM_FOLDER");
            cont1.setStructureInode(wpc.getInode());
            cont1.setLanguageId(1);
            cont1 = APILocator.getContentletAPI().checkin(cont1, user, false);
            APILocator.getVersionableAPI().setLive(cont1);
            contentlets[i]=cont1;
        }
        for(int i=0;i<len;i++) {
            APILocator.getContentletAPI().isInodeIndexed(contentlets[i].getInode(), true);
        }
        
        Container container = new Container();
        container.setCode("this is the code");
        String title="title container "+UUIDGenerator.generateUuid();
        container.setFriendlyName(title);
        container.setTitle(title);
        container.setMaxContentlets(100);
        container.setPreLoop("preloop");
        container.setPostLoop("postloop");
        
        List<ContainerStructure> csl = new ArrayList<ContainerStructure>();
        container = APILocator.getContainerAPI().save(container, csl, host, user, false);
        APILocator.getVersionableAPI().setLive(container);
        
        csl = new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setStructureId(wpc.getInode());
        cs.setContainerId(container.getIdentifier());
        cs.setCode("codecode");
        csl.add(cs);
        APILocator.getContainerAPI().saveContainerStructures(csl);
        
        // a test page just to have something in multitree
        Template template = new Template();
        template.setBody("<html> <body> #parseContainer('"+container.getIdentifier()+"') </body> </html>");
        template.setTitle("test_template_"+UUIDGenerator.generateUuid());
        template = APILocator.getTemplateAPI().saveTemplate(template, host, user, false);
        APILocator.getVersionableAPI().setLive(template);
        
        HTMLPage page = new HTMLPage();
        page.setPageUrl("test"+UUIDGenerator.generateUuid()+".html");
        page.setTitle("test page");
        page.setTemplateId(template.getIdentifier());
        Folder parentFolder=APILocator.getFolderAPI().createFolders("/cbt", host, user, false);
        page = APILocator.getHTMLPageAPI().saveHTMLPage(page, template, parentFolder, user, false);
        APILocator.getVersionableAPI().setLive(page);
        
        // insert all the contentlets in the page
        Set<String> childs=new HashSet<String>();
        for(int i=0;i<len;i++) {
            MultiTree mt=new MultiTree();
            mt.setParent1(page.getIdentifier());
            mt.setParent2(container.getIdentifier());
            mt.setChild(contentlets[i].getIdentifier());
            mt.setRelationType("child");
            mt.setTreeOrder(i);
            MultiTreeFactory.saveMultiTree(mt);
            childs.add(contentlets[i].getIdentifier());
        }
        
        PushPublisherConfig pc=new PushPublisherConfig();
        Set<String> ids=new HashSet<String>();
        ids.add(container.getIdentifier());
        pc.setContainers(ids);
        pc.setOperation(Operation.PUBLISH);
        File bundleRoot=BundlerUtil.getBundleRoot(UUIDGenerator.generateUuid());
        
        ContainerBundler bundler=new ContainerBundler();
        bundler.setConfig(pc);
        bundler.generate(bundleRoot, new BundlerStatus(ContainerBundler.class.getCanonicalName()));
        
        File wrapperFile=new File(bundleRoot,"live"+File.separator+"demo.dotcms.com"+File.separator
                +APILocator.getIdentifierAPI().find(container).getURI().replaceAll("/", File.separator));
        ContainerWrapper wrapper = (ContainerWrapper) new XStream(new DomDriver()).fromXML(wrapperFile);
        
        Assert.assertEquals(1,wrapper.getCsList().size());
        Assert.assertEquals(wpc.getInode(), wrapper.getCsList().get(0).getStructureId());
        Assert.assertEquals(container.getInode(), wrapper.getCvi().getLiveInode());
        Assert.assertEquals(container.getInode(), wrapper.getCvi().getWorkingInode());
        Assert.assertEquals(10,wrapper.getMultiTree().size());
        
        for(Map<String,Object> mt : wrapper.getMultiTree()) {
            Assert.assertTrue(childs.remove(mt.get("child"))); // make sure all childs are there
        }
        
        Assert.assertEquals(Operation.PUBLISH, wrapper.getOperation());
        Assert.assertEquals(title, wrapper.getContainer().getTitle());
        Assert.assertEquals("this is the code", wrapper.getContainer().getCode());
        Assert.assertEquals("preloop", wrapper.getContainer().getPreLoop());
        Assert.assertEquals("postloop", wrapper.getContainer().getPostLoop());
    }

}
