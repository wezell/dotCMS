package com.dotcms.enterprise.publishing.remote.handler;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotcms.publisher.pusher.wrapper.ContainerWrapper;
import com.dotcms.publishing.BundlerUtil;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.ContainerVersionInfo;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ContainerHandlerTest {
    
    @Test
    public void simpleContainerPush() throws Exception {
        String salt=UUIDGenerator.generateUuid();
        String containerName="ContainerTest"+salt;
        
        User user = APILocator.getUserAPI().getSystemUser();
        Host host=APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        
        String identId = UUIDGenerator.generateUuid();
        String inode = UUIDGenerator.generateUuid();
        
        Identifier ident=new Identifier();
        ident.setAssetName(containerName);
        ident.setAssetType("containers");
        ident.setHostId(host.getIdentifier());
        ident.setId(identId);
        ident.setParentPath("/");
        
        Container container=new Container();
        container.setInode(inode);
        container.setIdentifier(identId);
        container.setCode("this is the code for container "+identId);
        container.setMaxContentlets(10);
        container.setModDate(Calendar.getInstance().getTime());
        container.setModUser(user.getUserId());
        container.setPreLoop("preloop111");
        container.setPostLoop("postloop111");
        container.setTitle(containerName);
        container.setFriendlyName(containerName);
        
        ContainerVersionInfo cvi=new ContainerVersionInfo();
        cvi.setIdentifier(identId);
        cvi.setDeleted(false);
        cvi.setVersionTs(Calendar.getInstance().getTime());
        cvi.setWorkingInode(inode);
        cvi.setLiveInode(null);
        
        Structure st=StructureCache.getStructureByVelocityVarName("webPageContent");
        
        List<ContainerStructure> csList=new ArrayList<ContainerStructure>();
        ContainerStructure cs = new ContainerStructure();
        cs.setId(UUIDGenerator.generateUuid());
        cs.setStructureId(st.getInode());
        cs.setCode("$body");
        csList.add(cs);
        
        ContainerWrapper cw=new ContainerWrapper(ident,container);
        cw.setCvi(cvi);
        cw.setOperation(Operation.PUBLISH);
        cw.setCsList(csList);
        
        File bundle=BundlerUtil.getBundleRoot("test"+salt);
        File containerFile=new File(bundle,identId+".container.xml");
        XStream xstream=new XStream(new DomDriver());
        FileWriter fw=new FileWriter(containerFile);
        xstream.toXML(cw, fw);
        fw.close();
    }
}
